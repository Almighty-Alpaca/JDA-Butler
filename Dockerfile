FROM adoptopenjdk:8-jdk-hotspot AS builder

WORKDIR /butler

COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies
COPY . .
RUN ./gradlew --no-daemon :bot:build

FROM adoptopenjdk:8-jre-hotspot

WORKDIR /butler
COPY --from=builder /butler/bot/build/libs/Bot-all.jar ./butler.jar

CMD ["java", "-jar", "butler.jar", "--env-config"]
