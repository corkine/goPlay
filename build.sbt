name := "go"
 
version := "1.0" 
      
lazy val `go` = (project in file(".")).enablePlugins(PlayScala,LauncherJarPlugin)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache , ws , specs2 % Test , guice,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "com.h2database" % "h2" % "1.4.199",
  "mysql" % "mysql-connector-java" % "5.1.41"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case  manifest  if  manifest.contains("MANIFEST.MF")  =>
    MergeStrategy.discard
  case  moduleInfo  if  moduleInfo.contains("module-info.class")  =>
    MergeStrategy.discard
  case  referenceOverrides  if  referenceOverrides.contains("reference-overrides.conf")  =>
    MergeStrategy.concat
  case  x  =>
    val  oldStrategy  =  (assemblyMergeStrategy  in  assembly).value
    oldStrategy(x)
}