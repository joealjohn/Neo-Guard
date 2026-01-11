# ================================
# NeoGuard - Production Dockerfile
# ================================

# Stage 1: Build (if needed, we already have the JAR)
FROM eclipse-temurin:17-jre-alpine AS runtime

# Labels
LABEL maintainer="joeal"
LABEL description="NeoGuard - Java Obfuscation Web Application"
LABEL version="1.0.0"

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/data /app/uploads /app/output /app/configs /app/libs /app/mappings

# Copy the pre-built JAR
COPY target/neo-guard-1.0.0.jar /app/neoguard.jar

# Copy Skidfuscator library
COPY libs/skidfuscator.jar /app/libs/skidfuscator.jar

# Copy startup script
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Environment variables with defaults
ENV SERVER_PORT=8080
ENV MAX_MEMORY=2G
ENV JAVA_OPTS=""

# Expose the port
EXPOSE ${SERVER_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:${SERVER_PORT}/api/health || exit 1

# Run as non-root user for security
RUN addgroup -g 1000 neoguard && \
    adduser -u 1000 -G neoguard -s /bin/sh -D neoguard && \
    chown -R neoguard:neoguard /app

USER neoguard

# Start the application
ENTRYPOINT ["/app/start.sh"]
