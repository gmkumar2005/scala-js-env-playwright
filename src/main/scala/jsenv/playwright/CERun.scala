package jsenv.playwright

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.microsoft.playwright.BrowserType.LaunchOptions
import jsenv.playwright.PWEnv.Config
import jsenv.playwright.PageFactory._
import jsenv.playwright.ResourcesFactory._
import org.scalajs.jsenv.{Input, JSComRun, JSRun, RunConfig}

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent._
import scala.concurrent.duration.DurationInt

class CERun(
    browserName: String,
    headless: Boolean,
    pwConfig: Config,
    runConfig: RunConfig,
    input: Seq[Input]
) extends JSRun {

  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  // enableCom is false for CERun and true for CEComRun
  protected val enableCom = false
  protected val intf = "this.scalajsPlayWrightInternalInterface"
  protected val sendQueue = new ConcurrentLinkedQueue[String]
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  protected def receivedMessage(msg: String): Unit = ()

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

  def jsRunPrg(
      browserName: String,
      headless: Boolean,
      isComEnabled: Boolean,
      launchOptions: Option[LaunchOptions]
  ): Resource[IO, Unit] = for {
    _ <- Resource.pure(
      scribe.info(
        s"Begin Main with isComEnabled $isComEnabled " +
          s"and  browserName $browserName " +
          s"and headless is $headless "
      )
    )
    pageInstance <- createPage(
      browserName,
      headless,
      launchOptions
    )
    _ <- preparePageForJsRun(
      pageInstance,
      materializer(pwConfig),
      input,
      isComEnabled
    )
    connectionReady <- isConnectionUp(pageInstance, intf)
    _ <-
      if (!connectionReady) Resource.pure[IO, Unit](IO.sleep(100.milliseconds))
      else Resource.pure[IO, Unit](IO.unit)
    _ <- isConnectionUp(pageInstance, intf)
    out <- outputStream(runConfig)
    _ <- processUntilStop(
      wantToClose,
      pageInstance,
      intf,
      sendQueue,
      out,
      receivedMessage,
      isComEnabled
    )
  } yield ()

  lazy val future: Future[Unit] =
    jsRunPrg(browserName, headless, enableCom, None)
      .use(_ => IO.unit)
      .unsafeToFuture()

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

  override def close(): Unit = {
    wantToClose.set(true)
    scribe.info(s"StopSignal is ${wantToClose.get()}")
  }

}
// browserName, headless, pwConfig, runConfig, input, onMessage
class CEComRun(
    browserName: String,
    headless: Boolean,
    pwConfig: Config,
    runConfig: RunConfig,
    input: Seq[Input],
    onMessage: String => Unit
) extends CERun(
      browserName,
      headless,
      pwConfig,
      runConfig,
      input
    )
    with JSComRun {
  // enableCom is false for CERun and true for CEComRun
  override protected val enableCom = true
  // send is called only from JSComRun
  override def send(msg: String): Unit = sendQueue.offer(msg)
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  override protected def receivedMessage(msg: String): Unit = onMessage(msg)
}

private class WindowOnErrorException(errs: List[String])
    extends Exception(s"JS error: $errs")
