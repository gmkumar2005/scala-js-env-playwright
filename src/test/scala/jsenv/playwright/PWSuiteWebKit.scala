package jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PWSuiteWebKit extends JSEnvSuite(
  JSEnvSuiteConfig(new PWEnv("webkit", headless = true, showLogs = true))
)
