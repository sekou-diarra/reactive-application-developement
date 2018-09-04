name := "reactiv-eapplication-development"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies := Seq( "com.typesafe.akka" %% "akka-actor" % "2.5.15",
                            "com.typesafe.akka" %% "akka-remote" %"2.5.15",
                            "com.typesafe.akka" %% "akka-testkit" % "2.5.15" % Test)
