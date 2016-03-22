import me.tfeng.playmods._

name := "play-mods"

Settings.common ++ Settings.disablePublishing

lazy val parent = project in file(".") aggregate(avro, `avro-d2`, dust, http, oauth2, security, spring)

lazy val spring = project enablePlugins(PlayJava)

lazy val dust = project enablePlugins(PlayJava) dependsOn(spring)

lazy val http = project enablePlugins(PlayJava) dependsOn(spring)

lazy val security = project enablePlugins(PlayJava) dependsOn(spring)

lazy val avro = project enablePlugins(PlayScala) dependsOn(spring, http)

lazy val `avro-d2` = project enablePlugins(PlayScala) dependsOn(avro)

lazy val oauth2 = project enablePlugins(PlayJava) dependsOn(security, avro)
