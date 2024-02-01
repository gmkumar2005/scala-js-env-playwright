package jsenv.playwright

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv.{Input, JSRun, RunConfig}

import scala.concurrent._

class CERun(
    override val browserName: String,
    override val headless: Boolean,
    override val pwConfig: Config,
    override val runConfig: RunConfig,
    override val input: Seq[Input]
) extends JSRun
    with Runner {
  scribe.debug(s"Creating CERun for $browserName")
  lazy val future: Future[Unit] =
    jsRunPrg(browserName, headless, isComEnabled = false, None)
      .use(_ => IO.unit)
      .unsafeToFuture()
}
