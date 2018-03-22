
name := "play-mongo"

version := "0.1.0"

scalaVersion := "2.12.4"

organization := "cn.playscala"

organizationName := "cn.playscala"

val PlayVersion = playVersion(sys.env.getOrElse("PLAY_VERSION", "2.6.12"))

// Dependencies
val mongodbDriver  = "org.mongodb" % "mongodb-driver-async" % "3.6.3"
val reactiveStream = "org.reactivestreams" % "reactive-streams" % "1.0.2"
val scalaReflect   = "org.scala-lang" % "scala-reflect" % "2.12.4"
val playJson       = "com.typesafe.play" %% "play-json" % "2.6.9"
val scalaTest      = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

val scalacOptionsTest: Seq[String] = Seq( "-unchecked", "-deprecation", "-feature", "-Xlint:-missing-interpolator,_")

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq( "-unchecked", "-deprecation", "-feature", "-Ywarn-dead-code"
    /*,"-Xfatal-warnings", "-Ymacro-debug-verbose", "-Xlog-implicits", "-Yinfer-debug", "-Xprint:typer"*/) ++ (scalaVersion match {
    case "2.12.4" => Seq("-Xlint:-unused,-missing-interpolator,_" /*, "-Ywarn-unused:imports,privates,locals,-implicits,-params"*/)
    case _ => Seq("-language:existentials", "-Xlint:-missing-interpolator,_")
  })
}

val buildSettings = Seq(
  organization := "cn.playscala",
  organizationName := "cn.playscala",
  organizationHomepage := Some(url("http://www.playscala.cn")),
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.12.4"),
  scalacOptions in Compile := scalacOptionsVersion(scalaVersion.value),
  scalacOptions in Test := scalacOptionsTest,
  scalacOptions in IntegrationTest := scalacOptionsTest
)

lazy val core = Project(
  id = "core",
  base = file("core")
).configs(IntegrationTest)
  .settings(buildSettings)
  .settings(libraryDependencies ++= Seq(mongodbDriver, reactiveStream, scalaReflect, playJson, scalaTest))
  .aggregate(json)
  .dependsOn(json)

lazy val json = Project(
  id = "json",
  base = file("json")
).settings(buildSettings)
  .settings(libraryDependencies ++= Seq(mongodbDriver, reactiveStream, scalaReflect, playJson, scalaTest))

lazy val play = Project(
  id = "play",
  base = file("play")
).enablePlugins(PlayLibrary)
  .settings(buildSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % PlayVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
    )
  )
  .dependsOn(core)

lazy val root = Project(
  id = "play-mongo",
  base = file(".")
).settings(buildSettings)
  .aggregate(core)
  .aggregate(play)
  .dependsOn(core)
  .dependsOn(play)

playBuildRepoName in ThisBuild := "play-mongo"

homepage := Some(url("https://github.com/playcommunity/play-mongo"))
scmInfo := Some(ScmInfo(url("https://github.com/playcommunity/play-mongo"), "git@github.com:playcommunity/play-mongo.git"))
developers := List(
  Developer(
    "joymufeng",
    "joymufeng",
    "joymufeng@gmail.com",
    url("https://github.com/joymufeng")
  )
)
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true
publishArtifact in Test := false

// Add sonatype repository settings
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

