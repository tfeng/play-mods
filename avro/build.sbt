import me.tfeng.playmods._

name := "avro"

Settings.common

libraryDependencies ++= Seq(
  "me.tfeng.toolbox" % "avro" % Versions.toolbox,
  "org.apache.avro" % "avro-ipc" % Versions.avro,
  "org.apache.httpcomponents" % "httpcore" % Versions.httpComponents,
  "org.springframework.security.oauth" % "spring-security-oauth2" % Versions.springSecurityOauth
)
