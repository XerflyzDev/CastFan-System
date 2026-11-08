FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
	&& apt-get install --only-upgrade --yes apt libapt-pkg6.0 \
	&& rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/castfan-*.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
