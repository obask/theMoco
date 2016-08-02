name := "theMoCo"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.twitter" %% "finagle-http" % "6.36.0"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.4.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

mainClass in Compile := Some("Main")
