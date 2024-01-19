package jsenv.playwright

import org.scalajs.jsenv._
import scribe.format.{FormatterInterpolator, date, level, mdc, messages, methodName, threadName}

import scala.util.control.NonFatal

class CEEnv(
    browserName: String,
    headless: Boolean,
    showLogs: Boolean = false,
    pwConfig: PWEnv.Config
) extends JSEnv {
  private lazy val validator = {
    RunConfig
      .Validator()
      .supportsInheritIO()
      .supportsOnOutputStream()
  }
  override val name: String = s"CEEnv with $browserName"
  setupLogger(showLogs)

  override def start(input: Seq[Input], runConfig: RunConfig): JSRun =
    try {
      validator.validate(runConfig)
      new CERun(pwConfig, runConfig, input)
    } catch {
      case NonFatal(t) =>
        JSRun.failed(t)
    }

  override def startWithCom(
      input: Seq[Input],
      runConfig: RunConfig,
      onMessage: String => Unit
  ): JSComRun = new CEComRun(pwConfig, runConfig, input, onMessage)

  private def setupLogger(showLogs: Boolean): Unit = {
    val formatter =
      formatter"$date [$threadName] $level $methodName - $messages$mdc"
    scribe.Logger.root
      .clearHandlers()
      .withHandler(
        formatter = formatter,
      )
      .replace()
    if (showLogs) {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Trace).replace()
    } else {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()
    }
  }
}
