package jsenv.playwright

import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright._

import scala.collection.JavaConverters._

case class PWDriver(
    pw: Playwright,
    browser: Browser,
    context: BrowserContext,
    page: Page
)

trait DriverFactory {
  def pageBuilder(browserName: String): PWDriver
  def pageBuilder(browserName: String, headless: Boolean): PWDriver
  def pageBuilder(
      browserName: String,
      headless: Boolean,
      launchOptions: LaunchOptions
  ): PWDriver

}

class DefaultDriverFactory extends DriverFactory {
  scribe.debug("Creating new DefaultDriverFactory and Playwright instance")

  def pageBuilder(browserName: String): PWDriver = {
    pageBuilder(browserName, headless = true)
  }
  override def pageBuilder(browserName: String, headless: Boolean): PWDriver = {
    val launchOptions = browserName.toLowerCase match {
      case "chromium" | "chrome" =>
        new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-extensions",
              "--disable-web-security",
              "--allow-running-insecure-content",
              "--disable-site-isolation-trials",
              "--allow-file-access-from-files"
            ).asJava
          )
      case "firefox" =>
        new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-web-security"
            ).asJava
          )
      case "webkit" =>
        new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-extensions",
              "--disable-web-security",
              "--allow-running-insecure-content",
              "--disable-site-isolation-trials",
              "--allow-file-access-from-files"
            ).asJava
          )
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }
    launchOptions.setHeadless(headless)
    pageBuilder(browserName, headless, launchOptions)
  }

  override def pageBuilder(
      browserName: String,
      headless: Boolean,
      launchOptions: LaunchOptions
  ): PWDriver = {
    val playwright = Playwright.create()
    val browserType: BrowserType = browserName.toLowerCase match {
      case "chromium" | "chrome" =>
        playwright
          .chromium()
      case "firefox" =>
        playwright
          .firefox()
      case "webkit" =>
        playwright
          .webkit()
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }
    val browser = browserType.launch(launchOptions.setHeadless(headless))
    val context = browser.newContext()
    val page = context.newPage()
    scribe.info(s"Created new page ${page.hashCode()}")
    PWDriver(playwright, browser, context, page)
  }
}
