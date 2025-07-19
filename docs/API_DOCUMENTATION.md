# Thati Air Alert - API Documentation

## 📋 Table of Contents

1. [Introduction](#introduction)
2. [Authentication](#authentication)
3. [Alerts API](#alerts-api)
4. [Users API](#users-api)
5. [Devices API](#devices-api)
6. [Admin API](#admin-api)
7. [Analytics API](#analytics-api)
8. [WebSocket API](#websocket-api)
9. [Error Handling](#error-handling)
10. [Rate Limiting](#rate-limiting)
11. [Versioning](#versioning)

---

## Introduction

The Thati Air Alert API provides a comprehensive interface for managing alerts, users, devices, and analytics. This document outlines all available endpoints, request/response formats, and authentication requirements.

### Base URL

```
https://api.thatialert.com
```

### Response Format

All responses are returned in JSON format with the following structure:

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful"
}
```

Or in case of an error:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

---

## Authentication

### Login

**POST /api/auth/login**

Authenticate a user and receive a JWT token.

**Request:**

```json
{
  "username": "admin_user",
  "password": "secure_password"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user_id",
      "username": "admin_user",
      "role": "admin",
      "name": "Admin User"
    }
  },
  "message": "Login successful"
}
```

### Send Alert

**POST /api/alerts/send**

Send a new alert to users.

**Headers:**

```
Authorization: Bearer <jwt_token>
```

**Request:**

```json
{
  "message": "Aircraft spotted heading north",
  "type": "aircraft",
  "priority": "high",
  "region": "yangon",
  "coordinates": {
    "latitude": 16.8661,
    "longitude": 96.1951
  }
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "alertId": "alert_id",
    "timestamp": "2023-07-19T10:35:00Z"
  },
  "message": "Alert sent successfully"
}
```

---

## Error Handling

### Error Codes

| Code | Description |
|------|-------------|
| `INVALID_REQUEST` | Request format is invalid |
| `AUTHENTICATION_FAILED` | Authentication credentials are invalid |
| `AUTHORIZATION_FAILED` | User lacks required permissions |
| `RESOURCE_NOT_FOUND` | Requested resource does not exist |
| `VALIDATION_ERROR` | Request data validation failed |
| `RATE_LIMIT_EXCEEDED` | Too many requests in time window |
| `SERVER_ERROR` | Internal server error |
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable |

---

*Last updated: July 19, 2025*