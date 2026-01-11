#!/bin/bash

# ================================
# NeoGuard Startup Script
# ================================
# Compatible with both local development and container deployment

# Environment variables with defaults
SERVER_PORT="${SERVER_PORT:-8080}"
MAX_MEMORY="${MAX_MEMORY:-2G}"
JAVA_OPTS="${JAVA_OPTS:-}"

echo ""
echo "  ╔══════════════════════════════════════════════════════════╗"
echo "  ║                    NeoGuard Startup                      ║"
echo "  ╚══════════════════════════════════════════════════════════╝"
echo ""
echo "  Configuration:"
echo "    • Server Port: ${SERVER_PORT}"
echo "    • Max Memory:  ${MAX_MEMORY}"
echo ""

# Create required directories if they don't exist
mkdir -p data uploads output configs libs mappings 2>/dev/null

# Check if running in container (skip port killing)
if [ -z "${CONTAINER_MODE}" ] && [ -f "/proc/1/cgroup" ]; then
    # Might be in container, check
    if grep -q docker /proc/1/cgroup 2>/dev/null || grep -q kubepods /proc/1/cgroup 2>/dev/null; then
        CONTAINER_MODE="true"
    fi
fi

# Only attempt to kill port if not in container mode
if [ "${CONTAINER_MODE}" != "true" ]; then
    echo "  Checking port ${SERVER_PORT}..."
    PID=$(lsof -ti:${SERVER_PORT} 2>/dev/null)
    if [ ! -z "$PID" ]; then
        echo "  Stopping process $PID on port ${SERVER_PORT}..."
        kill -9 $PID 2>/dev/null
        sleep 1
    fi
fi

echo "  Starting NeoGuard..."
echo ""

# Determine JAR file location
if [ -f "neoguard.jar" ]; then
    JAR_FILE="neoguard.jar"
elif [ -f "target/neo-guard-1.0.0.jar" ]; then
    JAR_FILE="target/neo-guard-1.0.0.jar"
else
    echo "  ERROR: Could not find NeoGuard JAR file!"
    exit 1
fi

# Start the application with proper JVM settings
exec java \
    -Xms128M \
    -Xmx${MAX_MEMORY} \
    -Dterminal.jline=false \
    -Dterminal.ansi=true \
    ${JAVA_OPTS} \
    -jar ${JAR_FILE} \
    --server.port=${SERVER_PORT}
