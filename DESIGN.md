DATABASE DESIGN
```mermaid
erDiagram
    USER {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR password
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ROLE {
        BIGINT id PK
        VARCHAR name UK
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR name
        TEXT description
        DECIMAL price
        INT stock
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CART {
        BIGINT id PK
        BIGINT user_id FK
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CART_ITEM {
        BIGINT id PK
        BIGINT cart_id FK
        BIGINT product_id FK
        INT quantity
        DECIMAL unit_price
        DECIMAL subtotal
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        DECIMAL total_price
        VARCHAR status
        TIMESTAMP ordered_at
        TIMESTAMP updated_at
    }

    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT product_id FK
        INT quantity
        DECIMAL unit_price
        DECIMAL subtotal
    }

    ROLE ||--o{ USER : has
    USER ||--|| CART : owns
    USER ||--o{ ORDERS : places
    CATEGORY ||--o{ PRODUCT : contains
    CART ||--o{ CART_ITEM : contains
    PRODUCT ||--o{ CART_ITEM : added_as
    ORDERS ||--o{ ORDER_ITEM : contains
    PRODUCT ||--o{ ORDER_ITEM : purchased_as
```

CLASS DESIGN
```mermaid
classDiagram
    direction LR
    class User {
        -Long id
        -String fullName
        -String email
        -String password
        -LocalDateTime createdAt
        -LocalDateTime updatedAt

        +updateProfile()
        +updatePassword()
    }

    class Role {
        -Long id
        -String name
    }

    class Category {
        -Long id
        -String name
        -LocalDateTime createdAt
        -LocalDateTime updatedAt

        +rename()
    }

    class Product {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -LocalDateTime createdAt
        -LocalDateTime updatedAt

        +updateInfo()
        +increaseStock()
        +decreaseStock()
    }

    class Cart {
        -Long id
        -LocalDateTime createdAt
        -LocalDateTime updatedAt

        +addItem()
        +removeItem()
        +updateQuantity()
        +calculateTotal()
        +clear()
    }

    class CartItem {
        -Long id
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal subtotal

        +calculateSubtotal()
    }

    class Order {
        -Long id
        -BigDecimal totalPrice
        -OrderStatus status
        -LocalDateTime orderedAt
        -LocalDateTime updatedAt

        +checkout()
        +updateStatus()
        +calculateTotal()
    }

    class OrderItem {
        -Long id
        -Integer quantity
        -BigDecimal unitPrice
        -BigDecimal subtotal

        +calculateSubtotal()
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        PAID
        PROCESSING
        SHIPPED
        COMPLETED
        CANCELLED
    }

    Role "1" <-- "0..*" User : role
    User "1" --> "1" Cart : owns
    User "1" --> "0..*" Order : places
    Category "1" --> "0..*" Product : contains
    Cart "1" *-- "0..*" CartItem
    Product "1" --> "0..*" CartItem
    Order "1" *-- "1..*" OrderItem
    Product "1" --> "0..*" OrderItem
    Order --> OrderStatus
```

ARCHITECTURE DESIGN
```mermaid
flowchart TB
    Client["Client
    (Web / Mobile App)"]

    Controller["Controller Layer
    REST API"]

    Security["Spring Security
    JWT Filter
    Authentication
    Authorization"]

    Validation["Validation
    Jakarta Validation"]

    Service["Service Layer
    Business Logic"]

    Repository["Repository Layer
    Spring Data JPA"]

    Database[("PostgreSQL")]

    Exception["Global Exception Handler"]

    Client --> Controller
    Controller --> Security
    Security --> Validation
    Validation --> Service
    Service --> Repository
    Repository --> Database
    Controller -.-> Exception
    Service -.-> Exception
    Repository -.-> Exception
```

ENDPOINT ACCESS CONFIG
```
Authentication:
POST    /api/auth/register
POST    /api/auth/login
POST    /api/auth/logout

User Profile:
GET     /api/users/me
PUT     /api/users/me
PUT     /api/users/me/password

Product Catalog:
GET     /api/products
GET     /api/products/{id}
GET     /api/products/search

Product Management:
POST    /api/admin/products
PUT     /api/admin/products/{id}
DELETE  /api/admin/products/{id}

Category Management:
GET     /api/categories
POST    /api/admin/categories
PUT     /api/admin/categories/{id}
DELETE  /api/admin/categories/{id}

Shopping Cart:
GET     /api/cart
POST    /api/cart/items
PUT     /api/cart/items/{id}
DELETE  /api/cart/items/{id}

Checkout:
POST    /api/orders/checkout

Orders:
GET     /api/orders
GET     /api/orders/{id}

Order Management:
GET     /api/admin/orders
GET     /api/admin/orders/{id}
PUT     /api/admin/orders/{id}/status
```