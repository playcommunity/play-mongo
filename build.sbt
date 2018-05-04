
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
val scalaMeta = "org.scalameta" %% "scalameta" % "3.7.3"
val scalaMetaContrib = "org.scalameta" %% "contrib" % "3.7.3"
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

lazy val codecs = Project(
  id = "codecs",
  base = file("codecs")
).settings(buildSettings)
  .settings(
    version := "0.1.0",
    libraryDependencies ++= Seq("com.typesafe.play" %% "play" % PlayVersion, mongodbDriver, reactiveStream, scalaReflect, scalaMeta, scalaMetaContrib, playJson, scalaTest)
  )

lazy val play = Project(
  id = "play",
  base = file("play")
).enablePlugins(PlayLibrary)
  .settings(buildSettings)
  .settings(
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % PlayVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
    )
  )
  .aggregate(codecs)
  .dependsOn(codecs)

lazy val root = Project(
  id = "play-mongo",
  base = file(".")
).settings(buildSettings)
  .aggregate(play)
  .dependsOn(play)

playBuildRepoName in ThisBuild := "play-mongo"

homepage := Some(url("https://github.com/joymufeng/play-mongo"))
scmInfo := Some(ScmInfo(url("https://github.com/joymufeng/play-mongo.git"), "git@github.com:joymufeng/play-mongo.git"))
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

