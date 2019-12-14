import sbt.addCompilerPlugin

name := "play-mongo"

version := "0.3.1"

scalaVersion := "2.12.4"

organization := "cn.playscala"

organizationName := "cn.playscala"

organizationHomepage := Some(url("https://github.com/playcommunity"))

homepage := Some(url("https://github.com/playcommunity/play-mongo"))

updateOptions := updateOptions.value.withGigahorse(false)

playBuildRepoName in ThisBuild := "play-mongo"

version in ThisBuild := "0.3.1"

val PlayVersion = playVersion(sys.env.getOrElse("PLAY_VERSION", "2.6.12"))

// Dependencies
val scalaTest      = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val mongodbDriver  = "org.mongodb" % "mongodb-driver-async" % "3.7.0"
val reactiveStream = "org.reactivestreams" % "reactive-streams" % "1.0.2"
val scalaReflect   = "org.scala-lang" % "scala-reflect" % "2.12.4"
val scalaMeta = "org.scalameta" %% "scalameta" % "3.7.3"
val scalaMetaContrib = "org.scalameta" %% "contrib" % "3.7.3"
val playJson       = "com.typesafe.play" %% "play-json" % "2.6.9"
val scalaGraph       = "org.scala-graph" %% "graph-core" % "1.12.5"

val buildSettings = Seq(
  organization := "cn.playscala",
  organizationName := "cn.playscala",
  organizationHomepage := Some(url("https://github.com/playcommunity")),
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.6"),
  //scalacOptions in Compile := scalacOptionsVersion(scalaVersion.value),
  //scalacOptions in Test := scalacOptionsTest,
  //scalacOptions in IntegrationTest := scalacOptionsTest,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

lazy val codecs = Project(
  id = "codecs",
  base = file("codecs")
).settings(buildSettings)
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq("com.typesafe.play" %% "play" % PlayVersion, scalaTest, mongodbDriver, reactiveStream, scalaReflect, scalaMeta, scalaMetaContrib, playJson, scalaGraph)
  )

lazy val play = Project(
  id = "play",
  base = file("play")
).enablePlugins(PlayLibrary)
  .settings(buildSettings)
  .settings(publishSettings)
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
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
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

