# Media Backend

This is a Spring Boot backend project for a **Media Streaming Platform**. It supports admin user management, media metadata creation, secure media streaming with HMAC signatures, file uploads via Cloudinary, and caching with Redis.

---

## Table of Contents

* [Features](#features)
* [Tech Stack](#tech-stack)
* [Setup](#setup)
* [Environment Variables](#environment-variables)
* [Running the Project](#running-the-project)
* [Building JAR & Docker](#building-jar--docker)
* [API Endpoints](#api-endpoints)
* [Testing](#testing)
* [Security](#security)
* [Notes](#notes)
* [Author](#author)

---

## Features

* Admin user signup and login (JWT-based authentication)
* Upload media files via Cloudinary
* Create media metadata
* Generate secure stream URLs for media
* Stream media with partial content support (range requests)
* Log media views (IP + timestamp)
* Fetch media view logs and analytics
* Redis caching for analytics
* Rate limiting for media view logging

---

## Tech Stack

* **Backend:** Java 17, Spring Boot 3.5
* **Database:** PostgreSQL (NeonDB)
* **Caching:** Redis (Redis Cloud)
* **Security:** Spring Security, JWT, HMAC
* **File Storage:** Cloudinary
* **Build Tool:** Maven
* **Testing:** JUnit 5, Mockito, Spring Test
* **Dependencies:**

  * `spring-boot-starter-web`
  * `spring-boot-starter-security`
  * `spring-boot-starter-data-jpa`
  * `spring-boot-starter-data-redis`
  * `spring-boot-starter-cache`
  * `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
  * `cloudinary-http44`
  * `spring-dotenv`
  * `lombok`

---

## Setup

1. Clone the repository:

```bash
git clone <repo-url>
cd media-backend
```

2. Install dependencies using Maven:

```bash
mvn clean install
```

3. Create uploads folder (optional, Cloudinary used for storage):

```bash
mkdir uploads
```

## Environment Variables

Configure the application using `.env` or `application.properties`. Example `.env.example`:

```ini
# PostgreSQL
DB_URL=jdbc:postgresql://<host>:<port>/<db>?sslmode=require
DB_USERNAME=<username>
DB_PASSWORD=<password>

# JWT & HMAC
JWT_SECRET=<jwt-secret>
JWT_TTL_MS=3600000
HMAC_SECRET=<hmac-secret>
STREAM_TTL_MINUTES=10

# File Uploads
UPLOAD_DIR=uploads

# Redis
REDIS_HOST=<redis-host>
REDIS_PORT=<redis-port>
REDIS_PASSWORD=<redis-password>

# Cloudinary
CLOUDINARY_CLOUD_NAME=<cloud-name>
CLOUDINARY_API_KEY=<api-key>
CLOUDINARY_API_SECRET=<api-secret>
```

## Running the Project

Run locally using Maven:

```bash
mvn spring-boot:run
```

Default server port: `8080` (configurable in `application.properties`).

## Building JAR & Docker

Build JAR:

```bash
mvn clean package
```

Generated file: `target/media-backend-0.0.1-SNAPSHOT.jar`

Create Docker image:

```bash
docker build -t media-backend .
```

Run container:

```bash
docker run -d -p 8080:8080 --env-file .env media-backend
```

Optional: Use Docker Compose (if `docker-compose.yml` is present):

```bash
docker-compose up -d
```

## API Endpoints

### Auth

`POST /auth/signup` → Register new admin

```json
{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

`POST /auth/login` → Login as admin, returns JWT

```json
{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

Response:

```json
{
  "token": "<JWT_TOKEN>"
}
```

### Media

`POST /media/upload` → Upload media file (multipart/form-data)

`POST /media` → Create media metadata (authenticated)

```json
{
  "title": "Sample Video",
  "type": "video",
  "fileUrl": "/files/1756993493880-sample.mp4"
}
```

`GET /media/{id}/stream-url` → Generate secure 10-min stream URL

```json
{
  "streamUrl": "/media/2/stream?exp=1757002232021&sig=<HMAC_SIGNATURE>"
}
```

`GET /media/{id}/stream` → Stream media with range requests

`POST /media/{id}/view` → Log media view (IP + timestamp, rate limited)

`GET /media/{id}/view-log` → Fetch view logs

`GET /media/{id}/analytics` → Return analytics (total views, unique IPs, views per day)

```json
{
  "total_views": 174,
  "unique_ips": 122,
  "views_per_day": {
    "2025-08-01": 34,
    "2025-08-02": 56
  }
}
```

## Testing

Unit & integration tests are implemented using JUnit 5 and Mockito.

Security tests are covered with `spring-security-test`.

Run tests:

```bash
mvn test
```

## Security

* JWT authentication for admin users
* Secure media URLs using HMAC signature with expiry
* Public endpoints: `/auth/**`, `/media/upload`, `/media/*/stream`, `/files/**`
* All other endpoints require JWT in `Authorization` header: `Bearer <JWT_TOKEN>`

## Notes

* File uploads are stored in Cloudinary (not local filesystem)
* Media streaming supports partial content (seekable videos)
* Analytics endpoint is cached with Redis for performance
* Rate limiting implemented for `/media/:id/view` endpoint
* Ensure database and Redis are up and running before starting the app

## Author

**Simhadri Bharath**

* Email: [simhadribharath2004@gmail.com](mailto:simhadribharath2004@gmail.com)
* LinkedIn: [linkedin.com/in/simhadri-bharath](https://www.linkedin.com/in/simhadri-bharath)
* GitHub: [github.com/simhadri-bharath](https://github.com/simhadri-bharath)

