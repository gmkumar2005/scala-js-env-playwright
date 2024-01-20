package jsenv.playwright

import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv._

import java.nio.file.Path
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}
import java.util.function.Consumer
import scala.annotation.tailrec
import scala.concurrent._
import scala.util.control.NonFatal

class PwRun(
    driver: PWDriver,
    config: Config,
    streams: OutputStreams.Streams,
    materializer: FileMaterializer
) extends JSRun {

  @volatile
  private[this] var wantClose = false

  protected val intf = "this.scalajsSeleniumInternalInterface"

  private[this] implicit val ec =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  private val handler = Future {
    while (!isInterfaceUp() && !wantClose) {
      Thread.sleep(100)
    }

    while (!wantClose) {
      sendAll()
      fetchAndProcess()
      Thread.sleep(100)
    }
  }

  val future: Future[Unit] = handler.andThen { case _ =>
    PwRun.pageCleanup(driver, config)
    streams.close()
    materializer.close()
  }

  def close(): Unit = wantClose = true

  private final def fetchAndProcess(): Unit = {
    import PwRun.consumer

    val data = driver.page
      .evaluate(s"$intf.fetch();")
      .asInstanceOf[java.util.Map[String, java.util.List[String]]]

    data.get("consoleLog").forEach(consumer(streams.out.println _))
    data.get("consoleError").forEach(consumer(streams.err.println _))
    data.get("msgs").forEach(consumer(receivedMessage _))

    val errs = data.get("errors")
    if (!errs.isEmpty()) {
      // Convoluted way of writing errs.toList without JavaConverters.
      val errList = errs.toArray(Array[String]()).toList
      throw new PwRun.WindowOnErrorException(errList)
    }
  }

  private final def isInterfaceUp() =
    driver.page.evaluate(s"!!$intf;").asInstanceOf[Boolean]

  // Hooks for SeleniumComRun.

  protected def sendAll(): Unit = ()

  protected def receivedMessage(msg: String): Unit =
    throw new AssertionError(s"received message in non-com run: $msg")
}

private final class PwComRun(
    driver: PWDriver,
    config: PWEnv.Config,
    streams: OutputStreams.Streams,
    materializer: FileMaterializer,
    onMessage: String => Unit
) extends PwRun(driver, config, streams, materializer)
    with JSComRun {
  private[this] val sendQueue = new ConcurrentLinkedQueue[String]

  def send(msg: String): Unit = sendQueue.offer(msg)

  override protected def receivedMessage(msg: String): Unit = onMessage(msg)

  @tailrec
  override protected def sendAll(): Unit = {
    val msg = sendQueue.poll()
    if (msg != null) {
      scribe.info(s"Sending message $msg")
      val script = s"$intf.send(arguments[0]);"
      val wrapper = s"function(arg) { $script }"
      driver.page.evaluate(s"$wrapper", msg)
      sendAll()
    }
  }
}

private object PwRun {
  import OutputStreams.Streams
  import PWEnv.Config

  private lazy val validator = {
    RunConfig
      .Validator()
      .supportsInheritIO()
      .supportsOnOutputStream()
  }

  def start(
      newDriver: () => PWDriver,
      input: Seq[Input],
      config: Config,
      runConfig: RunConfig
  ): JSRun = {
    startInternal(newDriver, input, config, runConfig, enableCom = false)(
      new PwRun(_, _, _, _),
      JSRun.failed
    )
  }

  def startWithCom(
      newDriver: () => PWDriver,
      input: Seq[Input],
      config: Config,
      runConfig: RunConfig,
      onMessage: String => Unit
  ): JSComRun = {
    startInternal(newDriver, input, config, runConfig, enableCom = true)(
      new PwComRun(_, _, _, _, onMessage),
      JSComRun.failed
    )
  }

  private type Ctor[T] = (PWDriver, Config, Streams, FileMaterializer) => T

  private def startInternal[T](
      newDriver: () => PWDriver,
      input: Seq[Input],
      config: Config,
      runConfig: RunConfig,
      enableCom: Boolean
  )(newRun: Ctor[T], failed: Throwable => T): T = {
    scribe.debug(s"Starting PWRun")
    validator.validate(runConfig)

    try {
      withCleanup(FileMaterializer(config.materialization))(_.close()) { m =>
        val setupJsScript = Input.Script(JSSetup.setupFile(enableCom))
        val fullInput = setupJsScript +: input
        val materialPage =
          m.materialize("scalajsRun.html", htmlPage(fullInput, m))
        withCleanup(newDriver())(pageCleanup(_, config)) { driver =>
          driver.page.navigate(materialPage.toString)
          withCleanup(OutputStreams.prepare(runConfig))(_.close()) { streams =>
            newRun(driver, config, streams, m)
          }
        }
      }
    } catch {
      case NonFatal(t) =>
        failed(t)
    } finally {
      scribe.debug("Finished PWRun")
    }

  }

  private def withCleanup[V, R](
      mk: => V
  )(cleanup: V => Unit)(body: V => R): R = {
    val v = mk
    try {
      body(v)
    } catch {
      case t: Throwable =>
        cleanup(v)
        throw t
    }
  }

  private def pageCleanup(d: PWDriver, config: PWEnv.Config): Unit = {

    if (!config.keepAlive) {
      scribe.info(s"Closing page ${d.page.hashCode()}")
      if (d.page.isClosed) {
        scribe.debug(s"Page is already closed")
      } else {
        d.page.close()
      }
      d.context.close()
      d.browser.close()
      d.pw.close()
    }

  }

  private def htmlPage(
      fullInput: Seq[Input],
      materializer: FileMaterializer
  ): String = {
    val tags = fullInput.map {
      case Input.Script(path) => makeTag(path, "text/javascript", materializer)
      case Input.ESModule(path) => makeTag(path, "module", materializer)
      case _ => throw new UnsupportedInputException(fullInput)
    }

    s"""<html>
       |  <meta charset="UTF-8">
       |  <body>
       |    ${tags.mkString("\n    ")}
       |  </body>
       |</html>
    """.stripMargin
  }

  private def makeTag(
      path: Path,
      tpe: String,
      materializer: FileMaterializer
  ): String = {
    val url = materializer.materialize(path)
    s"<script defer type='$tpe' src='$url'></script>"
  }

  private class WindowOnErrorException(errs: List[String])
      extends Exception(s"JS error: $errs")

  private def consumer[A](f: A => Unit): Consumer[A] = new Consumer[A] {
    override def accept(v: A): Unit = f(v)
  }
}
