package jsenv.playwright

import cats.effect.{IO, Resource}
import com.microsoft.playwright.BrowserType.LaunchOptions
import jsenv.playwright.PWEnv.Config
import jsenv.playwright.PageFactory._
import jsenv.playwright.ResourcesFactory._
import org.scalajs.jsenv.{Input, RunConfig}

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent._
import scala.concurrent.duration.DurationInt

trait Runner {
  val browserName: String = "" // or provide actual values
  val headless: Boolean = false // or provide actual values
  val pwConfig: Config = Config() // or provide actual values
  val runConfig: RunConfig = RunConfig() // or provide actual values
  val input: Seq[Input] = Seq.empty // or provide actual values


  // enableCom is false for CERun and true for CEComRun
  protected val enableCom = false
  protected val intf = "this.scalajsPlayWrightInternalInterface"
  protected val sendQueue = new ConcurrentLinkedQueue[String]
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  protected def receivedMessage(msg: String): Unit = ()
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

  /** Stops the run and releases all the resources.
   *
   * This <strong>must</strong> be called to ensure the run's resources are
   * released.
   *
   * Whether or not this makes the run fail or not is up to the implementation.
   * However, in the following cases, calling [[close]] may not fail the run:
   * <ul> <li>[[future]] is already completed when [[close]] is called.
   * <li>This is a [[CERun]] and the event loop inside the VM is empty. </ul>
   *
   * Idempotent, async, nothrow.
   */

  def close(): Unit = {
    wantToClose.set(true)
    scribe.info(s"StopSignal is ${wantToClose.get()}")
  }

  def getCaller: String = {
    val stackTraceElements = Thread.currentThread().getStackTrace
    if (stackTraceElements.length > 5) {
      val callerElement = stackTraceElements(5)
      s"Caller class: ${callerElement.getClassName}, method: ${callerElement.getMethodName}"
    } else {
      "Could not determine caller."
    }
  }
  def logStackTrace(): Unit = {
    try {
      throw new Exception("Logging stack trace")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

}

//private class WindowOnErrorException(errs: List[String])
//  extends Exception(s"JS error: $errs")