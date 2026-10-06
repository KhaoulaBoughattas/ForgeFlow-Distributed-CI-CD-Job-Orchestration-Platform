# Build stage: compiles the whole multi-module reactor so forgeflow-common is available to
# forgeflow-worker, but only packages forgeflow-worker (-pl ... -am resolves and builds its
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
COPY forgeflow-worker/src forgeflow-worker/src

RUN mvn -pl forgeflow-worker -am package -DskipTests -B

# Runtime stage. NOTE: this image must run with access to the host's Docker socket
# (-v /var/run/docker.sock:/var/run/docker.sock) because DockerJobExecutor and
# GitWorkspaceCloner shell out to the `docker` CLI to run pipeline steps and clone
# repositories. We therefore install the docker CLI here and run as root; mounting the
# host docker.sock into a container is a known, documented security trade-off (see
# README.md "Security" section and deploy/k8s/06-worker.yaml) because it grants the
# worker container effective root-equivalent control of the host's Docker daemon.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/forgeflow-worker/target/forgeflow-worker.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
