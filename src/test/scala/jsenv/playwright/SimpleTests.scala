package jsenv.playwright

import org.junit.Test
import org.scalajs.jsenv.test.kit.{Run, TestKit}
import org.scalajs.jsenv.{Input, RunConfig}

import scala.concurrent.duration.DurationInt

class SimpleTests {
  private val kit = new TestKit(
    new PWEnv("chrome", headless = true, showLogs = false),
    10.second
  )
  private val cekit = new TestKit(
    new CEEnv("chrome", headless = false, showLogs = true, PWEnv.Config()),
    10.second
  )
  val withCom = true
  private def withRun(input: Seq[Input])(body: Run => Unit) = {
    if (withCom) kit.withComRun(input)(body)
    else kit.withRun(input)(body)
  }

  private def withRun(code: String, config: RunConfig = RunConfig())(
      body: Run => Unit
  ) = {
    if (withCom) kit.withComRun(code, config)(body)
    else kit.withRun(code, config)(body)
  }
  private def cewithRun(code: String, config: RunConfig = RunConfig())(
      body: Run => Unit
  ) = {
    if (withCom) cekit.withComRun(code, config)(body)
    else
      cekit.withRun(code, config)(body)
  }

  @Test
  def pwHello(): Unit = {
    kit.withRun("""console.log("Hello World");""") {
      _.expectOut("Hello World\n")
        .closeRun()
    }

//    withRun("console.log('Hello World')") {
//      _.expectOut("Hello World")
//        .closeRun()
//    }
  }
  @Test
  def ceHello(): Unit = {
    System.setProperty("playwright.driver.impl", "jsenv.DriverJar")
    cekit.withRun("""console.log("Hello World");""") {
      _.expectOut("Hello World\n")
        .closeRun()
    }
  }

  @Test
  def ceHello2(): Unit = {
    System.setProperty("playwright.driver.impl", "jsenv.DriverJar")
    cekit.withRun("""console.log("Hello World2");""") {
      _.expectOut("Hello World2\n")
        .closeRun()
    }
  }
}