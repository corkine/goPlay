name := "go"
 
version := "2.1.9"

description :=
  """
    |[Version 1.3] 2020-08-22 添加了使用 token 的认证流程，在避免使用 Cookie 的情况下，实现前端明文传递 Token 以使用后端服务，同时保证了暴露 Token 的安全性和可靠性(无法从中得到用户密码，无法延长有效期)。做到了同一套 API 支持三种验证方式，直接传递 user 和 password（测试用），传递 Basic 认证（用户用），使用 Token 认证（前端 AJAX）。
    |[Version 1.3.1] 2020-08-22 修复了访问页面权限的问题，现在访问所有记录，以及删除记录都需要 Admin 权限。
    |[Version 2.0.0] 2020-08-24 整合了 Vue 开发的管理页面，添加了几个便捷的 API。
    |[Version 2.1.0] 2020-08-24 提供了管理页面 Token 的自动加载和验证功能。
    |[Version 2.1.3] 2020-10-15 恢复了之前的 URL 编码方式，放弃对中文编码（交给 JS）。添加 CM(cm) 开头短连接直接跳转到 CMGOOD 系统。
    |[Version 2.1.4] 2020-12-24 实现了跳转日志系统，每个短链接都会记录来访者
    |[Version 2.1.5] 2020-12-24 紧急修复查看 /logs 数据库模型和 Slick 模型不一致问题
    |[Version 2.1.6] 2021-01-14 添加了 temp/before 临时文件的映射规则
    |[Version 2.1.7] 2021-02-17 丰富了 /logs 的显示选项，更加易读
    |[Version 2.1.8] 2021-02-17 提供了 /logs IP 地址识别选项
    |[Version 2.1.9] 2021-02-18 修复了 API 参数不起作用的问题
    |""".stripMargin
      
lazy val `go` = (project in file(".")).enablePlugins(PlayScala,LauncherJarPlugin)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache , ws , specs2 % Test , guice,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "com.h2database" % "h2" % "1.4.199"
)
// https://mvnrepository.com/artifact/org.lionsoul/ip2region
libraryDependencies += "org.lionsoul" % "ip2region" % "1.7.2"
//"mysql" % "mysql-connector-java" % "5.1.41"

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