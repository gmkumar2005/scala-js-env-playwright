package jsenv.playwright

import org.junit.Assert.assertTrue
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
    100.second
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
    if (withCom) {
      cekit.withComRun(code, config)(body)
    }
    else {
      cekit.withRun(code, config)(body)
    }
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

  @Test
  def basicTestce: Unit = {
    //    this.scalajsSeleniumInternalInterface.send(arguments[0]);
    //    0
    cekit.withComRun("""
      scalajsCom.init(function(msg) { scalajsCom.send("received: " + msg); });
      scalajsCom.send("Hello World");
    """) { run =>

      run.expectMsg("Hello World")

      for (i <- 0 to 10) {
        run
          .send(i.toString)
          .expectMsg(s"received: $i")
      }

      run.expectNoMsgs()
        .closeRun()
    }
  }



  @Test
  def basicTest: Unit = {
    //    this.scalajsSeleniumInternalInterface.send(arguments[0]);
    //    0
    kit.withComRun("""
      scalajsCom.init(function(msg) { scalajsCom.send("received: " + msg); });
      scalajsCom.send("Hello World");
    """) { run =>

      run.expectMsg("Hello World")

      for (i <- 0 to 10) {
        run
          .send(i.toString)
          .expectMsg(s"received: $i")
      }

      run.expectNoMsgs()
        .closeRun()
    }
  }
  private val slack = 10.millis

  @Test
  def basicTimeoutTestCE: Unit = {

    val deadline = (300.millis - slack).fromNow

    cewithRun("""
      setTimeout(function() { console.log("1"); }, 200);
      setTimeout(function() { console.log("2"); }, 100);
      setTimeout(function() { console.log("3"); }, 300);
      setTimeout(function() { console.log("4"); },   0);
    """) {
      _.expectOut("4\n")
        .expectOut("2\n")
        .expectOut("1\n")
        .expectOut("3\n")

        .closeRun()
    }

    assertTrue("Execution took too little time", deadline.isOverdue())
  }

  @Test
  def basicTimeoutTestPW: Unit = {

    val deadline = (300.millis - slack).fromNow

    withRun("""
      setTimeout(function() { console.log("1"); }, 200);
      setTimeout(function() { console.log("2"); }, 100);
      setTimeout(function() { console.log("3"); }, 300);
      setTimeout(function() { console.log("4"); },   0);
    """) {
      _.expectOut("4\n")
        .expectOut("2\n")
        .expectOut("1\n")
        .expectOut("3\n")

        .closeRun()
    }

    assertTrue("Execution took too little time", deadline.isOverdue())
  }

}
