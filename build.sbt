import me.tfeng.playmods._

name := "play-mods"

Settings.common ++ Settings.disablePublishing

lazy val parent = project in file(".") aggregate(avro, avroD2, dust, http, oauth2, security, spring)

lazy val spring = project in file("spring") enablePlugins(PlayJava)

lazy val dust = project in file("dust") enablePlugins(PlayScala) dependsOn(spring)

lazy val http = project in file("http") enablePlugins(PlayJava) dependsOn(spring)

lazy val security = project in file("security") enablePlugins(PlayJava) dependsOn(spring)

lazy val avro = project in file("avro") enablePlugins(PlayScala) dependsOn(spring, http)

lazy val avroD2 = project in file("avro-d2") enablePlugins(PlayScala) dependsOn(avro)

lazy val oauth2 = project in file("oauth2") enablePlugins(PlayJava) dependsOn(security, avro)
