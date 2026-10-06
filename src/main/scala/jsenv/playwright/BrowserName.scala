package jsenv.playwright

sealed trait BrowserName

object BrowserName {
  case object Chrome extends BrowserName

  case object Chromium extends BrowserName

  case object Edge extends BrowserName

  case object Firefox extends BrowserName

  case object Webkit extends BrowserName
}