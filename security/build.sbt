import me.tfeng.playmods._

name := "security"

Settings.common

libraryDependencies ++= Seq(
  cacheApi,
  "org.springframework.security" % "spring-security-core" % Versions.springSecurity
)
