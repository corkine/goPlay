FROM openjdk:11.0.11-jdk
MAINTAINER corkine<corkine@outlook.com>
WORKDIR /goPlay
COPY target/scala-2.12/go-assembly-2.1.12.jar /goPlay/go.jar
COPY public/ip2region.db /goPlay/public/ip2region.db
COPY application.conf /goPlay/application.conf
COPY goPlayDB.mv.db /root/goPlayDB.mv.db
# 注意 Docker 不支持 ~/goPlayDB.mv.db 这种写法
ENV TZ=Asia/ShangHai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
EXPOSE 8080
CMD ["java", "-XX:+UseSerialGC", "-Dconfig.file=application.conf", "-jar", "go.jar"]