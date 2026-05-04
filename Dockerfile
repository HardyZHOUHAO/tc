FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 下载 Jsoup 和 Gson
ADD https://repo1.maven.org/maven2/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar /app/jsoup.jar
ADD https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar /app/gson.jar

COPY App.java /app/

RUN javac -cp "jsoup.jar:gson.jar" App.java

CMD ["java", "-cp", ".:jsoup.jar:gson.jar", "App"]
