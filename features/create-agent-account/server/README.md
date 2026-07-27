# Customer Service Account API

Start the service with `mvn spring-boot:run`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

Base path: `/api/admin/customer-service-users`

- `POST /` create account
- `GET /?page=1&pageSize=10&keyword=&role=&status=` list accounts
- `PATCH /{userId}` edit name/email
- `PATCH /{userId}/status` enable or disable
- `POST /{userId}/reset-password` reset password
- `GET /{userId}/operation-logs` view operation logs

Create request:
```json
{"name":"Ling Wang","phone":"13800138000","email":"ling@example.com","initialPassword":"Password123!"}
```

Password rules: 12–20 characters, must contain letters and digits, and only letters, digits, `@ # $ % _ !` are allowed. Email is optional. Phone is the login account.
