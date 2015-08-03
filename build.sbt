import me.tfeng.playmods._

name := "play-mods"

Settings.common ++ Settings.disablePublishing

lazy val parent =
    project in file(".") aggregate(common, spring, dust, http, security, avro, avroD2, oauth2, kafka, mongodb, titan)

lazy val common = project in file("common") enablePlugins(PlayJava)

lazy val spring = project in file("spring") enablePlugins(PlayJava) dependsOn(common)

lazy val dust = project in file("dust") enablePlugins(PlayScala) dependsOn(spring)

lazy val http = project in file("http") enablePlugins(PlayJava) dependsOn(spring)

lazy val security = project in file("security") enablePlugins(PlayJava) dependsOn(spring)

lazy val avro = project in file("avro") enablePlugins(PlayScala) dependsOn(spring, http)

lazy val avroD2 = project in file("avro-d2") enablePlugins(PlayScala) dependsOn(avro)

lazy val oauth2 = project in file("oauth2") enablePlugins(PlayJava) dependsOn(security, avro)

lazy val kafka = project in file("kafka") enablePlugins(PlayJava) dependsOn(avro)

lazy val mongodb = project in file("mongodb") enablePlugins(PlayJava) dependsOn(avro)

lazy val titan = project in file("titan") enablePlugins(PlayJava) dependsOn(mongodb)
