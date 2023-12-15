ARG REGISTRY="azsdkengsys.azurecr.io"
FROM ${REGISTRY}/java/jdk-mariner-mvn:jdk11-latest as builder

RUN yum update -y

RUN mkdir /stress-eh
WORKDIR /stress-eh

ADD ./sdk/tools /stress-eh/sdk/tools
ADD ./sdk/parents /stress-eh/sdk/parents
ADD ./sdk/core /stress-eh/sdk/core
ADD ./sdk/eventhubs /stress-eh/sdk/eventhubs
ADD ./eng /stress-eh/eng

RUN --mount=type=cache,target=/root/.m2 \
 mvn -f /stress-eh/eng/code-quality-reports/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/tools/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/core/azure-core/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/core/azure-core-test/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/core/azure-core-amqp/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/core/azure-core-http-netty/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/eventhubs/azure-messaging-eventhubs/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/eventhubs/azure-messaging-eventhubs-checkpointstore-blob/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests && \
 mvn -f /stress-eh/sdk/eventhubs/azure-messaging-eventhubs-stress/pom.xml clean install -Dcheckstyle.skip -Dgpg.skip -Dmaven.javadoc.skip -Drevapi.skip -Dspotbugs.skip -Djacoco.skip -DskipTests

FROM mcr.microsoft.com/openjdk/jdk:11-mariner

RUN yum update -y

WORKDIR /app
COPY --from=builder /stress-eh/sdk/eventhubs/azure-messaging-eventhubs-stress/target .

ARG APPLICATION_INSIGHTS_AGENT_VERSION=3.4.18
ARG AGENT_URL=https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPLICATION_INSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APPLICATION_INSIGHTS_AGENT_VERSION}.jar
ADD ${AGENT_URL} ./applicationinsights-agent.jar

# This is never executed (since job yaml overrides it)
ENTRYPOINT ["bash"]
