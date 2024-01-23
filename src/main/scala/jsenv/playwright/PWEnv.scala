package jsenv.playwright

import jsenv.playwright.PWEnv.Config
import org.scalajs.jsenv._

import java.net.{URI, URL}
import java.nio.file.{Path, Paths}
import scala.util.control.NonFatal

class PWEnv(
             browserName: String = "chromium",
             headless: Boolean = true,
             showLogs: Boolean = false,
             debug: Boolean = false,
             pwConfig: Config = Config()
) extends JSEnv {

  private lazy val validator = {
    RunConfig
      .Validator()
      .supportsInheritIO()
      .supportsOnOutputStream()
  }
  override val name: String = s"CEEnv with $browserName"
  CEUtils.setupLogger(showLogs, debug)

  override def start(input: Seq[Input], runConfig: RunConfig): JSRun = {
    try {
      validator.validate(runConfig)
      new CERun(browserName, headless, pwConfig, runConfig, input)
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
      new CEComRun(
        browserName,
        headless,
        pwConfig,
        runConfig,
        input,
        onMessage
      )
    } catch {
      case ve: java.lang.IllegalArgumentException =>
        scribe.error(s"CEEnv.startWithCom failed with throw ve $ve")
        throw ve
      case NonFatal(t) =>
        scribe.error(s"CEEnv.startWithCom failed with $t")
        JSComRun.failed(t)
    }
  }

}

object PWEnv {
  final class Config private (val materialization: Config.Materialization) {
    import Config.Materialization

    private def this() = this(
      materialization = Config.Materialization.Temp
    )

    /** Materializes purely virtual files into a temp directory.
      *
      * Materialization is necessary so that virtual files can be referred to by
      * name. If you do not know/care how your files are referred to, this is a
      * good default choice. It is also the default of [[PWEnv.Config]].
      */
    def withMaterializeInTemp: Config =
      copy(materialization = Materialization.Temp)

    /** Materializes files in a static directory of a user configured server.
      *
      * This can be used to bypass cross origin access policies.
      *
      * @param contentDir
      *   Static content directory of the server. The files will be put here.
      *   Will get created if it doesn't exist.
      * @param webRoot
      *   URL making `contentDir` accessible thorugh the server. This must have
      *   a trailing slash to be interpreted as a directory.
      *
      * @example
      *
      * The following will make the browser fetch files using the http:// schema
      * instead of the file:// schema. The example assumes a local webserver is
      * running and serving the ".tmp" directory at http://localhost:8080.
      *
      * {{{
      *  jsSettings(
      *    jsEnv := new SeleniumJSEnv(
      *        new org.openqa.selenium.firefox.FirefoxOptions(),
      *        SeleniumJSEnv.Config()
      *          .withMaterializeInServer(".tmp", "http://localhost:8080/")
      *    )
      *  )
      * }}}
      */
    def withMaterializeInServer(contentDir: String, webRoot: String): Config =
      withMaterializeInServer(Paths.get(contentDir), new URI(webRoot).toURL)

    /** Materializes files in a static directory of a user configured server.
      *
      * Version of `withMaterializeInServer` with stronger typing.
      *
      * @param contentDir
      *   Static content directory of the server. The files will be put here.
      *   Will get created if it doesn't exist.
      * @param webRoot
      *   URL making `contentDir` accessible thorugh the server. This must have
      *   a trailing slash to be interpreted as a directory.
      */
    def withMaterializeInServer(contentDir: Path, webRoot: URL): Config =
      copy(materialization = Materialization.Server(contentDir, webRoot))

    def withMaterialization(materialization: Materialization): Config =
      copy(materialization = materialization)

    private def copy(
        materialization: Config.Materialization = materialization
    ): Config = {
      new Config(materialization)
    }
  }

  object Config {
    def apply(): Config = new Config()

    abstract class Materialization private ()
    object Materialization {
      final case object Temp extends Materialization
      final case class Server(contentDir: Path, webRoot: URL)
          extends Materialization {
        require(
          webRoot.getPath.endsWith("/"),
          "webRoot must end with a slash (/)"
        )
      }
    }
  }
}
