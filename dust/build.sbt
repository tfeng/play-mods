import me.tfeng.playmods._

name := "dust"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "me.tfeng.toolbox" % "dust" % Versions.toolbox
)

addSbtPlugin("me.tfeng.sbt-plugins" % "dust-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
