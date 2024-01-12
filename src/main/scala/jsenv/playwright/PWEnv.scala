package jsenv.playwright

import com.microsoft.playwright.{Browser, BrowserContext, Page, Playwright}
import org.scalajs.jsenv._

import java.net.{URI, URL}
import java.nio.file.{Path, Paths}

final class PWEnv(browserName: String, config: PWEnv.Config, headless:Boolean) extends JSEnv {
  def this(capabilities: String) =
    this(capabilities, PWEnv.Config(),headless= true)

  def this(capabilities: String,headless:Boolean) = {
    this(capabilities, PWEnv.Config(),headless)
  }

  val name: String = s"PWEnv ($browserName)"

  def start(input: Seq[Input], runConfig: RunConfig): JSRun =
    PwRun.start(newDriver _, input, config, runConfig)

  def startWithCom(
      input: Seq[Input],
      runConfig: RunConfig,
      onMessage: String => Unit
  ): JSComRun =
    PwRun.startWithCom(newDriver _, input, config, runConfig, onMessage)

  private def newDriver(): Page = {
    // Use custom DriverJar when initializing playwright
    System.setProperty("playwright.driver.impl", "jsenv.DriverJar")
    config.driverFactory.newInstance(browserName, headless)
  }
}

object PWEnv {
  final class Config private (
      val driverFactory: DriverFactory,
      val keepAlive: Boolean,
      val materialization: Config.Materialization
  ) {
    import Config.Materialization

    private def this() = this(
      keepAlive = false,
      materialization = Config.Materialization.Temp,
      driverFactory = new DefaultDriverFactory()
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

    def withKeepAlive(keepAlive: Boolean): Config =
      copy(keepAlive = keepAlive)

    def withDriverFactory(driverFactory: DriverFactory): Config =
      copy(driverFactory = driverFactory)

    private def copy(
        keepAlive: Boolean = keepAlive,
        materialization: Config.Materialization = materialization,
        driverFactory: DriverFactory = driverFactory
    ) = {
      new Config(driverFactory, keepAlive, materialization)
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
