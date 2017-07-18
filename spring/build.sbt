import me.tfeng.playmods._

name := "spring"

Settings.common

libraryDependencies ++= Seq(
  guice,
  "me.tfeng.toolbox" % "spring" % Versions.toolbox
)
