package jsenv.playwright

import org.scalajs.jsenv.{Input, UnsupportedInputException}
import scribe.format.{FormatterInterpolator, dateFull, level, mdc, messages, methodName, threadName}

import java.nio.file.Path

object CEUtils {
  def htmlPage(
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

  def setupLogger(showLogs: Boolean, debug: Boolean): Unit = {
    val formatter =
      formatter"$dateFull [$threadName] $level $methodName - $messages$mdc"
    scribe.Logger.root
      .clearHandlers()
      .withHandler(
        formatter = formatter
      )
      .replace()
    // default log level is error
    scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()

    if (showLogs) {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Info).replace()
    }
    if (debug) {
      scribe.Logger.root.withMinimumLevel(scribe.Level.Trace).replace()
    }
  }

}
