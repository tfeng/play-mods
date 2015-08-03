import me.tfeng.playmods._

name := "titan"

Settings.common

libraryDependencies ++= Seq(
  "com.thinkaurelius.titan" % "titan-core" % Versions.titan,
  "com.thinkaurelius.titan" % "titan-berkeleyje" % Versions.titan,
  "com.thinkaurelius.titan" % "titan-lucene" % Versions.titan,
  "org.apache.tinkerpop" % "gremlin-core" % Versions.gremlin,
  "org.hamcrest" % "hamcrest-all" % Versions.hamcrest % "test"
)
