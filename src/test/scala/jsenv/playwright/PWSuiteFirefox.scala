package jsenv.playwright

import org.junit.runner.RunWith
import org.scalajs.jsenv.test._

@RunWith(classOf[JSEnvSuiteRunner])
class PWSuiteFirefox
    extends JSEnvSuite(
      JSEnvSuiteConfig(new PWEnv("firefox", debug = true))
    )
