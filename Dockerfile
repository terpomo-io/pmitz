# Build stage
FROM gradle:8-jdk17 AS builder

WORKDIR /app
COPY . .

# Build the remoteserver JAR
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage  
FROM openjdk:17

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/remoteserver/build/libs/pmitz-remoteserver-*-SNAPSHOT.jar app.jar

# Install curl for health checks
RUN microdnf update -y && microdnf install -y curl && microdnf clean all

# Create non-root user
RUN groupadd -r pmitz && useradd -r -g pmitz pmitz
RUN chown pmitz:pmitz /app
USER pmitz

EXPOSE 8080

# Generate API key and start application
CMD ["sh", "-c", "if [ -z \"$PMITZ_API_KEY\" ]; then export PMITZ_API_KEY=$(openssl rand -hex 32); echo 'Generated API Key:' $PMITZ_API_KEY; else echo 'Using provided API Key:' $PMITZ_API_KEY; fi && java -jar app.jar"]
