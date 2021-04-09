# Go 短网址跳转服务 by Play

短链接跳转服务器，部署在 go.mazhangjing.com，项目基于 Scala Playframework，Slick ORM + H2 Database。用于替代现有的基于 Spring Boot 和 JPA 的实现。

## 前端界面

### 基本的 HTML 视图

Play 后端使用 RESTful API 提供服务，前端可直接通过 go.mazhangjing.com/xxx 进行跳转，也可以通过 mazhangjing.com/xxx 先跳转到 go.mazhangjing.com，然后进行二次跳转。
提供了一个基于 Vue 的管理页面：https://go.mazhangjing.com/manage。 其需要进行 Token 验证，可向数据库进行增、删、改、精准查找操作。
此外，基于 URL 暴露了一个快速的查找功能：https://go.mazhangjing.com/???/all[alljson]  可返回 Bootstrap 框架 HTML 或 JSON 格式的关于 ??? 的数据库搜索结果，其需要 Basic 认证登录。

详情参见 app/views/{xxx}.scala.html 所提供的 HTML 视图和 conf/route 路由文件。

![](http://static2.mazhangjing.com/20210409/d6388f3_截屏2021-04-09上午10.36.30.png)

### iOS 快捷方式提供的快捷视图

基于 Play 的 RESTful API，通过 iOS 的 "快捷方式" APP 提供了任意网址通过"分享"菜单快速创建对应短链接的功能。因为此 APP 需要硬编码用户名和密码，因此不做分享，详情请联系 concat@mazhanjging.com。

![](http://static2.mazhangjing.com/20210409/a969d67_截屏2021-04-09上午10.41.26.png)

### 基于 Flutter 的客户端视图

goPlay 所提供的服务被整合到 Flutter 客户端 'CyberMe' 中，Flutter 提供了丰富的动画、反馈以及流畅的操作体验，此客户端会自动保存当前偏好设置、记录当前登录用户和秘钥、自动滚动到上次浏览位置、
提供快速的短链接添加、更好体验的搜索和信息呈现能力。

关于此 Flutter 项目参见 [CyberMe](https://gitee.com/corkine/cyberMe) 。

![](http://static2.mazhangjing.com/20210409/e3349cb_截屏2021-04-09上午10.43.40.png)

## 使用技术

![](http://static2.mazhangjing.com/badge/scala.png)
![](http://static2.mazhangjing.com/badge/play.png)
![](http://static2.mazhangjing.com/badge/slick.png)
![](http://static2.mazhangjing.com/badge/akka.png)