<h1 align="center">🛡️ NeoGuard</h1>

<p align="center">
  <strong>Professional Java Obfuscation Web Application</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#deployment">Deployment</a> •
  <a href="#documentation">Documentation</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-green?style=for-the-badge&logo=spring" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Skidfuscator-Powered-blue?style=for-the-badge" alt="Skidfuscator"/>
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License"/>
</p>

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎯 Core Features
- **Drag & Drop Upload** - Simple JAR file upload
- **Real-time Progress** - Live obfuscation status
- **History Tracking** - View all past obfuscations
- **Auto-cleanup** - Automatic file management

</td>
<td width="50%">

### 🔐 Obfuscation Options
- **String Encryption** - Encrypt all string literals
- **Number Encryption** - Obfuscate numeric constants
- **Flow Obfuscation** - Control flow flattening
- **Multiple Transformers** - Full Skidfuscator power

</td>
</tr>
</table>

### 🎨 Modern UI
- **Glassmorphism Design** - Sleek, modern interface
- **Lime Accent Theme** - Eye-catching neon glow effects
- **Responsive Layout** - Works on all screen sizes
- **Real-time Feedback** - Toast notifications & live logs

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK, not JRE)
- **Maven 3.6+** (for building)

### Option 1: Pre-built JAR

```bash
# 1. Download the latest release
# 2. Download Skidfuscator JAR and place in libs/
mkdir libs
# Place skidfuscator.jar in libs/

# 3. Run NeoGuard
java -jar neo-guard-1.0.0.jar
```

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/joealjohn/Neo-Guard.git
cd Neo-Guard

# Build
mvn clean package -DskipTests

# Run
./start.sh    # Linux/Mac
start.bat     # Windows
```

### 📂 Required Structure
```
Neo-Guard/
├── neo-guard-1.0.0.jar    # Main application
├── libs/
│   └── skidfuscator.jar   # ⚠️ REQUIRED - Download separately
├── uploads/               # Auto-created
├── output/                # Auto-created
└── data/                  # Auto-created (SQLite DB)
```

> ⚠️ **Important**: You must download [Skidfuscator](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator) separately and place it in the `libs/` folder.

---

## 🐳 Deployment

### Docker

```bash
# Build image
docker build -t neoguard:latest .

# Run container
docker run -d \
  --name neoguard \
  -p 8080:8080 \
  -v neoguard-data:/app/data \
  neoguard:latest
```

### Docker Compose

```bash
docker-compose up -d
```

### Pterodactyl Panel

1. Go to **Admin Panel** → **Nests** → **Import Egg**
2. Upload `pterodactyl/egg-neoguard.json`
3. Create a new server using the **NeoGuard** egg
4. Start and access at the assigned port

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

---

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Web UI port |
| `MAX_MEMORY` | `2G` | JVM heap size |
| `MAX_FILE_SIZE` | `100MB` | Max upload size |

### application.yml

```yaml
server:
  port: ${SERVER_PORT:8080}

neo:
  upload-dir: ./uploads
  output-dir: ./output
  skidfuscator-jar: ./libs/skidfuscator.jar
```

---

## 📖 Documentation

### How to Obfuscate

1. **Upload** - Drag your `.jar` file onto the upload zone
2. **Configure** - Set main package, Java version, and transformers
3. **Obfuscate** - Click the Obfuscate button
4. **Download** - Get your protected JAR from the history

<p align="center">
  <img width="1919" height="958" alt="image" src="https://github.com/user-attachments/assets/4ca29c60-ac52-43a6-ba89-a69018a34ff4" />
</p>

### Available Transformers

| Transformer | Description |
|-------------|-------------|
| String Encryption | Encrypts all string literals |
| Number Encryption | Obfuscates numeric constants |
| Flow Condition | Adds opaque predicates |
| Flow Exception | Uses exception handling for obfuscation |
| Flow Range | Range-based flow obfuscation |
| Flow Switch | Switch-based flow flattening |

---

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.2, Java 17
- **Frontend**: Vanilla JS, CSS with Glassmorphism
- **Database**: SQLite
- **Obfuscator**: Skidfuscator

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

- [Skidfuscator](https://github.com/skidfuscatordev/skidfuscator-java-obfuscator) - The obfuscation engine powering NeoGuard
- Built with ❤️ by [@joealjohn](https://github.com/joealjohn)

---

<p align="center">
  <sub>Made with ☕ and 💚</sub>
</p>
