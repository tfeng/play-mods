import me.tfeng.playmods._

name := "dust"

sbtPlugin := true

Settings.common

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator" % Versions.webjarsLocator,
  "org.webjars" % "dustjs-linkedin" % Versions.dustjs
)

addSbtPlugin("me.tfeng.sbt-plugins" % "dust-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
