# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# The SQLite file lives here. Mount a persistent volume at /data or the data is lost on redeploy.
ENV DB_PATH=/data/realestate.db
RUN mkdir -p /data
VOLUME /data

# JVM sizing for containers; add extra flags through JAVA_OPTS (e.g. -Xmx384m on small plans)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
# The container runs as root on purpose: managed hosts (Railway, Render, Fly.io) often mount
# volumes owned by root, and a non-root user would then be unable to write the database file.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
