
name := "play-mongo"

version := "0.1.0"

scalaVersion := "2.12.4"

organization := "cn.playscala"

organizationName := "cn.playscala"

organizationHomepage := Some(url("https://github.com/playcommunity"))

homepage := Some(url("https://github.com/playcommunity/play-mongo"))

playBuildRepoName in ThisBuild := "play-mongo"

version in ThisBuild := "0.1.0"

val PlayVersion = playVersion(sys.env.getOrElse("PLAY_VERSION", "2.6.12"))

// Dependencies
val mongodbDriver  = "org.mongodb" % "mongodb-driver-async" % "3.7.0"
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
  organizationHomepage := Some(url("https://github.com/playcommunity")),
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.6"),
  scalacOptions in Compile := scalacOptionsVersion(scalaVersion.value),
  scalacOptions in Test := scalacOptionsTest,
  scalacOptions in IntegrationTest := scalacOptionsTest
)

lazy val codecs = Project(
  id = "codecs",
  base = file("codecs")
).settings(buildSettings)
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq("com.typesafe.play" %% "play" % PlayVersion, mongodbDriver, reactiveStream, scalaReflect, scalaMeta, scalaMetaContrib, playJson, scalaTest)
  )

lazy val play = Project(
  id = "play",
  base = file("play")
).enablePlugins(PlayLibrary)
  .settings(buildSettings)
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      //"com.typesafe.play" %% "play" % PlayVersion,
      //"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
    )
  )
  .dependsOn(codecs)

lazy val root = Project(
    id = "play-mongo-root",
    base = file(".")
  )
  .aggregate(play)
  .dependsOn(play)
  .aggregate(codecs)
  .dependsOn(codecs)
  .settings(buildSettings)
  .settings(publishSettings)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
      <scm>
        <url>git@github.com:playcommunity/play-mongo.git</url>
        <connection>scm:git:git@github.com:playcommunity/play-mongo.git</connection>
      </scm>
      <developers>
        <developer>
          <name>joymufeng</name>
          <organization>Play Community</organization>
        </developer>
      </developers>
    )
)

lazy val noPublishing = Seq(
  publishTo := None
)

