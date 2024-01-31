package jsenv.playwright

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv.{Input, JSComRun, RunConfig}

import scala.concurrent._

// browserName, headless, pwConfig, runConfig, input, onMessage
class CEComRun(
    override val browserName: String,
    override val headless: Boolean,
    override val pwConfig: Config,
    override val runConfig: RunConfig,
    override val input: Seq[Input],
    onMessage: String => Unit
) extends JSComRun
    with Runner {
  scribe.debug(s"Creating CEComRun")
  logStackTrace()
  scribe.debug(s"runConfig is ${runConfig} and input is ${input} and stream is ${runConfig.onOutputStream.hashCode()}")
  // enableCom is false for CERun and true for CEComRun
  // send is called only from JSComRun
  override def send(msg: String): Unit = sendQueue.offer(msg)
  // receivedMessage is called only from JSComRun. Hence its implementation is empty in CERun
  override protected def receivedMessage(msg: String): Unit = onMessage(msg)

  lazy val future: Future[Unit] =
    jsRunPrg(browserName, headless, isComEnabled = true, None)
      .use(_ => IO.unit)
      .unsafeToFuture()


}

private class WindowOnErrorException(errs: List[String])
    extends Exception(s"JS error: $errs")
