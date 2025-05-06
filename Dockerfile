FROM openjdk:21-jdk-slim
WORKDIR /app

COPY build/libs/HTTPaWOL-*-all.jar /app/HTTPaWOL.jar

EXPOSE 8080

CMD ["java", "-jar", "HTTPaWOL.jar"]