FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src src
COPY ../proto proto
RUN chmod +x gradlew && ./gradlew build -x test -x detekt -x lintKotlin -PquarkusBuild

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/quarkus-app /app
EXPOSE 9090
CMD ["java", "-jar", "quarkus-run.jar"]
