package jsenv.playwright

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import jsenv.playwright.PWEnv.Config
import jsenv.playwright.ResourcesFactory._
import org.scalajs.jsenv.{Input, JSComRun, JSRun, RunConfig}

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent._
import scala.concurrent.duration.DurationInt

class CERun(
    pwConfig: Config,
    runConfig: RunConfig,
    input: Seq[Input]
) extends JSRun {

  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

//  private[this] implicit val ec =
//    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

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

  private def jsRunPrg: Resource[IO, Unit] = for {
    _ <- Resource.pure(scribe.info(s"Begin Main JSRun!"))
    pageInstance <- createPage(headlessConfig)
    _ <- preparePageForJsRun(
      pageInstance,
      materializer(pwConfig),
      input,
      enableCom = false
    )
    initialStatus <- isConnectionUp(pageInstance, intf)
    _ <-
      if (!initialStatus) Resource.pure[IO, Unit](IO.sleep(100.milliseconds))
      else Resource.pure[IO, Unit](IO.unit)
    _ <- isConnectionUp(pageInstance, intf)
    jsResponse <- fetchMessages(pageInstance, intf)
    _ <- Resource.pure(scribe.info(s"jsResponse is $jsResponse"))
    _ <- streamWriter(jsResponse, runConfig)
  } yield {
    handleErrors(jsResponse)
  }

  def handleErrors(
      jsResponse: util.Map[String, util.List[String]]
  ): Unit = {
    val processErrors = jsResponse.get("errors")
    val processConsoleErrors = jsResponse.get("consoleError")
    if (!processErrors.isEmpty || !processConsoleErrors.isEmpty) {
      scribe.info("Closing the main program inside yield with errors")
      throw new WindowOnErrorException(
        processErrors.toArray(Array[String]()).toList ++
          processConsoleErrors.toArray(Array[String]()).toList
      )
    }
  }

  val future: Future[Unit] = jsRunPrg.use(_ => IO.unit).unsafeToFuture()

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
    scribe.info(s"WantToClose in close is ${wantToClose.get()}")
  }

}

class CEComRun(
    pwConfig: Config,
    runConfig: RunConfig,
    input: Seq[Input],
    onMessage: String => Unit
) extends CERun(pwConfig, runConfig, input)
    with JSComRun {
  private[this] val sendQueue = new ConcurrentLinkedQueue[String]
  override def send(msg: String): Unit = {
    scribe.info("Send a message")
    sendQueue.offer(msg)
  }
  private def jsRunPrg: Resource[IO, Unit] = for {
    _ <- Resource.pure(scribe.info(s"Begin Main JSComRun!"))
    pageInstance <- createPage(headlessConfig)
    _ <- preparePageForJsRun(
      pageInstance,
      materializer(pwConfig),
      input,
      enableCom = true
    )
    initialStatus <- isConnectionUp(pageInstance, intf)
    _ <-
      if (!initialStatus) Resource.pure[IO, Unit](IO.sleep(100.milliseconds))
      else Resource.pure[IO, Unit](IO.unit)
    _ <- isConnectionUp(pageInstance, intf)
    jsResponse <- fetchMessages(pageInstance, intf)
    _ <- Resource.pure(scribe.info(s"jsResponse is $jsResponse"))
    _ <- streamWriter(jsResponse, runConfig)
  } yield {
    handleErrors(jsResponse)
  }

}

private class WindowOnErrorException(errs: List[String])
    extends Exception(s"JS error: $errs")
