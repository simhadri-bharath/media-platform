# Media Backend

This is a Spring Boot backend project for a **Media Streaming Platform**. It supports admin user management, media metadata creation, secure media streaming with HMAC signatures, and file uploads.

---

## Table of Contents

* [Features](#features)
* [Tech Stack](#tech-stack)
* [Setup](#setup)
* [Environment Variables](#environment-variables)
* [Running the Project](#running-the-project)
* [API Endpoints](#api-endpoints)
* [Security](#security)
* [Notes](#notes)
* [Author](#author)

---

## Features

* Admin user signup and login (JWT-based authentication)
* Upload media files
* Create media metadata
* Generate secure stream URLs for media
* Stream media with partial content support (range requests)
* Log media views (IP + timestamp)
* Fetch media view logs

---

## Tech Stack

* **Backend:** Java 17, Spring Boot 3.5
* **Database:** PostgreSQL
* **Security:** Spring Security, JWT
* **File Storage:** Local filesystem (`uploads` folder)
* **Build Tool:** Maven
* **Dependencies:** `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `jjwt`, `spring-dotenv`, `lombok`

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

3. Create the uploads folder (optional, the app will create it automatically if missing):

```bash
mkdir uploads
```

---

## Environment Variables

You can configure the application using `.env` or `application.properties`. Example `.env`:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>

APP_JWT_SECRET=<jwt-secret>
APP_JWT_TTL_MS=3600000
APP_HMAC_SECRET=<hmac-secret>
APP_STREAM_TTL_MINUTES=10
APP_UPLOAD_DIR=uploads

SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=100MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=100MB
```

---

## Running the Project

```bash
mvn spring-boot:run
```

Default server port: `8080` (configurable in `application.properties`).

---

## API Endpoints

### Auth

* **POST** `/auth/signup`
  Register a new admin user

  ```json
  {
    "email": "admin1@example.com",
    "password": "Admin@123"
  }
  ```

* **POST** `/auth/login`
  Login as admin user

  ```json
  {
    "email": "admin1@example.com",
    "password": "Admin@123"
  }
  ```

  Response:

  ```json
  {
    "token": "<JWT_TOKEN>"
  }
  ```

---

### Media

* **POST** `/media/upload`
  Upload a media file (`multipart/form-data`)
  Response:

  ```json
  "/files/1756993493880-sample.mp4"
  ```

* **POST** `/media`
  Create media metadata

  ```json
  {
    "title": "Sample Video",
    "type": "video",
    "fileUrl": "/files/1756993493880-sample.mp4"
  }
  ```

* **GET** `/media/{id}/stream-url`
  Generate secure stream URL for media
  Response:

  ```json
  {
    "streamUrl": "/media/2/stream?exp=1757002232021&sig=RYLxNFb9_O0GuJtOaNpY2ndZv9jFlfo5xdY6Zd4Vb3Y"
  }
  ```

* **GET** `/media/{id}/stream`
  Stream media using the secure URL (supports Range requests)

* **GET** `/media/{id}/view-log`
  Fetch view logs for a media file


* **GET** `/media/{id}/analytics`
 Fetch analytics for a media file (Requires Authorization header: Bearer JWT_TOKEN)
  Response:

  ```json
  {
  "total_views": 3,
  "unique_ips": 1,
  "views_per_day": {
    "2025-09-05": 3
  }

  ```



---

## Security

* JWT authentication for admin users
* Secure media URLs using HMAC signature with expiry (`exp` parameter)
* Public endpoints: `/auth/**`, `/media/upload`, `/media/*/stream`, `/files/**`
* All other endpoints require JWT token in `Authorization` header:
  `Bearer <JWT_TOKEN>`

---

## Example URL for Streaming

```
http://localhost:8080/media/2/stream?exp=1757002232021&sig=RYLxNFb9_O0GuJtOaNpY2ndZv9jFlfo5xdY6Zd4Vb3Y
```

* `exp` → Expiration timestamp (milliseconds)
* `sig` → HMAC signature for verification

---

## Notes

* File uploads are stored locally in `uploads/` folder.
* Media streaming supports partial content (so video players can seek).
* JWT token expiration and HMAC signature expiration must be respected.
* Ensure database is up and running before starting the application.

---

## Author

**Simhadri Bharath**
Email: [simhadribharath2004@gmail.com](mailto:simhadribharath2004@gmail.com)
LinkedIn: [linkedin.com/in/simhadri-bharath](https://linkedin.com/in/simhadri-bharath)
GitHub: [github.com/simhadri-bharath](https://github.com/simhadri-bharath)
