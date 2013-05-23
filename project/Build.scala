import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "word-service"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "org.springframework" % "spring-context" % "3.2.1.RELEASE",
    "org.springframework" % "spring-orm" % "3.2.1.RELEASE",
    "org.springframework" % "spring-jdbc" % "3.2.1.RELEASE",
    "org.springframework" % "spring-tx" % "3.2.1.RELEASE",
    "org.springframework" % "spring-test" % "3.2.1.RELEASE" % "test"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
