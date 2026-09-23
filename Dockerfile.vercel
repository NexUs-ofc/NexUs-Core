FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
COPY config ./config
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests


FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system nexus \
    && useradd --system --gid nexus --no-create-home nexus

WORKDIR /app

COPY --from=build --chown=nexus:nexus /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

USER nexus

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
