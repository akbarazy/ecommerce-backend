# Cara Menjalankan Aplikasi

## 1. Prasyarat
- **Java 17**
- **Maven**
- **PostgreSQL**

---

## 2. Setup Database
Pastikan service PostgreSQL sudah berjalan, lalu buat database:
```sql
CREATE DATABASE ecommerce_db;
```

Sesuaikan kredensial di [`src/main/resources/application.properties`](file:///c:/Users/Alfarizy/Documents/Programming%20Volume%203/Projects/ecommerce-backend/src/main/resources/application.properties) jika berbeda:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## 3. Jalankan Aplikasi
Jalankan perintah berikut di terminal:
```bash
mvn spring-boot:run
```

Aplikasi akan berjalan di:
```
http://localhost:8080
```
