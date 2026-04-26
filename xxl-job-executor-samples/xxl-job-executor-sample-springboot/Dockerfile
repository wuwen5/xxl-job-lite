# Perform the extraction in a separate builder container
FROM eclipse-temurin:17-jre AS builder
WORKDIR /builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract 

# This is the runtime container
FROM eclipse-temurin:17-jre
WORKDIR /application
ENV PARAMS=""
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY --from=builder /builder/dependencies/ ./
COPY --from=builder /builder/spring-boot-loader/ ./
COPY --from=builder /builder/snapshot-dependencies/ ./
COPY --from=builder /builder/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
