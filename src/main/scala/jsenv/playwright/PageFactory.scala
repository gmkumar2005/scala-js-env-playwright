package jsenv.playwright

import cats.effect.IO
import cats.effect.Resource
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import jsenv.playwright.BrowserName._

object PageFactory {
  def pageBuilder(browser: Browser): Resource[IO, Page] = {
    Resource.make(IO {
      val pg = browser.newContext().newPage()
      scribe.debug(s"Creating page ${pg.hashCode()} ")
      pg
    })(page => IO { page.close() })
  }

  private def browserBuilder(
      playwright: Playwright,
      browserName: BrowserName,
      headless: Boolean,
      launchOptions: LaunchOptions
  ): Resource[IO, Browser] =
    Resource.make(IO {
      val browserType: BrowserType = browserName match {
        case Chrome | Chromium | Edge =>
          playwright.chromium()
        case Firefox =>
          playwright.firefox()
        case Webkit =>
          playwright.webkit()
      }

      val options = launchOptions.setHeadless(headless)
      // set channel for chromium browsers
      // which aren't installed by default
      // by playwright:
      // https://playwright.dev/java/docs/browsers#google-chrome--microsoft-edge
      if(browserName == Edge) options.setChannel("msedge")
      if(browserName == Chrome) options.setChannel("chrome")

      val browser = browserType.launch(options)

      scribe.info(
        s"Creating browser ${browser.browserType().name()} version ${browser.version()} with ${browser.hashCode()}"
      )
      browser
    })(browser =>
      IO {
        scribe.debug(s"Closing browser with ${browser.hashCode()}")
        browser.close()
      })

  private def playWrightBuilder: Resource[IO, Playwright] =
    Resource.make(IO {
      scribe.debug(s"Creating playwright")
      Playwright.create()
    })(pw =>
      IO {
        scribe.debug("Closing playwright")
        pw.close()
      })

  def createPage(
      browserName: BrowserName,
      headless: Boolean,
      launchOptions: LaunchOptions
  ): Resource[IO, Page] =
    for {
      playwright <- playWrightBuilder
      browser <- browserBuilder(
        playwright,
        browserName,
        headless,
        launchOptions
      )
      page <- pageBuilder(browser)
    } yield page

}
