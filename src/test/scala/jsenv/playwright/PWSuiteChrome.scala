package jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PWSuiteChrome
    extends JSEnvSuite(
      JSEnvSuiteConfig(new PWEnv("chrome", debug = false, headless = true))
    )
