[![Scala CI](https://github.com/gmkumar2005/scala-js-env-playwright/actions/workflows/scala.yml/badge.svg)](https://github.com/gmkumar2005/scala-js-env-playwright/actions/workflows/scala.yml)
# scala-js-env-playwright
A JavaScript environment for Scala.js (a JSEnv) running playwright
## Usage
Add the following line to your `project/plugins.sbt` 
```scala
// For Scala.js 1.x
libraryDependencies += "io.github.gmkumar2005" %% "scala-js-env-playwright" % "0.1.11"
```
Add the following line to your `build.sbt` 
```scala
Test / jsEnv := new PWEnv(
      browserName = "chrome",
      headless = true,
      showLogs = true
    )
```
## Avoid trouble
* This is a very early version. It may not work for all projects. It is tested on chrome/chromium and firefox.
* Few test cases are failing on webkit. Keep a watch on this space for updates.
* It works only with Scala.js 1.x
* Make sure the project is set up to use ModuleKind.ESModule in the Scala.js project.
  ```scala
    // For Scala.js 1.x
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
    ```
* Some projects which may need to use both Selenium and Playwright. 
If it runs into google exception, add the following line to your `plugins.sbt` 
```scala
libraryDependencies += "com.google.guava" % "guava" % "33.0.0-jre"
```

## Supported browsers
* chrome
* chromium (same as chrome)
* firefox
* webkit (experimental) - Works will on macOS. Mileage may vary on other platforms.

## Compatibility notes
### Scala versions
* This library can be used with any scala version 2.x and 3.x
* This project is compiled with scala 2.12.20
### sbt versions
* This library can be used with any sbt version 1.x 
### Playwright versions
* This library can be used with playwright version 1.49.0 `"com.microsoft.playwright" % "playwright" % "1.49.0"`
### JDK versions
* This library is tested on JDK11 and JDK21 

## Default configuration
```scala
jsEnv := new jsenv.playwright.PWEnv(
  browserName = "chrome",
  headless = true,
  showLogs = false,
)
```

## Launch options

The launch options are used to enable/disable the browser features. They can be overridden via the launchOptions parameter or extended via the additionalLaunchOptions parameter.

When not passing launchOptions, default launch options are as follows:

### Chrome/chromium
```scala
jsEnv := new jsenv.playwright.PWEnv(
  browserName = "chrome",
  launchOptions = List(
      "--disable-extensions", 
      "--disable-web-security", 
      "--allow-running-insecure-content", 
      "--disable-site-isolation-trials", 
      "--allow-file-access-from-files", 
      "--disable-gpu"
    )
)
```

### Firefox
```scala
jsEnv := new jsenv.playwright.PWEnv(
  browserName = "firefox",
  launchOptions = List(
      "--disable-web-security"
    )
)
```

### Webkit
```scala
jsEnv := new jsenv.playwright.PWEnv(
  browserName = "webkit",
  launchOptions = List(
      "--disable-extensions", 
      "--disable-web-security", 
      "--allow-running-insecure-content", 
      "--disable-site-isolation-trials", 
      "--allow-file-access-from-files"
    )
)
```

## KeepAlive configuration 
It is work in progress.
As a workaround introducing delay in the test cases may help to keep the browser alive. 

## Debugging
debug parameter can be passed to the PWEnv constructor to enable debugging. It will also display the version of the browser which is used.
```scala
Test / jsEnv := new PWEnv(
      browserName = "chrome",
      headless = true,
      showLogs = true,
      debug = true
    )
```

## Wiki
Watch this space for more details on how to use this library.

## References
* Sample project using this JSEnv: https://github.com/gmkumar2005/scalajs-sbt-vite-laminar-chartjs-example
* Fork of Laminar: https://github.com/gmkumar2005/Laminar
