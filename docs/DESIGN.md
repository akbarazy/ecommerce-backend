DATABASE DESIGN
```mermaid
erDiagram
    USER {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password
        VARCHAR phone
        TEXT address
        VARCHAR role
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name UK
        TEXT description
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT {
        BIGINT id PK
        BIGINT category_id FK
        VARCHAR name
        TEXT description
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    VARIANT {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR sku UK
        VARCHAR color
        VARCHAR size
        DECIMAL price
        INT stock
        VARCHAR image_url
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    CART_ITEM {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT variant_id FK
        INT quantity
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ORDERS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR order_number UK
        DECIMAL total_price
        VARCHAR status
        TEXT shipping_address
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ORDER_ITEM {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT variant_id FK
        INT quantity
        DECIMAL price_at_purchase
        TIMESTAMP created_at
    }

    BLACKLISTED_TOKEN {
        BIGINT id PK
        VARCHAR token UK
        TIMESTAMP expiry_date
    }

    USER ||--o{ CART_ITEM : has
    USER ||--o{ ORDERS : places
    CATEGORY ||--o{ PRODUCT : contains
    PRODUCT ||--o{ VARIANT : has
    VARIANT ||--o{ CART_ITEM : added_as
    ORDERS ||--|{ ORDER_ITEM : contains
    VARIANT ||--o{ ORDER_ITEM : purchased_as
```

CLASS DESIGN
```mermaid
```

ARCHITECTURE DESIGN
```mermaid
```

API VISUALIZATION
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
GET     /api/products?keyword=value

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