version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.13"

name := "Nino"

javaOptions += "--add-exports"
javaOptions += "java.base/sun.nio.ch=ALL-UNNAMED"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.5.0"
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.3"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.1.1"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.8.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

// libraryDependencies += "com.h2database" % "h2" % "2.2.234"