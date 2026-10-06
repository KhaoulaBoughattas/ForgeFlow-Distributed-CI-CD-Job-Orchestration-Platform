# Build stage: compiles the whole multi-module reactor so forgeflow-common is available to
# forgeflow-api, but only packages forgeflow-api (-pl ... -am resolves and builds its
# dependencies too, i.e. forgeflow-common).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY pom.xml ./
COPY forgeflow-common/pom.xml forgeflow-common/pom.xml
COPY forgeflow-api/pom.xml forgeflow-api/pom.xml
COPY forgeflow-worker/pom.xml forgeflow-worker/pom.xml

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY forgeflow-common/src forgeflow-common/src
COPY forgeflow-api/src forgeflow-api/src

RUN mvn -pl forgeflow-api -am package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin forgeflow
COPY --from=build /workspace/forgeflow-api/target/forgeflow-api.jar app.jar
USER forgeflow

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
