name := "Agitators Server"

version := "0.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo" at "http://repo.spray.io/")

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "1.0.1-RC1",
  //"io.spray" %%  "spray-json" % "1.2.5",
  "org.json4s" %% "json4s-jackson" % "3.2.5",
  "com.typesafe.akka" %% "akka-actor" % "2.3-M1",
  "io.backchat.hookup" %% "hookup" % "0.2.3")

initialCommands in console := """
  import akka.actor._
  import akka.routing._
  import akka.event.Logging
 import org.json4s._
 import org.json4s.JsonDSL._
 import org.json4s.jackson.JsonMethods._
 import org.json4s.jackson.Serialization.{read, write}
implicit val formats = DefaultFormats
""".stripMargin


