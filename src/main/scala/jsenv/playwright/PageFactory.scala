package jsenv.playwright

import cats.effect.{IO, Resource}
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}

import scala.jdk.CollectionConverters.seqAsJavaListConverter
object PageFactory {
  def pageBuilder(browser: Browser): Resource[IO, Page] = {
    Resource.make(IO {
      val pg = browser.newContext().newPage()
      scribe.debug(s"Creating page ${pg.hashCode()} ")
      pg
    })(page =>
      IO {page.close()}
    )
  }

  private def browserBuilder(
                              playwright: Playwright,
                              browserName: String,
                              headless: Boolean,
                              launchOptions: Option[LaunchOptions] = None
  ): Resource[IO, Browser] =
    Resource.make(IO {

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
      launchOptions match {
        case Some(launchOptions) =>
          browserType.launch(launchOptions.setHeadless(headless))
        case None =>
          val launchOptions = browserName.toLowerCase match {
            case "chromium" | "chrome" =>
              new BrowserType.LaunchOptions()
                .setArgs(
                  List(
                    "--disable-extensions",
                    "--disable-web-security",
                    "--allow-running-insecure-content",
                    "--disable-site-isolation-trials",
                    "--allow-file-access-from-files",
                    "--disable-gpu"
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
         val browser =  browserType.launch(launchOptions.setHeadless(headless))
          scribe.debug(s"Creating browser $browserName version ${browser.version()} with ${browser.hashCode()}")
          browser
      }
    })(browser =>
      IO {
        scribe.debug(s"Closing browser with ${browser.hashCode()}")
        browser.close()
      }
    )

  private def playWrightBuilder: Resource[IO, Playwright] =
    Resource.make(IO {
      scribe.debug(s"Creating playwright")
      Playwright.create()
    })(pw =>
      IO {
        scribe.debug("Closing playwright")
        pw.close()
      }
    )

  def createPage(
                  browserName: String,
                  headless: Boolean,
                  launchOptions: Option[LaunchOptions]
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
