package jsenv.playwright

import cats.effect.{IO, Resource}
import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}
import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv.{Input, RunConfig}

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.seqAsJavaListConverter

object ResourcesFactory {
  def pageBuilder(browser: Browser): Resource[IO, Page] = {
    Resource.make(IO {
      val pg = browser.newContext().newPage()
      scribe.info(s"Creating page ${pg.hashCode()}")
      pg
    })(page =>
      IO {
        scribe.info(s"Closing page ${page.hashCode()}")
        page.close()
      }
    )
  }

  private def browserBuilder(
      playwright: Playwright,
      headlessConfig: Boolean
  ): Resource[IO, Browser] = {
    val launchOptions: BrowserType.LaunchOptions =
      new BrowserType.LaunchOptions()
        .setArgs(
          List(
            "--disable-extensions",
            "--disable-web-security",
            "--allow-running-insecure-content",
            "--disable-site-isolation-trials",
            "--allow-file-access-from-files",
            "--disable-gpu"
          ).asJava
        )
    browserBuilder(playwright, launchOptions, headlessConfig)
  }
  private def browserBuilder(
      playwright: Playwright,
      launchOptions: BrowserType.LaunchOptions,
      headlessConfig: Boolean
  ): Resource[IO, Browser] =
    Resource.make(IO {
      scribe.info("Creating browser")
      playwright.chromium().launch(launchOptions.setHeadless(headlessConfig))
    })(browser =>
      IO {
        scribe.info("Closing browser")
        browser.close()
      }
    )

  private def plawrightBuilder: Resource[IO, Playwright] =
    Resource.make(IO {
      scribe.info("Creating playwright")
      Playwright.create()
    })(pw =>
      IO {
        scribe.info("Closing playwright")
        pw.close()
      }
    )

  def createPage(headlessConfig: Boolean): Resource[IO, Page] =
    for {
      playwright <- ResourcesFactory.plawrightBuilder
      browser <- ResourcesFactory.browserBuilder(playwright, headlessConfig)
      page <- ResourcesFactory.pageBuilder(browser)
    } yield page

  def preparePageForJsRun(
      pageInstance: Page,
      materializerResource: Resource[IO, FileMaterializer],
      input: Seq[Input],
      enableCom: Boolean
  ): Resource[IO, Unit] =
    for {
      m <- materializerResource
      _ <- Resource.pure(
        scribe.info(s"Page instance is ${pageInstance.hashCode()}")
      )
      _ <- Resource.pure {
        val setupJsScript = Input.Script(JSSetup.setupFile(enableCom))
        val fullInput = setupJsScript +: input
        val materialPage =
          m.materialize(
            "scalajsRun.html",
            CEPageUtils.htmlPage(fullInput, m)
          )
        pageInstance.navigate(materialPage.toString)
      }
    } yield ()

  def fetchMessages(
      pageInstance: Page,
      intf: String
  ): Resource[IO, util.Map[String, util.List[String]]] = {
    Resource.make {
      IO {
        scribe.info(s"Page instance is ${pageInstance.hashCode()}")
        val data = pageInstance
          .evaluate(s"$intf.fetch();")
          .asInstanceOf[java.util.Map[String, java.util.List[String]]]
        data
      }
    }(_ => IO.unit)
  }

  def isConnectionUp(
      pageInstance: Page,
      intf: String
  ): Resource[IO, Boolean] = {
    Resource.make {
      IO {
        scribe.info(s"Page instance is ${pageInstance.hashCode()}")
        pageInstance.evaluate(s"!!$intf;").asInstanceOf[Boolean]
      }
    }(_ => IO.unit)
  }

  def materializer(pwConfig: Config): Resource[IO, FileMaterializer] =
    Resource.make {
      IO.blocking(FileMaterializer(pwConfig.materialization)) // build
    } { fileMaterializer =>
      IO {
        scribe.info("Closing the fileMaterializer")
        fileMaterializer.close()
      }.handleErrorWith(_ => {
        scribe.info("Error in closing the fileMaterializer")
        IO.unit
      }) // release
    }

  /*
   * Creates resource for outputStream
   */
  private def outputStream(
      runConfig: RunConfig
  ): Resource[IO, OutputStreams.Streams] =
    Resource.make {
      IO.blocking(OutputStreams.prepare(runConfig)) // build
    } { outStream =>
      IO {
        scribe.debug(s"Closing the stream ${outStream.hashCode()}")
        outStream.close()
      }.handleErrorWith(_ => {
        scribe.debug(s"Error in closing the stream ${outStream.hashCode()})")
        IO.unit
      }) // release
    }

  def streamWriter(
      jsResponse: util.Map[String, util.List[String]],
      runConfig: RunConfig,
      onMessage: Option[String => Unit] = None
  ): Resource[IO, Unit] = {
    val data = jsResponse.get("consoleLog")
    val consoleError = jsResponse.get("consoleError")
    val error = jsResponse.get("errors")
    onMessage match {
      case Some(f) =>
        val msgs = jsResponse.get("msgs")
        msgs.forEach(consumer(f))
      case None => ()
    }
    for {
      out <- ResourcesFactory.outputStream(runConfig)
      _ <- Resource.pure(data.forEach(out.out.println _))
      _ <- Resource.pure(error.forEach(out.out.println _))
      _ <- Resource.pure(consoleError.forEach(out.out.println _))
    } yield ()
  }

  @tailrec
  def sendAll(
      sendQueue: ConcurrentLinkedQueue[String],
      pageInstance: Page,
      intf: String
  ): Unit = {
    val msg = sendQueue.poll()
    if (msg != null) {
      val script = s"$intf.send(arguments[0]);"
      val wrapper = s"function(arg) { $script }"
      pageInstance.evaluate(s"$wrapper", msg)
      sendAll(sendQueue, pageInstance, intf)
    }
  }
  private def consumer[A](f: A => Unit): Consumer[A] = new Consumer[A] {
    override def accept(v: A): Unit = f(v)
  }
}
