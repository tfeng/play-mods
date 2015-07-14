import me.tfeng.playmods._

name := "mongodb"

Settings.common

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % Versions.mongoDb,
  "org.hamcrest" % "hamcrest-all" % Versions.hamcrest % "test"
)

SbtAvro.settings
