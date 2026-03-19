FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/garage-backend-1.0.0.jar app.jar
EXPOSE 3003
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
