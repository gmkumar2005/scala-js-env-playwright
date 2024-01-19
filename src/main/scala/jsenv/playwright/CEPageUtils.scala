package jsenv.playwright

import jsenv.playwright.PwRun.makeTag
import org.scalajs.jsenv.{Input, UnsupportedInputException}

import java.nio.file.Path

object CEPageUtils {
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
}
