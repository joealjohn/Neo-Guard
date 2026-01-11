# NeoGuard - VPS Deployment Guide

Complete guide for deploying NeoGuard on a VPS server.

---

## Quick Start Options

### Option 1: Pterodactyl Panel (Recommended)

1. Import the egg in your Pterodactyl admin panel:
   - Go to **Nests** → **Import Egg**
   - Upload `pterodactyl/egg-neoguard.json`

2. Create a new server using the **NeoGuard** egg

3. Start the server - it will auto-install NeoGuard and Skidfuscator

4. Access the web UI at `http://your-server:8080`

---

### Option 2: Docker Compose

```bash
# Clone or copy the project files
cd neo-obfuscator

# Build and start
docker-compose up -d

# View logs
docker-compose logs -f neoguard
```

Access at `http://localhost:8080`

---

### Option 3: Docker Standalone

```bash
# Build the image
docker build -t neoguard:latest .

# Run the container
docker run -d \
  --name neoguard \
  -p 8080:8080 \
  -v neoguard-data:/app/data \
  -v neoguard-uploads:/app/uploads \
  -v neoguard-output:/app/output \
  neoguard:latest
```

---

### Option 4: Manual Installation

```bash
# Requirements: Java 17+

# Create directories
mkdir -p data uploads output configs libs mappings

# Download/copy NeoGuard JAR to current directory
cp neo-guard-1.0.0.jar neoguard.jar

# Download Skidfuscator
curl -Lo libs/skidfuscator.jar \
  https://github.com/skidfuscatordev/skidfuscator-java-obfuscator/releases/latest/download/skidfuscator.jar

# Make start script executable
chmod +x start.sh

# Start NeoGuard
./start.sh
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP port for web UI |
| `MAX_MEMORY` | `2G` | Maximum JVM heap size |
| `MAX_FILE_SIZE` | `100MB` | Max upload file size |
| `DATA_DIR` | `./data` | SQLite database directory |
| `UPLOAD_DIR` | `./uploads` | Uploaded files directory |
| `OUTPUT_DIR` | `./output` | Obfuscated files directory |
| `SKIDFUSCATOR_JAR` | `./libs/skidfuscator.jar` | Path to Skidfuscator |

---

## Reverse Proxy (Nginx)

```nginx
server {
    listen 80;
    server_name obfuscator.yourdomain.com;

    client_max_body_size 100M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## Troubleshooting

**Port already in use:**
```bash
# Find and kill the process
lsof -ti:8080 | xargs kill -9
```

**Out of memory:**
```bash
# Increase MAX_MEMORY
export MAX_MEMORY=4G
./start.sh
```

**Skidfuscator not found:**
- Ensure `libs/skidfuscator.jar` exists
- Check file permissions

---

## Support

- GitHub Issues: [Report a bug](https://github.com/yourusername/neoguard/issues)
- Documentation: [Read the docs](https://github.com/yourusername/neoguard/wiki)
