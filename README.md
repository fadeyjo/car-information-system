# 🚗 Vehicle Tracking System

Final qualifying work for collecting, monitoring, and analyzing vehicle movement data.  
The data is obtained from the vehicle's **OBD-II port**.

---

## 📌 Features

- 📡 Collects vehicle telemetry data
- 📊 Stores and analyzes movement data
- 🔐 JWT-based authentication
- 🖼️ File storage for avatars and car photos
- 🌐 REST API for interaction

---

## 🛠️ Tech Stack

- ASP.NET Core
- MySQL
- JWT Authentication
- REST API
- Pytest

---

## server

This part of project is REST API server to work with database and server file_system. To start this part you need to add appsettings.json in the project root. Example appsettings.json:

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "AllowedHosts": "*",
  "ConnectionStrings": {
    "DefaultConnection": "server=address;database=database;user=user;password=pas;"
  },
  "Store": {
    "AvatarsPath": "path/to/avatars",
    "CarPhotosPath": "path/to/car_photos"
  },
  "Jwt": {
    "Key": "SECRET_KEY",
    "Issuer": "MyApp",
    "Audience": "MyAppClient",
    "AccessTokenMinutes": 15,
    "RefreshTokenDays": 7
  }
}
```
