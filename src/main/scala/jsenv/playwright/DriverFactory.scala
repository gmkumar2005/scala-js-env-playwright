package jsenv.playwright

import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}

import scala.collection.JavaConverters._

trait DriverFactory {
  def newInstance(browserName: String): Page
  def newInstance(browserName: String, launchOptions: LaunchOptions): Page
  def newInstance(browserName: String, headless: Boolean): Page
  def newInstance(browserName: String, headless: Boolean, launchOptions: LaunchOptions): Page
}

class DefaultDriverFactory extends DriverFactory {
  def newInstance(browserName: String, headless: Boolean): Page ={
    val page: Page = browserName.toLowerCase match {
      case "chromium" | "chrome" =>
        val chromeLaunchOptions = new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-extensions",
              "--disable-web-security",
              "--allow-running-insecure-content",
              "--disable-site-isolation-trials",
              "--allow-file-access-from-files"
            ).asJava
          ).setHeadless(headless)
        newInstance(browserName, chromeLaunchOptions)
      case "firefox" =>
        val firefoxLaunchOptions = new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-web-security"
            ).asJava
          ).setHeadless(headless)
        newInstance(browserName, firefoxLaunchOptions)
      case "webkit" =>
        val webkitLaunchOptions = new BrowserType.LaunchOptions()
          .setArgs(
            List(
              "--disable-web-security"
            ).asJava
          ).setHeadless(headless)
        newInstance(browserName, webkitLaunchOptions)
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }
    page
  }
  def newInstance(browserName: String, headless: Boolean, launchOptions: LaunchOptions): Page = {
    val playwright = Playwright.create()
    val browser: Browser = browserName.toLowerCase match {
      case "chromium" | "chrome" =>
        playwright
          .chromium()
          .launch(launchOptions.setHeadless(headless))
      case "firefox" =>
        playwright
          .firefox()
          .launch(launchOptions.setHeadless(headless))
      case "webkit" =>
        playwright
          .webkit()
          .launch(launchOptions.setHeadless(headless))
      case _ => throw new IllegalArgumentException("Invalid browser type")
    }
    val context = browser.newContext()
    val page = context.newPage()
    page
  }

  def newInstance(browserName: String, launchOptions: LaunchOptions): Page = {
    newInstance(browserName, headless = true, launchOptions)
  }

  def newInstance(browserName: String): Page = {
    newInstance(browserName, headless = true)
  }
}
