package jsenv.playwright

import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}

import scala.collection.JavaConverters._

trait DriverFactory {
  def newInstance(capabilities: String): Page
}

class DefaultDriverFactory extends DriverFactory {
  val headless = true

  def newInstance(capabilities: String): Page = {
    val playwright = Playwright.create()

    val browser: Browser = capabilities.toLowerCase match {
      case "chromium" =>
        val chromeLaunchOptions = new BrowserType.LaunchOptions()
        //        "--disable-web-security", "--allow-running-insecure-content")
        chromeLaunchOptions.setArgs(List("--disable-extensions", "--disable-web-security",
          "--allow-running-insecure-content", "--disable-site-isolation-trials",
          "--allow-file-access-from-files").asJava)
        chromeLaunchOptions.setHeadless(headless)
        playwright
          .chromium()
          .launch(chromeLaunchOptions)
      case "chrome" =>
        val chromeLaunchOptions = new BrowserType.LaunchOptions()
//        "--disable-web-security", "--allow-running-insecure-content")
        chromeLaunchOptions.setArgs(List("--disable-extensions", "--disable-web-security",
          "--allow-running-insecure-content", "--disable-site-isolation-trials",
          "--allow-file-access-from-files").asJava)

        chromeLaunchOptions.setHeadless(headless)
        playwright
          .chromium()
          .launch(chromeLaunchOptions)
      case "firefox" =>
        playwright
          .firefox()
          .launch(new BrowserType.LaunchOptions().setHeadless(headless))
      case "webkit" =>
        playwright
          .webkit()
          .launch(new BrowserType.LaunchOptions().setHeadless(headless))
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }

    val context = browser.newContext()
    val page = context.newPage()
    page
  }
}
