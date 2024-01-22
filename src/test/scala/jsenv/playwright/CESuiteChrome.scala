package jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class CESuiteChrome
    extends JSEnvSuite(
      JSEnvSuiteConfig(
        new CEEnv("chrome", headless = true, showLogs = true, PWEnv.Config())
      )
    )
