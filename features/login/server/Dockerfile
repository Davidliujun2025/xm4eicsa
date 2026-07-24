FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY maven-settings-cn.xml .
RUN mvn -s maven-settings-cn.xml -B -ntp -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -s maven-settings-cn.xml -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/ai-customer-service-login-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
