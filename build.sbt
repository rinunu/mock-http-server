name := "mock-http-server"

scalaVersion := "2.9.1"

organization := "nu.rinu"

publishTo <<= (version) { version: String =>
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots") 
  else                                   Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

libraryDependencies ++= {
  Seq(
    "org.eclipse.jetty" %  "jetty-server" % "8.1.0.v20120127",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test",
    "junit" % "junit" % "4.10" % "test",
    "org.mockito" % "mockito-all" % "1.9.0" % "test;compile",
    "org.apache.httpcomponents" % "httpclient" % "4.1.2" % "test",
    "ch.qos.logback" % "logback-classic" % "1.0.0" % "test;compile"
    )
}

seq(sbtrelease.Release.releaseSettings: _*)

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("https://github.com/rinunu/mock-http-server"))

pomExtra :=
<parent>
    <groupId>org.sonatype.oss</groupId>
    <artifactId>oss-parent</artifactId>
    <version>7</version>
</parent>
<scm>
  <connection>scm:git:git@github.com:rinunu/mock-http-server.git</connection>
  <developerConnection>scm:git:git@github.com:rinunu/mock-http-server.git</developerConnection>
  <url>git@github.com:rinunu/mock-http-server.git</url>
</scm>
<developers>
  <developer>
    <id>rinunu</id>
    <name>Rintaro Tsuchihashi</name>
    <url>https://github.com/rinunu</url>
  </developer>
</developers>
