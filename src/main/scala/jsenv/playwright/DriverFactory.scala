package jsenv.playwright

import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}

trait DriverFactory {
  def newInstance(capabilities: String): Page
}

class DefaultDriverFactory extends DriverFactory {
  val headless = true
  def newInstance(capabilities: String): Page = {
    val playwright = Playwright.create()

    val browser: Browser = capabilities.toLowerCase match {
      case "chromium" =>
        playwright
          .chromium()
          .launch(new BrowserType.LaunchOptions().setHeadless(headless))
      case "chrome" =>
        playwright
          .chromium()
          .launch(new BrowserType.LaunchOptions().setHeadless(headless))
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
