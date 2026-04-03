FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY backend/gradle gradle
COPY backend/gradlew backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/src src
COPY proto proto
RUN chmod +x gradlew && ./gradlew build -x test -x detekt -x lintKotlin -PquarkusBuild

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/quarkus/app /app
EXPOSE 9090
CMD ["java", "-jar", "quarkus-run.jar"]
