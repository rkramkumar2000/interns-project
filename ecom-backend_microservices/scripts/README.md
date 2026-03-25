# Microservices Scripts

This directory contains utility scripts for managing the e-commerce microservices platform.

## 📋 Available Scripts

### Prerequisites Check

#### 🔍 check-prerequisites.sh / check-prerequisites.ps1
Verifies all required software is installed and configured.

```bash
# Linux/Mac
./scripts/check-prerequisites.sh

# Windows PowerShell
.\scripts\check-prerequisites.ps1
```

**What it checks:**
- Required software (Docker, Git)
- Optional development tools (Java, Maven, Node.js)
- System resources (RAM - 8GB recommended for microservices)
- Port availability (3000, 8080-8086, 5432, 6379, 9092, etc.)
- Environment setup (.env file)
- Docker daemon status

### Docker Management (Recommended)

#### 🐳 docker-start.sh / docker-start.ps1
Starts all microservices using Docker Compose.

```bash
# Linux/Mac
./scripts/docker-start.sh

# Windows PowerShell
.\scripts\docker-start.ps1
```

**What it does:**
- Checks for .env file (creates if missing)
- Starts all services with Docker Compose
- Waits for health checks on all services
- Displays access URLs for all services
- Includes all microservices, infrastructure, and monitoring

**Services started:**
- PostgreSQL, Redis, Kafka, Elasticsearch
- Config Server, Eureka Server
- API Gateway
- All microservices (Auth, Product, Cart, Order, Payment, Notification)
- Frontend (React with NGINX)
- Monitoring (Prometheus, Grafana, Zipkin)

#### 🛑 docker-stop.sh / docker-stop.ps1
Stops all Docker containers with cleanup options.

```bash
# Linux/Mac
./scripts/docker-stop.sh

# Windows PowerShell
.\scripts\docker-stop.ps1
```

**What it does:**
- Stops all containers
- Optionally removes volumes (deletes all data)
- Optionally cleans up dangling images

### Local Development Scripts

#### 🚀 start-all-services.sh / start-all-services.ps1
Starts microservices locally using Maven (infrastructure must be running in Docker).

```bash
# Linux/Mac
./scripts/start-all-services.sh

# Windows PowerShell
.\scripts\start-all-services.ps1
```

**Prerequisites:**
- Infrastructure services running in Docker
- Java 17+ installed
- Maven installed

#### 🛑 stop-all-services.sh / stop-all-services.ps1
Stops locally running microservices.

```bash
# Linux/Mac
./scripts/stop-all-services.sh

# Windows PowerShell
.\scripts\stop-all-services.ps1
```

### Utility Scripts

#### 📦 copy-dockerignore.sh / copy-dockerignore.ps1
Copies .dockerignore template to all microservices.

```bash
# Linux/Mac
./scripts/copy-dockerignore.sh

# Windows PowerShell
.\scripts\copy-dockerignore.ps1
```

#### 📊 load-sample-data.sh / load-sample-data.ps1
Loads sample data into databases.

```bash
# Linux/Mac
./scripts/load-sample-data.sh

# Windows PowerShell
.\scripts\load-sample-data.ps1
```

#### 🔧 add-swagger-resilience.ps1
Adds Swagger and Resilience4j dependencies to all services.

```powershell
.\scripts\add-swagger-resilience.ps1
```

#### 📝 add-logstash-encoder.ps1
Adds Logstash encoder for centralized logging.

```powershell
.\scripts\add-logstash-encoder.ps1
```

## 🎯 Quick Start with Docker

1. **First time setup:**
   ```bash
   # Create .env file
   echo "STRIPE_SECRET_KEY=your_key_here" > .env
   
   # Make scripts executable (Linux/Mac)
   chmod +x scripts/*.sh
   
   # Start everything
   ./scripts/docker-start.sh
   ```

2. **Access the application:**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - Grafana: http://localhost:3001
   - Zipkin: http://localhost:9411

3. **Stop everything:**
   ```bash
   ./scripts/docker-stop.sh
   ```

## 🔧 Prerequisites

### For Docker Scripts
- Docker Desktop or Docker Engine
- Docker Compose v2.0+
- 8GB RAM minimum (16GB recommended)

### For Local Development Scripts
- Java 17+
- Maven 3.8+
- Node.js 18+ (for frontend)
- Docker (for infrastructure)

## 📊 Service Ports Reference

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React UI via NGINX |
| API Gateway | 8080 | Main entry point |
| Eureka Server | 8761 | Service discovery |
| Config Server | 8888 | Configuration management |
| Auth Service | 8081 | Authentication |
| Product Service | 8082 | Product management |
| Cart Service | 8083 | Shopping cart |
| Order Service | 8084 | Order processing |
| Payment Service | 8085 | Payment processing |
| Notification Service | 8086 | Email notifications |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |
| Kafka | 9092 | Message broker |
| Elasticsearch | 9200 | Search engine |
| Kibana | 5601 | Log visualization |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Metrics visualization |
| Zipkin | 9411 | Distributed tracing |

## 🛠️ Troubleshooting

### Docker Issues

**Permission Denied:**
```bash
chmod +x scripts/*.sh
```

**Port Already in Use:**
```bash
# Find process using port
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows
```

**Out of Memory:**
- Increase Docker Desktop memory to 8GB+
- Stop unnecessary services

**Container Won't Start:**
```bash
# Check logs
docker-compose -f docker-compose-microservices.yml logs service-name
```

### Local Development Issues

**Service Won't Start:**
- Check if port is already in use
- Verify infrastructure services are running
- Check logs in `logs/` directory

**Database Connection Failed:**
```bash
# Verify PostgreSQL is running
docker ps | grep postgres
```

## 📚 Related Documentation

- [Microservices Architecture](../MICROSERVICES_ARCHITECTURE.html)
- [Docker Documentation](../docs/tech/DOCKER.md)
- [Microservices Starter Guide](../docs/setup/MICROSERVICES_STARTER_GUIDE.md)
- [Developer Quick Reference](../docs/development/DEVELOPER_QUICK_REFERENCE.md)

## 💡 Tips

1. **Development Workflow**: Use Docker for infrastructure, run services locally for faster development
2. **Logs**: All logs are in the `logs/` directory when running locally
3. **Database Access**: Connect to PostgreSQL at `localhost:5432`
4. **Service Discovery**: Check Eureka at http://localhost:8761 to see registered services
5. **API Testing**: Use Swagger UI through the gateway at http://localhost:8080/swagger-ui.html

## 🔍 Script Details

### Environment Variables
Create `.env` file in the microservices root:
```env
STRIPE_SECRET_KEY=your_stripe_secret_key_here
```

### Making Scripts Executable
```bash
# Linux/Mac only
chmod +x scripts/*.sh
```

### Running Scripts
- **Linux/Mac**: Use `./scripts/script-name.sh`
- **Windows PowerShell**: Use `.\scripts\script-name.ps1`
- **Windows Git Bash**: Use `./scripts/script-name.sh` 