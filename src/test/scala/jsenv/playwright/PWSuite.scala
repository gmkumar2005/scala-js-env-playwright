package jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PWSuite extends JSEnvSuite(
  JSEnvSuiteConfig(new PWEnv("chrome"))
)
