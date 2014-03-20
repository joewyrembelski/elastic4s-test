import sbt._

object Dependencies {

  val elastic      = "org.elasticsearch" % "elasticsearch" % "1.0.1"
  val elastic4s    = "com.sksamuel.elastic4s" %%  "elastic4s" % "1.0.1.1"

  val log4j        = "log4j" % "log4j" % "1.2.17"

  val log4jslf4j   = "org.slf4j" % "log4j-over-slf4j" % "1.6.6"

  val buildDeps = Seq(elastic, elastic4s, log4j, log4jslf4j)
}