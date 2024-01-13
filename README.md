# scala-js-env-playwright
A JavaScript environment for Scala.js (a JSEnv) running playwright
## Usage
Add the following line to your `project/plugins.sbt` 
```scala
// For Scala.js 1.x
libraryDependencies += "github.gmkumar2005" %% "scala-js-env-playwright" % "0.1.0-SNAPSHOT"
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
* This is a very early version. It is not yet published to maven central. You need to clone this repo and do a `sbt publishLocal` to use it.
* It works only with Scala.js 1.x
* Make sure the project is set up to use ModuleKind.ESModule in the Scala.js project.
  ```scala
    // For Scala.js 1.x
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
    ```
* Some projects which may need to use both Selenium and Playwright may run into google execption. To resolve this, add the following line to your `plugins.sbt` 
```scala
libraryDependencies += "com.google.guava" % "guava" % "33.0.0-jre"
```

## KeepAlive configuration 
```scala
lazy val pwenvConfig = Def.setting {
  jsenv.playwright.PWEnv
    .Config()
    .withKeepAlive(false)
}

jsEnv := new jsenv.playwright.PWEnv(
  browserName = "chrome",
  headless = true,
  showLogs = true,
  pwenvConfig.value,
)

```
## References
* Sample project using this JSEnv: https://github.com/gmkumar2005/scalajs-sbt-vite-laminar-chartjs-example

## Todo 
* Add examples to demonstrate how to use LaunchOptions
* ~~Add feature to keepAlive the browser~~
* Optimize to use a single browser instance for all tests by creating multiple tabs
* Configure github actions to test this project
* Configure github actions to publish to maven central
* Verify debug mode works
* Verify other test frameworks work
* Verify it works on windows and Linux
