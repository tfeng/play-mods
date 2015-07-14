import me.tfeng.playmods._

name := "security"

Settings.common

libraryDependencies ++= Seq(
  cache,
  "org.springframework.security" % "spring-security-core" % Versions.springSecurity
)
