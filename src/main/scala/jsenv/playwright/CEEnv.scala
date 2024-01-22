package jsenv.playwright

import org.scalajs.jsenv._
import scribe.format.{FormatterInterpolator, dateFull, level, mdc, messages, methodName, threadName}

import scala.util.control.NonFatal

class CEEnv(
    browserName: String,
    headless: Boolean,
    showLogs: Boolean = false,
    pwConfig: PWEnv.Config
) extends JSEnv {
//  private lazy val validator = ExternalJSRun.supports(RunConfig.Validator())

  private lazy val validator = {
    RunConfig
      .Validator()
      .supportsInheritIO()
      .supportsOnOutputStream()
  }
  override val name: String = s"CEEnv with $browserName"
  setupLogger(showLogs)

  override def start(input: Seq[Input], runConfig: RunConfig): JSRun = {
    try {
      scribe.info(
        s"Calling validator CEEnv.start with input $input and runConfig $runConfig"
      )
      validator.validate(runConfig)
      new CERun(pwConfig, runConfig, input)
    } catch {
      case ve: java.lang.IllegalArgumentException =>
        scribe.error(s"CEEnv.startWithCom failed with throw ve $ve")
        throw ve
      case NonFatal(t) =>
        scribe.error(s"CEEnv.start failed with $t")
        JSRun.failed(t)
    }
  }

  override def startWithCom(
      input: Seq[Input],
      runConfig: RunConfig,
      onMessage: String => Unit
  ): JSComRun = {
    try {
      validator.validate(runConfig)
      new CEComRun(pwConfig, runConfig, input, onMessage)
    } catch {
      case ve: java.lang.IllegalArgumentException =>
        scribe.error(s"CEEnv.startWithCom failed with throw ve $ve")
        throw ve
      case NonFatal(t) =>
        scribe.error(s"CEEnv.startWithCom failed with $t")
        JSComRun.failed(t)
    }
  }
  private def setupLogger(showLogs: Boolean): Unit = {
    val formatter =
      formatter"$dateFull [$threadName] $level $methodName - $messages$mdc"
//    val formatter"${date("yyyy-MM-dd HH:mm:ss.SSS")} [$threadName] $level $methodName - $messages$mdc"
    scribe.Logger.root
      .clearHandlers()
      .withHandler(
        formatter = formatter
      )
      .replace()
    if (showLogs) {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Trace).replace()
    } else {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()
    }
  }
}
