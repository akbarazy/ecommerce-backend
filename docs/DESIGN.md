# API SPECIFICATIONS
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
GET     /api/products/{id}/variants

Product Management:
POST    /api/admin/products
PUT     /api/admin/products/{id}
DELETE  /api/admin/products/{id}

Variant Management:
GET     /api/admin/products/{productId}/variants
POST    /api/admin/products/{productId}/variants
PUT     /api/admin/variants/{id}
DELETE  /api/admin/variants/{id}

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

# DATABASE DIAGRAM
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

    PRODUCT_OPTION {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR name
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT_VARIANT {
        BIGINT id PK
        BIGINT product_id FK
        VARCHAR sku UK
        DECIMAL price
        INT stock
        DECIMAL weight
        VARCHAR image_url
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    VARIANT_OPTION {
        BIGINT id PK
        BIGINT variant_id FK
        BIGINT option_id FK
        VARCHAR value
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

    ORDER {
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
        VARCHAR product_name
        VARCHAR variant_attributes
        INT quantity
        DECIMAL price_at_purchase
        TIMESTAMP created_at
    }

    BLACKLISTED_TOKEN {
        BIGINT id PK
        VARCHAR token UK
        TIMESTAMP expiry_date
    }

    USER ||--o{ CART_ITEM : ""
    USER ||--o{ ORDER : ""
    CATEGORY ||--o{ PRODUCT : ""
    PRODUCT ||--|{ PRODUCT_OPTION : ""
    PRODUCT ||--|{ PRODUCT_VARIANT : ""
    PRODUCT_OPTION ||--o{ VARIANT_OPTION : ""
    PRODUCT_VARIANT ||--|{ VARIANT_OPTION : ""
    PRODUCT_VARIANT ||--o{ CART_ITEM : ""
    ORDER ||--|{ ORDER_ITEM : ""
    PRODUCT_VARIANT ||--o{ ORDER_ITEM : ""
```