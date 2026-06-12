# Delivery Service API

Spring Boot REST API for a simple delivery service workflow.

List endpoints return JSON objects in the form `{ "items": [...], "count": N }`.
Too-large numeric values in path params, query params, or JSON bodies return `400 Bad Request` instead of an internal error.

## Requirements
- Java 17
- PostgreSQL

## Configuration
Update `src/main/resources/application.properties` if needed:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

## Run
```bash
./mvnw spring-boot:run
```

## Default Shop
On startup, the system creates a single shop ("Default Shop") and seeds a few shop items if none exist.
`POST /api/testing/database/clear` still fully empties the current database state without repopulating it.

## Core Flow (Quick Start)
1. Create users (customer, courier). The default shop is created automatically.
2. Customer fetches shop items and adds items to cart by item id.
3. Customer creates an order.
4. Shop accepts/declines within 10 minutes.
5. If accepted, system assigns an available courier (or cancels after 10 minutes).

## Endpoints
### Users
- `POST /api/users`
- `GET /api/users?role=CUSTOMER|SHOP|COURIER`
- `PATCH /api/couriers/{courierId}/availability`

### Shop items
- `GET /api/items`

### Cart
- `GET /api/customers/{customerId}/cart`
- `POST /api/customers/{customerId}/cart/items`
- `DELETE /api/customers/{customerId}/cart/items/{itemId}`

### Orders
- `POST /api/customers/{customerId}/orders`
- `GET /api/orders/{orderId}`
- `GET /api/customers/{customerId}/orders`
- `GET /api/shop/orders?status=PENDING_SHOP`
- `GET /api/couriers/{courierId}/orders`

### Shop actions
- `POST /api/shop/orders/{orderId}/accept`
- `POST /api/shop/orders/{orderId}/decline`

### Notifications
- `GET /api/notifications?userId={id}&unreadOnly=true`
- `POST /api/notifications/{notificationId}/read`
- `GET /api/shop/notifications?unreadOnly=true`

### Testing utilities
- `POST /api/testing/database/clear`

## Sample Requests
### Create users
```json
POST /api/users
{
  "name": "Alice",
  "role": "CUSTOMER"
}
```
```json
POST /api/users
{
  "name": "Courier 1",
  "role": "COURIER",
  "available": true
}
```

### List shop items
```json
GET /api/items
```

### Add item to cart
```json
POST /api/customers/1/cart/items
{
  "shopItemId": 1,
  "quantity": 2
}
```

### Create order
```json
POST /api/customers/1/orders
{
  "deliveryAddress": "221B Baker Street",
  "deliveryTime": "2026-04-03T18:30:00+00:00"
}
```

### Shop accepts
```json
POST /api/shop/orders/1/accept
```

### Notifications
```json
GET /api/notifications?userId=2&unreadOnly=true
```

### Testing clear
```json
POST /api/testing/database/clear
```
