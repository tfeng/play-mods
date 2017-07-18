import me.tfeng.playmods._

name := "http"

Settings.common

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % Versions.playWs
)
