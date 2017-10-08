organization in ThisBuild := "sample.carrental"

scalaVersion in ThisBuild := "2.11.8"

lazy val carRentalApi = project("carrental-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lagomJavadslJackson
    )
  )

lazy val carRentalImpl = project("carrental-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomJavadslPersistence,
      lagomJavadslPubSub,
      lagomJavadslTestKit
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(carRentalApi, utils)


lazy val utils = project("utils")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslApi
  )

def project(id: String) = Project(id, base = file(id))
  .settings(
    scalacOptions in Compile += "-Xexperimental" // this enables Scala lambdas to be passed as Java SAMs  
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.3" // actually, only api projects need this
    )
  )

// do not delete database files on start
lagomCassandraCleanOnStart in ThisBuild := false

// set up information for where to publish our bundles to
// (see http://conductr.lightbend.com/docs/1.1.x/CreatingBundles#Publishing-bundles for more
// information)
licenses in ThisBuild := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
bintrayVcsUrl in Bundle in ThisBuild := Some("https://github.com/lagom/activator-lagom-scala-chirper")
bintrayOrganization in Bundle in ThisBuild := Some("typesafe")
bintrayReleaseOnPublish in Bundle in ThisBuild := true
