FROM eclipse-temurin:17-jdk-alpine
COPY . /app
WORKDIR /app
RUN javac -cp jsoup-1.22.2.jar App.java
CMD ["java", "-cp", ".:jsoup-1.22.2.jar", "App"]
