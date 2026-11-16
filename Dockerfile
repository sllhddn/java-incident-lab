# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app && mkdir /dumps && chown app /dumps
COPY --from=build /src/target/java-incident-lab-*.jar app.jar
USER app
EXPOSE 8080
# Heap dumps on OOM go to a volume, so the evidence survives the restart.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps -Xlog:gc*:file=/dumps/gc.log:time,uptime:filecount=3,filesize=10m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
