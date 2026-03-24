# Lost and Found Application

A Spring Boot application for managing lost and found items.

## Tech Stack

- **Backend**: Spring Boot 4.0, Kotlin, Spring Data JDBC
- **Database**: PostgreSQL 16
- **Migrations**: Flyway
- **Build**: Gradle with Jib (Docker image creation)
- **Testing**: Testcontainers with PostgreSQL
- **Frontend**: Simple HTML/CSS/JavaScript

## Development

### Prerequisites

- JDK 17+
- Docker and Docker Compose

### Local Development

1. **(Optional) Configure environment variables:**
   ```bash
   # Copy and customize if needed
   cp .env.example .env
   ```
   Default values work fine for local development.

2. **Start PostgreSQL database:**
   ```bash
   docker-compose up -d
   ```

3. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application:**
   - Frontend: http://localhost:8080
   - API: http://localhost:8080/api/items

4. **Run tests:**
   ```bash
   ./gradlew test
   ```
   Tests use Testcontainers to spin up a PostgreSQL container automatically.

5. **Build:**
   ```bash
   ./gradlew build
   ```

### Database

The application uses PostgreSQL with Flyway migrations. Schema is managed in `src/main/resources/db/migration/`.

**Local database connection:**
- Host: localhost:5432
- Database: lnf
- User: lnf
- Password: lnf_password

## Production Deployment

### Setup

#### 1. Create local.properties

Create a `local.properties` file in the project root:

```properties
# Local configuration - DO NOT commit this file
dockerUsername=your-dockerhub-username
dockerPassword=your-dockerhub-token
```

#### 2. Get Docker Hub Token

1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Give it a name (e.g., "fe-lnf-deploy")
4. Set permissions: Read, Write, Delete
5. Copy the token (you'll only see it once)
6. Add it to `local.properties` as `dockerPassword`

#### 3. Build and Push to Docker Hub

```bash
./gradlew publish
```

This will:
- Build the Spring Boot application
- Create a Docker image with Jib
- Push to Docker Hub as `your-username/fe-lnf:latest`

### Deploy to Server

#### 1. Prepare Environment

On your server, create a deployment directory and set up environment variables:

```bash
mkdir -p ~/fe-lnf-deploy
cd ~/fe-lnf-deploy

# Create .env file with secure credentials
cat > .env << EOF
POSTGRES_DB=lnf
POSTGRES_USER=lnf
POSTGRES_PASSWORD=YOUR_SECURE_PASSWORD_HERE
EOF

# Secure the .env file
chmod 600 .env
```

#### 2. Update docker-compose.yml

Edit `deploy/docker-compose.yml` and replace the image name with your Docker Hub username:

```yaml
services:
  app:
    image: your-username/fe-lnf:latest
```

#### 3. Copy to Server

Copy `deploy/docker-compose.yml` to your server's deployment directory (`~/fe-lnf-deploy`).

#### 4. Run on Server

On your server:

```bash
# Navigate to deployment directory
cd ~/fe-lnf-deploy

# Pull the latest image
docker-compose pull

# Start the application (PostgreSQL + App)
docker-compose up -d

# View logs
docker-compose logs -f

# View app logs only
docker-compose logs -f app

# Stop the application
docker-compose down
```

**The application will be available on port 80 (http://your-server-ip)**

**Important:** The `.env` file contains sensitive credentials. Make sure it's secured with `chmod 600 .env` and never committed to git.

### Setting Up Auto-Start on Boot

To ensure the application starts automatically on server boot and restarts on failure, set up systemd:

```bash
cd ~/fe-lnf-deploy

# Customize the service file
sed -i "s|YOUR_USERNAME|$USER|g" fe-lnf.service

# Install and enable
sudo cp fe-lnf.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable fe-lnf
sudo systemctl start fe-lnf

# Check status
sudo systemctl status fe-lnf
```

See **[deploy/SYSTEMD_SETUP.md](deploy/SYSTEMD_SETUP.md)** for complete setup and management guide.

### Managing the Deployed Application

See **[deploy/OPERATIONS.md](deploy/OPERATIONS.md)** for comprehensive guide on:
- Checking status and viewing logs
- Restarting services when they fail
- Updating to new versions
- Database backup and restore
- Troubleshooting common issues

**Quick reference:**

```bash
# Check status
docker-compose ps
docker-compose logs -f

# Restart services
docker-compose restart              # Restart all
docker-compose restart app          # Restart only app
docker-compose restart db           # Restart only database

# Update to latest version
docker-compose pull
docker-compose up -d

# Backup database
docker exec fe-lnf-db pg_dump -U lnf lnf > backup.sql

# View application
curl http://localhost:8080/api/items
```

### Security Notes

✅ **PostgreSQL is NOT exposed to the internet** - It's only accessible within the Docker network
✅ **Environment variables** - Database credentials passed via environment variables, not hardcoded
✅ **Production profile** - Uses `application-prod.properties` with production settings
✅ **Health checks** - Both PostgreSQL and app have health checks configured
✅ **Persistent data** - PostgreSQL data is stored in a Docker volume and survives restarts
✅ **Secure .env** - Environment file should be chmod 600 and never committed to git

### Database Backups

To backup the PostgreSQL database:

```bash
# Backup
docker exec fe-lnf-db pg_dump -U lnf lnf > backup.sql

# Restore
docker exec -i fe-lnf-db psql -U lnf lnf < backup.sql
```

## API Endpoints

### Items API (`/api/items`)

- `GET /api/items` - List all items
- `GET /api/items/{id}` - Get item by ID
- `GET /api/items/status/{status}` - Filter by status (LOST, FOUND, CLAIMED)
- `POST /api/items` - Create new item
- `PUT /api/items/{id}` - Update item
- `DELETE /api/items/{id}` - Delete item

### Example Request

```bash
# Create a new lost item
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Blue Backpack",
    "description": "Blue backpack with laptop inside",
    "location": "Building A, Room 101",
    "status": "LOST"
  }'

# List all items
curl http://localhost:8080/api/items
```

## Project Structure

```
src/main/kotlin/fe/lnf/
├── FeLnfApplication.kt          # Main application
├── controller/
│   └── ItemsRestController.kt   # REST API endpoints
├── model/
│   └── LostItem.kt              # Domain model
└── repository/
    └── LostItemRepository.kt    # Data access layer

src/main/resources/
├── application.properties        # Default config (dev)
├── application-prod.properties   # Production config
├── db/migration/
│   └── V1__initial_schema.sql   # Database schema
└── static/
    └── index.html               # Frontend UI
```
