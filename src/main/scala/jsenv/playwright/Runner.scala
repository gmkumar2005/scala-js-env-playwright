package jsenv.playwright

import cats.effect.{IO, Resource}
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserType.LaunchOptions
import jsenv.playwright.BrowserName.{Chrome, Chromium, Edge, Firefox, Webkit}
import jsenv.playwright.PWEnv.Config
import jsenv.playwright.PageFactory._
import jsenv.playwright.ResourcesFactory._
import org.scalajs.jsenv.{Input, RunConfig}

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.seqAsJavaListConverter

trait Runner {
  val browserName: BrowserName// or provide actual values
  val headless: Boolean = false // or provide actual values
  val pwConfig: Config = Config() // or provide actual values
  val runConfig: RunConfig = RunConfig() // or provide actual values
  val input: Seq[Input] = Seq.empty // or provide actual values
  val launchOptions: List[String] = Nil
  val additionalLaunchOptions: List[String] = Nil

  // enableCom is false for CERun and true for CEComRun
  protected val enableCom = false
  protected val intf = "this.scalajsPlayWrightInternalInterface"
  protected val sendQueue = new ConcurrentLinkedQueue[String]
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  protected def receivedMessage(msg: String): Unit
  private val wantToClose = new AtomicBoolean(false)
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
      browserName: BrowserName,
      headless: Boolean,
      isComEnabled: Boolean,
      launchOptions: LaunchOptions
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
      if (!connectionReady) Resource.pure[IO, Unit] {
        IO.sleep(100.milliseconds)
      }
      else Resource.pure[IO, Unit](IO.unit)
    _ <-
      if (!connectionReady) isConnectionUp(pageInstance, intf)
      else Resource.pure[IO, Unit](IO.unit)
    out <- outputStream(runConfig)
    _ <- processUntilStop(
      wantToClose,
      pageInstance,
      intf,
      sendQueue,
      out,
      receivedMessage
    )
  } yield ()

  /**
   * Stops the run and releases all the resources.
   *
   * This <strong>must</strong> be called to ensure the run's resources are released.
   *
   * Whether or not this makes the run fail or not is up to the implementation. However, in the
   * following cases, calling [[close]] may not fail the run: <ul> <li>[[scala.concurrent.Future]] is already
   * completed when [[close]] is called. <li>This is a [[CERun]] and the event loop inside the
   * VM is empty. </ul>
   *
   * Idempotent, async, nothrow.
   */

  def close(): Unit = {
    wantToClose.set(true)
    scribe.debug(s"Received stopSignal ${wantToClose.get()}")
  }

  protected lazy val pwLaunchOptions: LaunchOptions =
    browserName match {
      case Chrome | Chromium | Edge =>
        new BrowserType.LaunchOptions().setArgs(
          if (launchOptions.isEmpty)
            (PWEnv.chromeLaunchOptions ++ additionalLaunchOptions).asJava
          else (launchOptions ++ additionalLaunchOptions).asJava
        )
      case Firefox =>
        new BrowserType.LaunchOptions().setArgs(
          if (launchOptions.isEmpty)
            (PWEnv.firefoxLaunchOptions ++ additionalLaunchOptions).asJava
          else (launchOptions ++ additionalLaunchOptions).asJava
        )
      case Webkit =>
        new BrowserType.LaunchOptions().setArgs(
          if (launchOptions.isEmpty)
            (PWEnv.webkitLaunchOptions ++ additionalLaunchOptions).asJava
          else (launchOptions ++ additionalLaunchOptions).asJava
        )
    }
}
