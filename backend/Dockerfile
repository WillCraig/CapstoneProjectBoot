# Runtime-only image: CI supplies the Maven-verified JAR in target/.
# This preserves the package-once rule; the image build does not rebuild code.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --chown=spring:spring target/crm-backend-*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
