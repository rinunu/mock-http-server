name := "mock-http-server"

scalaVersion := "2.9.1"

libraryDependencies ++= {
  Seq(
    "org.eclipse.jetty" %  "jetty-server" % "8.1.0.v20120127",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test",
    "junit" % "junit" % "4.10" % "test",
    "org.mockito" % "mockito-all" % "1.9.0" % "test;compile",
    "org.apache.httpcomponents" % "httpclient" % "4.1.2" % "test"
    )
}

organization := "nu.rinu"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
