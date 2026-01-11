# NeoGuard - Local Development Setup

## Quick Start (2 Steps)

```bash
# 1. Build (requires Maven)
mvn clean package -DskipTests

# 2. Run (auto-kills any process on port 8080)
start.bat
```

## Alternative (Manual)

```bash
java -jar target/neo-guard-1.0.0.jar
```

## Requirements

- **Java 17+** (JDK, not JRE)
- **Maven 3.6+** (for building)
- **Skidfuscator JAR** at `libs/skidfuscator.jar`

## Important Notes

1. **Always run from the project root directory** - The app needs to find `libs/skidfuscator.jar`
2. **Auto-cleanup enabled** - Error files (`skidfuscator-error-*.txt`) are deleted every 5 minutes
3. **Port 8080** - Change in `application.yml` if needed

## If Something Goes Wrong

**Port in use:**
```powershell
netstat -ano | findstr :8080
taskkill /F /PID <pid>
```

**Skidfuscator not found:**
- Make sure you're running from `neo-obfuscator/` directory
- Check that `libs/skidfuscator.jar` exists

**Build fails:**
```bash
mvn clean package -DskipTests -e
```

## Configuration

Edit `src/main/resources/application.yml` for:
- Port number
- File size limits
- Directory paths
