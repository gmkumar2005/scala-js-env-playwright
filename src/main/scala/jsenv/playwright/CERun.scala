package jsenv.playwright

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}
import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv.{Input, JSComRun, JSRun, RunConfig}

import java.util
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.{asScalaBufferConverter, seqAsJavaListConverter}

class CERun(
    pwConfig: Config,
    streams: OutputStreams.Streams,
    input: Seq[Input]
) extends JSRun {

//  FileMaterializer(pwConfig.materialization)
//  implicit val ec: scala.concurrent.ExecutionContext =
//    scala.concurrent.ExecutionContext.global
  private[this] implicit val ec =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

//  @volatile
//  private[this] var wantClose = false
  val headlessConfig = true
  protected val intf = "this.scalajsSeleniumInternalInterface"
  System.setProperty("playwright.driver.impl", "jsenv.DriverJar")

  /** A [[scala.concurrent.Future Future]] that completes if the run completes.
    *
    * The future is failed if the run fails.
    *
    * Note that a [[JSRun]] is not required to ever terminate on it's own. That
    * means even if all code is executed and the event loop is empty, the run
    * may continue to run. As a consequence, it is *not* correct to rely on
    * termination of a [[JSRun]] without any external means of stopping it (i.e.
    * calling [[close]]).
    */
  @volatile
  var execCounter = new AtomicInteger(0)
  @volatile
  var endCounter = new AtomicInteger(0)


  var wantToClose = new AtomicBoolean(false)
  // List of programs
  // 1. isInterfaceUp()
  // Create PW resource if not created. Create browser,context and page
  // 2. Sleep
  // 3. wantClose
  // 4. sendAll()
  // 5. fetchAndProcess()
  // 6. Close diver
  // 7. Close streams
  // 8. Close materializer
  // Flow
  // if interface is down and dont want to close wait for 100 milliseconds
  // interface is up and dont want to close sendAll(), fetchAndProcess() Sleep for 100 milliseconds
  // If want to close then close driver, streams, materializer
  // After future is completed close driver, streams, materializer

  def evalutionResponse: Resource[IO, String] =
    Resource.make {
      IO("Hello World\n") // build
    } { outStream =>
      IO {
        scribe.info("Closing the response stream")
      }.handleErrorWith(_ => {
        scribe.info("Error in closing the stream")
        IO.unit
      }) // release
    }

  def outputStream: Resource[IO, OutputStreams.Streams] =
    Resource.make {
      IO.blocking(streams) // build
    } { outStream =>
      IO {
        scribe.debug(s"Closing the stream ${outStream.hashCode()}")
        outStream.close()
      }.handleErrorWith(_ => {
        scribe.debug(s"Error in closing the stream ${outStream.hashCode()})")
        IO.unit
      }) // release
    }

  val launchOptions: BrowserType.LaunchOptions = new BrowserType.LaunchOptions()
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
  def playwrightResource: Resource[IO, Playwright] =
    Resource.make(IO {
      scribe.info("Creating playwright")
      Playwright.create()
    })(pw =>
      IO {
        scribe.info("Closing playwright")
        pw.close()
      }
    )
  def browserResource(playwright: Playwright): Resource[IO, Browser] =
    Resource.make(IO {
      scribe.info("Creating browser")
      playwright.chromium().launch(launchOptions.setHeadless(headlessConfig))
    })(browser =>
      IO {
        scribe.info("Closing browser")
        browser.close()
      }
    )
  def pageResource(
      browser: Browser
  ): Resource[IO, Page] =
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

  def createPage(wantToClose: Boolean): Resource[IO, Option[Page]] = {
    if (wantToClose) {
      Resource.pure[IO, Option[Page]](None)
    } else {
      for {
        playwright <- playwrightResource
        browser <- browserResource(playwright)
        page <- pageResource(browser)
      } yield Some(page)
    }
  }

//  def createPage: Resource[IO, Option[Page]] = {
//
//  }

  def isConnectionUp(
      pageInstanceOption: Option[Page]
  ): Resource[IO, Boolean] = {
    pageInstanceOption match {
      case Some(pageInstance) =>
        isConnectionUp(pageInstance)
      case None => Resource.pure[IO, Boolean](false)
    }
  }
  def isConnectionUp(pageInstance: Page): Resource[IO, Boolean] = {

    Resource.make {
      IO {
        scribe.info(s"Page instance is ${pageInstance.hashCode()}")
//        pageInstance.evaluate(s"true;").asInstanceOf[Boolean]
        pageInstance.evaluate(s"!!$intf;").asInstanceOf[Boolean]
      }
    }(_ => IO.unit)
  }

  def streamWriter(data: String) = for {
    out <- outputStream
    _ <- Resource.pure(out.out.write(data.getBytes()))
  } yield ()

  def streamOutWriter(data: util.List[String]) = for {
    out <- outputStream
    _ <- Resource.pure(data.forEach(out.out.println _))
  } yield ()
  def streamErrWriter(data: util.List[String]) = for {
    out <- outputStream
    _ <- Resource.pure(data.forEach(out.err.println _))
  } yield ()

  def materializerResource: Resource[IO, FileMaterializer] =
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

  def preparePageForJsRun(pageInstance: Option[Page]): Resource[IO, Unit] = pageInstance match {
    case Some(pageInstance) =>
      for {
        m <- materializerResource
        _ <- Resource.pure(
          scribe.info(s"Page instance is ${pageInstance.hashCode()}")
        )
        _ <- Resource.pure {
          val setupJsScript = Input.Script(JSSetup.setupFile(false))
          val fullInput = setupJsScript +: input
          val materialPage =
            m.materialize("scalajsRun.html", CEPageUtils.htmlPage(fullInput, m))
          pageInstance.navigate(materialPage.toString)
        }
      } yield ()
    case None =>
      Resource.pure[IO, Unit](None)
//      Resource.pure(())
  }

  // fetch and process

  def fetchMessages(
      pageInstance: Page
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

  def fetchMessages(
      pageInstanceOption: Option[Page]
  ): Resource[IO, util.Map[String, util.List[String]]] =
    pageInstanceOption match {
      case Some(pageInstance) => fetchMessages(pageInstance)
      case None =>
        Resource.pure[IO, util.Map[String, util.List[String]]](
          new util.HashMap[String, util.List[String]]()
        )
    }

  val atomicMainCounter = new java.util.concurrent.atomic.AtomicInteger(0)
  def mainPrg = for {
    _ <- Resource.pure(scribe.info(s"Begin Main! ${atomicMainCounter.incrementAndGet()}"))
    _ <- Resource.pure(scribe.info(s"wantToClose1 is ${wantToClose.get()}"))
    pageInstanceOption  <- createPage(wantToClose.get())
    _ <- Resource.pure(scribe.info(s"wantToClose2 is ${wantToClose.get()}"))
    // Prepare page for scriptJSSetup
    _ <- preparePageForJsRun(pageInstanceOption)
    initialStatus <- isConnectionUp(pageInstanceOption)
//    _ <- if(initialStatus) Resource.pure[IO, Unit](IO.sleep(1.second)) else Resource.pure[IO, Unit](IO.unit)
    jsResponse <- fetchMessages(pageInstanceOption)
    _ <- Resource.pure(scribe.info(s"jsResponse is $jsResponse"))
    _ <- streamOutWriter(jsResponse.get("consoleLog"))
//    proceeErrors <- Resource.pure(jsResponse.get("consoleError").asInstanceOf[util.List[String]])
    // sendAll()
    // fetchAndProcess()
//    data <- evalutionResponse
//    _ <- streamWriter(data)
  } yield {
    val processErrors = jsResponse.get("errors").asInstanceOf[util.List[String]]
    scribe.info(s"Closing the main program inside yield ${processErrors}")

    if (!processErrors.isEmpty) {
      scribe.info("Closing the main program inside yield with errors")
      JSRun.failed(new WindowOnErrorException(processErrors.asScala.toList))
      throw new WindowOnErrorException(
        processErrors.toArray(Array[String]()).toList
      )
    }
  }


  val atomicFutureCounter = new java.util.concurrent.atomic.AtomicInteger(0)
  val future: Future[Unit] =
//  {
//    val io: IO[Unit] = IO(println(s"Doing something asynchronously ${atomicFutureCounter.incrementAndGet()}"))
//    io.unsafeToFuture()
//  }
  {
    scribe.info(s"Begin Future program ${atomicFutureCounter.incrementAndGet()}")
    scribe.info(s"Simple log")
    mainPrg.use(_ => IO.unit).unsafeToFuture().andThen { case _ =>
      // After future is completed
//      PwRun.pageCleanup(driver, config)
      streams.close()
//      materializer.close()
    }
  }

  /** Stops the run and releases all the resources.
    *
    * This <strong>must</strong> be called to ensure the run's resources are
    * released.
    *
    * Whether or not this makes the run fail or not is up to the implementation.
    * However, in the following cases, calling [[close]] may not fail the run:
    * <ul> <li>[[future]] is already completed when [[close]] is called.
    * <li>This is a [[JSComRun]] and the event loop inside the VM is empty.
    * </ul>
    *
    * Idempotent, async, nothrow.
    */
  @volatile
  var receiveCounter = new AtomicInteger(0)

  override def close(): Unit = {
    streams.close()
    wantToClose.set(true)
    scribe.info(
      s"Received Close signal count is ${receiveCounter.getAndIncrement()}"
    )
    scribe.info(s"wantToClose in close  ${wantToClose.get()}")

  }
}

class CEComRun(
    config: Config,
    streams: OutputStreams.Streams,
    input: Seq[Input]
) extends CERun(config, streams, input)
    with JSComRun {
  override def send(msg: String): Unit = scribe.info("Send a message")
}

private class WindowOnErrorException(errs: List[String])
    extends Exception(s"JS error: $errs")
