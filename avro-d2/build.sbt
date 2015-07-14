import me.tfeng.playmods._

name := "avro-d2"

sbtPlugin := true

Settings.common

libraryDependencies += "org.apache.zookeeper" % "zookeeper" % Versions.zookeeper

addSbtPlugin("me.tfeng.sbt-plugins" % "avro-plugin" % Versions.sbtPlugins)

unmanagedSourceDirectories in Compile += baseDirectory.value / "../project"
