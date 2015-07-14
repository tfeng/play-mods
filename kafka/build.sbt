import me.tfeng.playmods._

name := "kafka"

Settings.common

libraryDependencies += "org.apache.kafka" % "kafka_2.10" % Versions.kafka exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri")
