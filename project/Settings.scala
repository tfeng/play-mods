package me.tfeng.playmods

import sbt._
import sbt.Keys._

object Settings {
  val common: Seq[Setting[_]] = Seq(
    organization := "me.tfeng.play-mods",
    version := Versions.project,
    scalaVersion := Versions.scala,
    crossPaths := false,
    pomExtra :=
      <developers>
        <developer>
          <email>tfeng@berkeley.edu</email>
          <name>Thomas Feng</name>
          <url>https://github.com/tfeng</url>
          <id>tfeng</id>
        </developer>
      </developers>
      <url>https://github.com/tfeng/play-mods</url>
      <scm>
        <url>https://github.com/tfeng/play-mods</url>
        <connection>scm:git:https://github.com/tfeng/play-mods.git</connection>
        <developerConnection>scm:git:git@github.com:tfeng/play-mods.git</developerConnection>
      </scm>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
  )

  val disablePublishing: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := (),
    publishLocal := (),
    publishM2 := ()
  )
}
