# BUILD
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Copy build files first so dependency downloads are cached between code changes.
COPY mvnw ./mvnw
COPY .mvn/ .mvn/
COPY pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
    && cp target/*.jar /workspace/app.jar

# RUNTIME
FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system --gid 1000 app \
    && useradd --system --uid 1000 --gid app --home-dir /app appuser

WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

COPY --from=builder /workspace/app.jar ./app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
