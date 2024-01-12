package jsenv.playwright

import com.microsoft.playwright.{Browser, BrowserContext, Page, Playwright}

class PageFactory(browserName: String,headless:Boolean) extends AutoCloseable {
  val playwright: Playwright = Playwright.create()
//  scribe.debug(s"Created new playwright $playwright")
  val browser: Browser = browserName.toLowerCase match {
    case "chromium" | "chrome" =>
      playwright
        .chromium()
        .launch()
    case "firefox" =>
      playwright
        .firefox()
        .launch()
    case "webkit" =>
      playwright
        .webkit()
        .launch()
    case _ => throw new IllegalArgumentException("Invalid browser type")
  }
  val context: BrowserContext = browser.newContext()

  def getPage: Page = {

    val page = context.newPage()
//    scribe.debug(s"Created new page $page")
    page
  }

  override def close(): Unit = {
    context.close()
    browser.close()
    playwright.close()
  }
}

object PageFactory {
  def apply(browserName: String): PageFactory = {
    System.setProperty("playwright.driver.impl", "jsenv.DriverJar")
    new PageFactory(browserName, headless = true)
  }

}
