# 🏗️ E-Commerce Backend — Design Document

> [!NOTE]
> Dokumen perancangan lengkap berdasarkan [VISION.txt](file:///c:/Users/Alfarizy/Documents/Programming%20Volume%203/Projects/ecommerce-backend/VISION.txt).
> Mencakup **Database Design**, **Class Design**, dan **Architecture Design**.

---

# 📊 Part 1: Database Design

## 1.1 Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    users {
        bigint id PK "AUTO_INCREMENT"
        varchar_100 name "NOT NULL"
        varchar_150 email "NOT NULL, UNIQUE"
        varchar_255 password "NOT NULL"
        varchar_20 phone "NULLABLE"
        text address "NULLABLE"
        varchar_10 role "NOT NULL, DEFAULT 'USER'"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
        timestamp updated_at "NOT NULL, DEFAULT NOW()"
    }

    categories {
        bigint id PK "AUTO_INCREMENT"
        varchar_100 name "NOT NULL, UNIQUE"
        text description "NULLABLE"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
        timestamp updated_at "NOT NULL, DEFAULT NOW()"
    }

    products {
        bigint id PK "AUTO_INCREMENT"
        varchar_200 name "NOT NULL"
        text description "NULLABLE"
        decimal_12_2 price "NOT NULL, CHECK >= 0"
        int stock "NOT NULL, DEFAULT 0, CHECK >= 0"
        varchar_500 image_url "NULLABLE"
        bigint category_id FK "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
        timestamp updated_at "NOT NULL, DEFAULT NOW()"
    }

    cart_items {
        bigint id PK "AUTO_INCREMENT"
        bigint user_id FK "NOT NULL"
        bigint product_id FK "NOT NULL"
        int quantity "NOT NULL, CHECK >= 1"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
        timestamp updated_at "NOT NULL, DEFAULT NOW()"
    }

    orders {
        bigint id PK "AUTO_INCREMENT"
        varchar_50 order_number "NOT NULL, UNIQUE"
        bigint user_id FK "NOT NULL"
        decimal_15_2 total_price "NOT NULL"
        varchar_20 status "NOT NULL, DEFAULT 'PENDING'"
        text shipping_address "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
        timestamp updated_at "NOT NULL, DEFAULT NOW()"
    }

    order_items {
        bigint id PK "AUTO_INCREMENT"
        bigint order_id FK "NOT NULL"
        bigint product_id FK "NOT NULL"
        int quantity "NOT NULL, CHECK >= 1"
        decimal_12_2 price_at_purchase "NOT NULL"
        timestamp created_at "NOT NULL, DEFAULT NOW()"
    }

    users ||--o{ cart_items : "has"
    users ||--o{ orders : "places"
    categories ||--o{ products : "contains"
    products ||--o{ cart_items : "added to"
    products ||--o{ order_items : "purchased as"
    orders ||--|{ order_items : "consists of"
```

## 1.2 Tabel & Kolom Detail

### `users` — Data akun pengguna

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `name` | `VARCHAR(100)` | NOT NULL | Nama lengkap |
| `email` | `VARCHAR(150)` | NOT NULL, UNIQUE | Email untuk login |
| `password` | `VARCHAR(255)` | NOT NULL | BCrypt hashed password |
| `phone` | `VARCHAR(20)` | NULLABLE | Nomor telepon |
| `address` | `TEXT` | NULLABLE | Alamat default |
| `role` | `VARCHAR(10)` | NOT NULL, DEFAULT `'USER'` | `USER` atau `ADMIN` |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu registrasi |
| `updated_at` | `TIMESTAMP` | NOT NULL | Waktu update terakhir |

---

### `categories` — Kategori produk

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `name` | `VARCHAR(100)` | NOT NULL, UNIQUE | Nama kategori |
| `description` | `TEXT` | NULLABLE | Deskripsi kategori |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu dibuat |
| `updated_at` | `TIMESTAMP` | NOT NULL | Waktu update |

---

### `products` — Data produk

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `name` | `VARCHAR(200)` | NOT NULL | Nama produk |
| `description` | `TEXT` | NULLABLE | Deskripsi produk |
| `price` | `DECIMAL(12,2)` | NOT NULL, CHECK ≥ 0 | Harga produk |
| `stock` | `INT` | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Stok tersedia |
| `image_url` | `VARCHAR(500)` | NULLABLE | URL gambar produk |
| `category_id` | `BIGINT` | FK → `categories.id`, NOT NULL | Relasi ke kategori |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu dibuat |
| `updated_at` | `TIMESTAMP` | NOT NULL | Waktu update |

---

### `cart_items` — Keranjang belanja user

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL | Pemilik keranjang |
| `product_id` | `BIGINT` | FK → `products.id`, NOT NULL | Produk di keranjang |
| `quantity` | `INT` | NOT NULL, CHECK ≥ 1 | Jumlah item |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu ditambahkan |
| `updated_at` | `TIMESTAMP` | NOT NULL | Waktu update |

> [!IMPORTANT]
> Kombinasi `(user_id, product_id)` sebaiknya dibuat **UNIQUE** agar satu user tidak punya duplikat produk di keranjang. Jika user menambah produk yang sama, maka `quantity` yang di-update.

---

### `orders` — Data order/pesanan

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `order_number` | `VARCHAR(50)` | NOT NULL, UNIQUE | Nomor order (e.g. `ORD-20260716-0001`) |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL | User yang memesan |
| `total_price` | `DECIMAL(15,2)` | NOT NULL | Total harga seluruh item |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'PENDING'` | Status order |
| `shipping_address` | `TEXT` | NOT NULL | Alamat pengiriman |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu order dibuat |
| `updated_at` | `TIMESTAMP` | NOT NULL | Waktu update status |

---

### `order_items` — Item-item dalam satu order

| Kolom | Tipe | Constraint | Keterangan |
|-------|------|------------|------------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | Primary key |
| `order_id` | `BIGINT` | FK → `orders.id`, NOT NULL | Relasi ke order |
| `product_id` | `BIGINT` | FK → `products.id`, NOT NULL | Produk yang dipesan |
| `quantity` | `INT` | NOT NULL, CHECK ≥ 1 | Jumlah item |
| `price_at_purchase` | `DECIMAL(12,2)` | NOT NULL | Harga saat pembelian |
| `created_at` | `TIMESTAMP` | NOT NULL | Waktu dibuat |

> [!IMPORTANT]
> `price_at_purchase` menyimpan **snapshot harga** saat pembelian. Ini penting agar perubahan harga produk di masa depan tidak mengubah histori order yang sudah ada.

## 1.3 Enum Values

```mermaid
graph LR
    subgraph Role["🔑 Role (Enum)"]
        R1["USER"]
        R2["ADMIN"]
    end

    subgraph OrderStatus["📋 OrderStatus (Enum)"]
        S1["PENDING"] --> S2["PROCESSING"]
        S2 --> S3["SHIPPED"]
        S3 --> S4["DELIVERED"]
        S1 --> S5["CANCELLED"]
        S2 --> S5
    end
```

| Enum | Values | Keterangan |
|------|--------|------------|
| `Role` | `USER`, `ADMIN` | Hak akses pengguna |
| `OrderStatus` | `PENDING` → `PROCESSING` → `SHIPPED` → `DELIVERED` / `CANCELLED` | Alur status order |

## 1.4 Index Strategy

| Tabel | Index | Tipe | Alasan |
|-------|-------|------|--------|
| `users` | `email` | UNIQUE | Login lookup, mencegah duplikat |
| `categories` | `name` | UNIQUE | Mencegah duplikat nama kategori |
| `products` | `category_id` | INDEX | Filter produk berdasarkan kategori |
| `products` | `name` | INDEX | Full-text search produk |
| `products` | `price` | INDEX | Filter & sorting berdasarkan harga |
| `cart_items` | `(user_id, product_id)` | UNIQUE | Satu user, satu produk per entry |
| `cart_items` | `user_id` | INDEX | Fetch semua cart milik satu user |
| `orders` | `user_id` | INDEX | Fetch semua order milik satu user |
| `orders` | `order_number` | UNIQUE | Lookup by order number |
| `order_items` | `order_id` | INDEX | Fetch semua item dalam satu order |

## 1.5 Relasi Antar Tabel

```mermaid
graph TD
    U["👤 users"] -->|"1 : N"| CI["🛒 cart_items"]
    U -->|"1 : N"| O["📋 orders"]
    C["📂 categories"] -->|"1 : N"| P["📦 products"]
    P -->|"1 : N"| CI
    P -->|"1 : N"| OI["📄 order_items"]
    O -->|"1 : N"| OI

    style U fill:#4a90d9,color:#fff
    style C fill:#7b68ee,color:#fff
    style P fill:#e67e22,color:#fff
    style CI fill:#27ae60,color:#fff
    style O fill:#e74c3c,color:#fff
    style OI fill:#f39c12,color:#fff
```

| Dari | Ke | Tipe | Foreign Key |
|------|----|------|-------------|
| `users` | `cart_items` | One-to-Many | `cart_items.user_id` → `users.id` |
| `users` | `orders` | One-to-Many | `orders.user_id` → `users.id` |
| `categories` | `products` | One-to-Many | `products.category_id` → `categories.id` |
| `products` | `cart_items` | One-to-Many | `cart_items.product_id` → `products.id` |
| `products` | `order_items` | One-to-Many | `order_items.product_id` → `products.id` |
| `orders` | `order_items` | One-to-Many | `order_items.order_id` → `orders.id` |

---
---

# 🧩 Part 2: Class Design

## 2.1 Domain Entity Classes

```mermaid
classDiagram
    class User {
        -Long id
        -String name
        -String email
        -String password
        -String phone
        -String address
        -Role role
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -List~CartItem~ cartItems
        -List~Order~ orders
    }

    class Role {
        <<enumeration>>
        USER
        ADMIN
    }

    class Category {
        -Long id
        -String name
        -String description
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -List~Product~ products
    }

    class Product {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -String imageUrl
        -Category category
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class CartItem {
        -Long id
        -User user
        -Product product
        -Integer quantity
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +getSubtotal() BigDecimal
    }

    class Order {
        -Long id
        -String orderNumber
        -User user
        -BigDecimal totalPrice
        -OrderStatus status
        -String shippingAddress
        -List~OrderItem~ orderItems
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        PROCESSING
        SHIPPED
        DELIVERED
        CANCELLED
    }

    class OrderItem {
        -Long id
        -Order order
        -Product product
        -Integer quantity
        -BigDecimal priceAtPurchase
        -LocalDateTime createdAt
        +getSubtotal() BigDecimal
    }

    User --> Role
    User "1" --o "*" CartItem
    User "1" --o "*" Order
    Category "1" --o "*" Product
    Product "1" --* "*" CartItem
    Product "1" --* "*" OrderItem
    Order --> OrderStatus
    Order "1" --o "*" OrderItem
```

## 2.2 DTO Classes (Request & Response)

### Request DTOs

```mermaid
classDiagram
    class RegisterRequest {
        -String name
        -String email
        -String password
    }

    class LoginRequest {
        -String email
        -String password
    }

    class UpdateProfileRequest {
        -String name
        -String phone
        -String address
    }

    class ChangePasswordRequest {
        -String currentPassword
        -String newPassword
    }

    class ProductRequest {
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -String imageUrl
        -Long categoryId
    }

    class CategoryRequest {
        -String name
        -String description
    }

    class CartItemRequest {
        -Long productId
        -Integer quantity
    }

    class UpdateCartItemRequest {
        -Integer quantity
    }

    class UpdateOrderStatusRequest {
        -String status
    }

    class CheckoutRequest {
        -String shippingAddress
    }
```

### Response DTOs

```mermaid
classDiagram
    class ApiResponse~T~ {
        -boolean success
        -String message
        -T data
        -LocalDateTime timestamp
        +of(message, data)$ ApiResponse
        +error(message)$ ApiResponse
    }

    class AuthResponse {
        -String token
        -String tokenType
        -UserResponse user
    }

    class UserResponse {
        -Long id
        -String name
        -String email
        -String phone
        -String address
        -String role
        -LocalDateTime createdAt
    }

    class ProductResponse {
        -Long id
        -String name
        -String description
        -BigDecimal price
        -Integer stock
        -String imageUrl
        -String categoryName
        -Long categoryId
        -LocalDateTime createdAt
    }

    class CategoryResponse {
        -Long id
        -String name
        -String description
        -int productCount
        -LocalDateTime createdAt
    }

    class CartItemResponse {
        -Long id
        -Long productId
        -String productName
        -BigDecimal productPrice
        -String productImageUrl
        -Integer quantity
        -BigDecimal subtotal
    }

    class CartResponse {
        -List~CartItemResponse~ items
        -int totalItems
        -BigDecimal totalPrice
    }

    class OrderResponse {
        -Long id
        -String orderNumber
        -BigDecimal totalPrice
        -String status
        -String shippingAddress
        -List~OrderItemResponse~ items
        -LocalDateTime createdAt
    }

    class OrderItemResponse {
        -Long id
        -String productName
        -Integer quantity
        -BigDecimal priceAtPurchase
        -BigDecimal subtotal
    }
```

> [!TIP]
> **Kenapa pakai DTO?**
> - Mencegah eksposur field sensitif (e.g. `password` pada `User`)
> - Memberi kontrol penuh terhadap bentuk JSON request & response
> - Memisahkan representasi database dari representasi API

### DTO Mapping Flow

```mermaid
graph LR
    subgraph "📥 Incoming Request"
        JSON_IN["JSON Body"]
    end

    subgraph "🎯 Controller"
        CTRL["@RequestBody\n+ @Valid"]
    end

    subgraph "⚙️ Service"
        SVC["Business Logic"]
    end

    subgraph "💾 Entity"
        ENT["JPA Entity"]
    end

    subgraph "📤 Outgoing Response"
        JSON_OUT["JSON Response"]
    end

    JSON_IN -->|deserialize| CTRL
    CTRL -->|"Request DTO"| SVC
    SVC -->|"map DTO → Entity"| ENT
    ENT -->|"save / query"| SVC
    SVC -->|"map Entity → Response DTO"| CTRL
    CTRL -->|serialize| JSON_OUT
```

## 2.3 Service Classes

```mermaid
classDiagram
    class AuthService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) AuthResponse
    }

    class UserService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +getProfile(Long userId) UserResponse
        +updateProfile(Long userId, UpdateProfileRequest) UserResponse
        +changePassword(Long userId, ChangePasswordRequest) void
    }

    class ProductService {
        -ProductRepository productRepository
        -CategoryRepository categoryRepository
        +createProduct(ProductRequest) ProductResponse
        +getProductById(Long id) ProductResponse
        +getAllProducts(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable) Page~ProductResponse~
        +updateProduct(Long id, ProductRequest) ProductResponse
        +deleteProduct(Long id) void
    }

    class CategoryService {
        -CategoryRepository categoryRepository
        +createCategory(CategoryRequest) CategoryResponse
        +getAllCategories() List~CategoryResponse~
        +getCategoryById(Long id) CategoryResponse
        +updateCategory(Long id, CategoryRequest) CategoryResponse
        +deleteCategory(Long id) void
    }

    class CartService {
        -CartItemRepository cartItemRepository
        -ProductRepository productRepository
        +addToCart(Long userId, CartItemRequest) CartItemResponse
        +getCart(Long userId) CartResponse
        +updateCartItem(Long userId, Long itemId, UpdateCartItemRequest) CartItemResponse
        +removeFromCart(Long userId, Long itemId) void
    }

    class OrderService {
        -OrderRepository orderRepository
        -OrderItemRepository orderItemRepository
        -CartItemRepository cartItemRepository
        -ProductRepository productRepository
        +checkout(Long userId, CheckoutRequest) OrderResponse
        +getMyOrders(Long userId, Pageable) Page~OrderResponse~
        +getMyOrderById(Long userId, Long orderId) OrderResponse
        +getAllOrders(Pageable) Page~OrderResponse~
        +updateOrderStatus(Long orderId, UpdateOrderStatusRequest) OrderResponse
    }
```

## 2.4 Repository Interfaces

```mermaid
classDiagram
    class JpaRepository~T_ID~ {
        <<interface>>
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String email) Optional~User~
        +existsByEmail(String email) boolean
    }

    class ProductRepository {
        <<interface>>
        +findAll(Specification, Pageable) Page~Product~
    }

    class CategoryRepository {
        <<interface>>
        +existsByName(String name) boolean
    }

    class CartItemRepository {
        <<interface>>
        +findByUserId(Long userId) List~CartItem~
        +findByUserIdAndProductId(Long userId, Long productId) Optional~CartItem~
        +deleteByUserId(Long userId) void
    }

    class OrderRepository {
        <<interface>>
        +findByUserIdOrderByCreatedAtDesc(Long userId, Pageable) Page~Order~
        +findByIdAndUserId(Long id, Long userId) Optional~Order~
    }

    class OrderItemRepository {
        <<interface>>
    }

    JpaRepository <|-- UserRepository
    JpaRepository <|-- ProductRepository
    JpaRepository <|-- CategoryRepository
    JpaRepository <|-- CartItemRepository
    JpaRepository <|-- OrderRepository
    JpaRepository <|-- OrderItemRepository
```

## 2.5 Controller Classes

```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +register(RegisterRequest) ResponseEntity
        +login(LoginRequest) ResponseEntity
    }

    class UserController {
        -UserService userService
        +getProfile() ResponseEntity
        +updateProfile(UpdateProfileRequest) ResponseEntity
        +changePassword(ChangePasswordRequest) ResponseEntity
    }

    class ProductController {
        -ProductService productService
        +getAllProducts(search, categoryId, minPrice, maxPrice, pageable) ResponseEntity
        +getProductById(Long id) ResponseEntity
    }

    class AdminProductController {
        -ProductService productService
        +createProduct(ProductRequest) ResponseEntity
        +updateProduct(Long id, ProductRequest) ResponseEntity
        +deleteProduct(Long id) ResponseEntity
    }

    class CategoryController {
        -CategoryService categoryService
        +getAllCategories() ResponseEntity
    }

    class AdminCategoryController {
        -CategoryService categoryService
        +createCategory(CategoryRequest) ResponseEntity
        +updateCategory(Long id, CategoryRequest) ResponseEntity
        +deleteCategory(Long id) ResponseEntity
    }

    class CartController {
        -CartService cartService
        +getCart() ResponseEntity
        +addToCart(CartItemRequest) ResponseEntity
        +updateCartItem(Long id, UpdateCartItemRequest) ResponseEntity
        +removeFromCart(Long id) ResponseEntity
    }

    class OrderController {
        -OrderService orderService
        +checkout(CheckoutRequest) ResponseEntity
        +getMyOrders(Pageable) ResponseEntity
        +getMyOrderById(Long id) ResponseEntity
    }

    class AdminOrderController {
        -OrderService orderService
        +getAllOrders(Pageable) ResponseEntity
        +updateOrderStatus(Long id, UpdateOrderStatusRequest) ResponseEntity
    }
```

## 2.6 Security Classes

```mermaid
classDiagram
    class SecurityConfig {
        -JwtAuthenticationFilter jwtAuthFilter
        +securityFilterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager() AuthenticationManager
    }

    class JwtTokenProvider {
        -String jwtSecret
        -long jwtExpiration
        +generateToken(User) String
        +getUserIdFromToken(String token) Long
        +validateToken(String token) boolean
    }

    class JwtAuthenticationFilter {
        -JwtTokenProvider jwtTokenProvider
        -CustomUserDetailsService userDetailsService
        #doFilterInternal(request, response, filterChain) void
    }

    class CustomUserDetailsService {
        -UserRepository userRepository
        +loadUserByUsername(String email) UserDetails
    }

    SecurityConfig --> JwtAuthenticationFilter
    JwtAuthenticationFilter --> JwtTokenProvider
    JwtAuthenticationFilter --> CustomUserDetailsService
    CustomUserDetailsService --> UserRepository

    class UserRepository {
        <<interface>>
    }
```

## 2.7 Exception Classes

```mermaid
classDiagram
    class RuntimeException {
        <<Java Standard>>
    }

    class ResourceNotFoundException {
        -String resourceName
        -String fieldName
        -Object fieldValue
        +ResourceNotFoundException(resourceName, fieldName, fieldValue)
    }

    class BadRequestException {
        -String message
        +BadRequestException(message)
    }

    class GlobalExceptionHandler {
        <<@ControllerAdvice>>
        +handleResourceNotFoundException() ResponseEntity
        +handleBadRequestException() ResponseEntity
        +handleMethodArgumentNotValid() ResponseEntity
        +handleAccessDeniedException() ResponseEntity
        +handleAuthenticationException() ResponseEntity
        +handleGenericException() ResponseEntity
    }

    RuntimeException <|-- ResourceNotFoundException
    RuntimeException <|-- BadRequestException
```

## 2.8 Validation Annotations (per Request DTO)

| DTO | Field | Annotation | Pesan |
|-----|-------|------------|-------|
| `RegisterRequest` | `name` | `@NotBlank` | Nama tidak boleh kosong |
| | `email` | `@NotBlank`, `@Email` | Email harus valid |
| | `password` | `@NotBlank`, `@Size(min=8)` | Password minimal 8 karakter |
| `LoginRequest` | `email` | `@NotBlank`, `@Email` | Email harus valid |
| | `password` | `@NotBlank` | Password tidak boleh kosong |
| `ProductRequest` | `name` | `@NotBlank` | Nama produk tidak boleh kosong |
| | `price` | `@NotNull`, `@DecimalMin("0")` | Harga tidak boleh negatif |
| | `stock` | `@NotNull`, `@Min(0)` | Stok tidak boleh negatif |
| | `categoryId` | `@NotNull` | Kategori wajib diisi |
| `CategoryRequest` | `name` | `@NotBlank` | Nama kategori tidak boleh kosong |
| `CartItemRequest` | `productId` | `@NotNull` | Product ID wajib |
| | `quantity` | `@NotNull`, `@Min(1)` | Quantity minimal 1 |
| `ChangePasswordRequest` | `currentPassword` | `@NotBlank` | Password lama wajib diisi |
| | `newPassword` | `@NotBlank`, `@Size(min=8)` | Password baru minimal 8 karakter |

---
---

# 🏛️ Part 3: Architecture Design

## 3.1 Layered Architecture

```mermaid
graph TB
    subgraph CLIENT["🌐 Client"]
        CL["Postman / Frontend / cURL"]
    end

    subgraph FILTER["🔐 Security Filter Chain"]
        SF["JwtAuthenticationFilter"]
        SC["SecurityConfig"]
    end

    subgraph CONTROLLER["🎯 Controller Layer"]
        direction LR
        AC["AuthController"]
        UC["UserController"]
        PC["ProductController"]
        APC["AdminProductController"]
        CCC["CategoryController"]
        ACC["AdminCategoryController"]
        CC["CartController"]
        OC["OrderController"]
        AOC["AdminOrderController"]
    end

    subgraph SERVICE["⚙️ Service Layer"]
        direction LR
        AS["AuthService"]
        US["UserService"]
        PS["ProductService"]
        CS["CategoryService"]
        CarS["CartService"]
        OS["OrderService"]
    end

    subgraph REPOSITORY["💾 Repository Layer"]
        direction LR
        UR["UserRepository"]
        PR["ProductRepository"]
        CR["CategoryRepository"]
        CIR["CartItemRepository"]
        OR["OrderRepository"]
        OIR["OrderItemRepository"]
    end

    subgraph DB["🗄️ Database"]
        PG["PostgreSQL"]
    end

    CL --> SF
    SF --> SC
    SC --> CONTROLLER

    AC --> AS
    UC --> US
    PC --> PS
    APC --> PS
    CCC --> CS
    ACC --> CS
    CC --> CarS
    OC --> OS
    AOC --> OS

    AS --> UR
    US --> UR
    PS --> PR
    PS --> CR
    CS --> CR
    CarS --> CIR
    CarS --> PR
    OS --> OR
    OS --> OIR
    OS --> CIR
    OS --> PR

    UR --> PG
    PR --> PG
    CR --> PG
    CIR --> PG
    OR --> PG
    OIR --> PG
```

### Tanggung Jawab Tiap Layer

| Layer | Tanggung Jawab | Anotasi Spring |
|-------|---------------|----------------|
| **Controller** | Menerima HTTP request, validasi input, mapping DTO, return response | `@RestController`, `@RequestMapping` |
| **Service** | Business logic, validasi bisnis, koordinasi repository | `@Service`, `@Transactional` |
| **Repository** | Akses database via JPA/Hibernate | `@Repository`, extends `JpaRepository` |
| **Security** | Autentikasi JWT, otorisasi role-based | `@Configuration`, `OncePerRequestFilter` |
| **Exception** | Menangkap exception dan map ke HTTP response | `@ControllerAdvice`, `@ExceptionHandler` |

## 3.2 Request Lifecycle

```mermaid
sequenceDiagram
    actor Client
    participant Filter as JwtAuthenticationFilter
    participant Security as SecurityConfig
    participant Controller
    participant Validator as Bean Validator
    participant Service
    participant Repository
    participant DB as PostgreSQL

    Client->>Filter: HTTP Request + JWT Token (Header)
    Filter->>Filter: Extract & Validate JWT
    Filter->>Security: Set Authentication in SecurityContext
    Security->>Controller: Route to matched endpoint

    Controller->>Validator: @Valid Request DTO
    alt Validation Fails
        Validator-->>Client: 400 Bad Request (field errors)
    else Validation Passes
        Controller->>Service: Call service method
        Service->>Repository: Query / Save
        Repository->>DB: SQL via Hibernate
        DB-->>Repository: Result
        Repository-->>Service: Entity
        Service-->>Controller: Response DTO
        Controller-->>Client: 200 OK / 201 Created
    end
```

## 3.3 Security Architecture

```mermaid
graph TD
    A["📨 HTTP Request masuk"] --> B{"Endpoint termasuk\npublic path?"}

    B -->|"Ya (/api/auth/**, /api/products/**, /api/categories)"| C["✅ Langsung masuk ke Controller"]

    B -->|"Tidak"| D{"Ada JWT Token\ndi Header?"}
    D -->|"Tidak ada"| E["❌ 401 Unauthorized"]
    D -->|"Ada"| F["🔍 JwtAuthenticationFilter\nvalidasi token"]

    F -->|"Token invalid / expired"| G["❌ 401 Unauthorized"]
    F -->|"Token valid"| H["📌 Load UserDetails\nset SecurityContext"]

    H --> I{"Endpoint butuh\nrole ADMIN?"}
    I -->|"Ya (/api/admin/**)"| J{"User punya\nrole ADMIN?"}
    J -->|"Ya"| K["✅ Akses diberikan"]
    J -->|"Tidak"| L["❌ 403 Forbidden"]

    I -->|"Tidak"| K

    style C fill:#1b5e20,color:#fff
    style K fill:#1b5e20,color:#fff
    style E fill:#b71c1c,color:#fff
    style G fill:#b71c1c,color:#fff
    style L fill:#b71c1c,color:#fff
```

### Konfigurasi Endpoint Access

```
Public (tanpa login):
├── POST   /api/auth/register
├── POST   /api/auth/login
├── GET    /api/products/**
└── GET    /api/categories

Authenticated (perlu login):
├── GET    /api/users/me
├── PUT    /api/users/me
├── PUT    /api/users/me/password
├── GET    /api/cart
├── POST   /api/cart/items
├── PUT    /api/cart/items/{id}
├── DELETE /api/cart/items/{id}
├── POST   /api/orders/checkout
├── GET    /api/orders
└── GET    /api/orders/{id}

Admin Only (perlu role ADMIN):
├── POST   /api/admin/products
├── PUT    /api/admin/products/{id}
├── DELETE /api/admin/products/{id}
├── POST   /api/admin/categories
├── PUT    /api/admin/categories/{id}
├── DELETE /api/admin/categories/{id}
├── GET    /api/admin/orders
└── PUT    /api/admin/orders/{id}/status
```

## 3.4 Sequence Diagram — Fitur Utama

### 3.4.1 Register → Login

```mermaid
sequenceDiagram
    actor Client
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant PE as PasswordEncoder
    participant JWT as JwtTokenProvider

    Note over Client, JWT: 📝 REGISTER
    Client->>AC: POST /api/auth/register<br/>{"name","email","password"}
    AC->>AC: @Valid → validate input
    AC->>AS: register(request)
    AS->>UR: existsByEmail(email)
    UR-->>AS: false
    AS->>PE: encode(rawPassword)
    PE-->>AS: "$2a$10$..."
    AS->>UR: save(new User)
    UR-->>AS: savedUser
    AS-->>AC: UserResponse
    AC-->>Client: 201 Created

    Note over Client, JWT: 🔑 LOGIN
    Client->>AC: POST /api/auth/login<br/>{"email","password"}
    AC->>AS: login(request)
    AS->>UR: findByEmail(email)
    UR-->>AS: User
    AS->>PE: matches(raw, encoded)
    PE-->>AS: true ✅
    AS->>JWT: generateToken(user)
    JWT-->>AS: "eyJhbGciOi..."
    AS-->>AC: AuthResponse{token, user}
    AC-->>Client: 200 OK + JWT Token
```

### 3.4.2 Checkout Flow

```mermaid
sequenceDiagram
    actor Client
    participant OC as OrderController
    participant OS as OrderService
    participant CIR as CartItemRepository
    participant PR as ProductRepository
    participant OR as OrderRepository

    Client->>OC: POST /api/orders/checkout<br/>{"shippingAddress": "..."}
    OC->>OS: checkout(userId, request)

    OS->>CIR: findByUserId(userId)
    CIR-->>OS: List~CartItem~ [3 items]

    alt Cart kosong
        OS-->>OC: throw BadRequestException
        OC-->>Client: 400 "Cart is empty"
    else Cart ada isinya
        loop Tiap CartItem
            OS->>PR: findById(productId)
            PR-->>OS: Product
            Note over OS: Cek stok ≥ quantity
            Note over OS: Kurangi stock produk
            OS->>PR: save(updatedProduct)
            Note over OS: Buat OrderItem<br/>priceAtPurchase = product.price
        end

        Note over OS: Generate orderNumber<br/>"ORD-20260716-0001"
        Note over OS: Hitung totalPrice
        OS->>OR: save(Order + OrderItems)
        OR-->>OS: savedOrder

        OS->>CIR: deleteByUserId(userId)
        Note over CIR: 🗑️ Kosongkan keranjang

        OS-->>OC: OrderResponse
        OC-->>Client: 201 Created
    end
```

### 3.4.3 Product Catalog — Search, Filter, Pagination

```mermaid
sequenceDiagram
    actor Client
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository
    participant DB as PostgreSQL

    Client->>PC: GET /api/products<br/>?search=laptop<br/>&categoryId=1<br/>&minPrice=5000000<br/>&maxPrice=15000000<br/>&sort=price,asc<br/>&page=0&size=10

    PC->>PS: getAllProducts(search, categoryId, min, max, pageable)
    PS->>PS: Build JPA Specification

    PS->>PR: findAll(specification, pageable)
    PR->>DB: SELECT * FROM products<br/>WHERE name ILIKE '%laptop%'<br/>AND category_id = 1<br/>AND price BETWEEN 5000000 AND 15000000<br/>ORDER BY price ASC<br/>LIMIT 10 OFFSET 0

    DB-->>PR: ResultSet
    PR-->>PS: Page~Product~
    PS->>PS: Map Entity → ProductResponse
    PS-->>PC: Page~ProductResponse~
    PC-->>Client: 200 OK + Paginated JSON
```

## 3.5 Error Handling Architecture

```mermaid
graph TD
    EX["Exception Thrown"] --> GEH["GlobalExceptionHandler\n(@ControllerAdvice)"]

    GEH --> T{"Exception Type?"}

    T -->|"MethodArgumentNotValidException"| R400["400 Bad Request\n+ field-level errors"]
    T -->|"BadRequestException"| R400
    T -->|"AuthenticationException"| R401["401 Unauthorized"]
    T -->|"AccessDeniedException"| R403["403 Forbidden"]
    T -->|"ResourceNotFoundException"| R404["404 Not Found"]
    T -->|"Exception (catch-all)"| R500["500 Internal Server Error"]

    R400 --> API["ApiResponse JSON"]
    R401 --> API
    R403 --> API
    R404 --> API
    R500 --> API

    API --> CL["📤 Kirim ke Client"]

    style R400 fill:#e65100,color:#fff
    style R401 fill:#b71c1c,color:#fff
    style R403 fill:#880e4f,color:#fff
    style R404 fill:#4a148c,color:#fff
    style R500 fill:#1a237e,color:#fff
```

### Contoh Error Response

````carousel
```json
// 400 Bad Request — Validation Error
{
    "success": false,
    "message": "Validation failed",
    "data": {
        "email": "Email harus valid",
        "password": "Password minimal 8 karakter"
    },
    "timestamp": "2026-07-16T19:20:00"
}
```
<!-- slide -->
```json
// 401 Unauthorized
{
    "success": false,
    "message": "Invalid email or password",
    "data": null,
    "timestamp": "2026-07-16T19:20:00"
}
```
<!-- slide -->
```json
// 404 Not Found
{
    "success": false,
    "message": "Product not found with id: 99",
    "data": null,
    "timestamp": "2026-07-16T19:20:00"
}
```
````

## 3.6 Package Structure

```
com.alfarizy.ecommerce/
│
├── EcommerceApplication.java              ← Main class
│
├── config/                                ← Konfigurasi Spring
│   ├── SecurityConfig.java                  ← Security filter chain, CORS
│   └── AppConfig.java                       ← Bean definitions
│
├── security/                              ← Komponen keamanan
│   ├── JwtTokenProvider.java                ← Generate & validate JWT
│   ├── JwtAuthenticationFilter.java         ← Filter intercept tiap request
│   └── CustomUserDetailsService.java        ← Load user dari DB
│
├── entity/                                ← JPA Entity (mapping ke tabel DB)
│   ├── User.java
│   ├── Product.java
│   ├── Category.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Role.java                            ← Enum
│   └── OrderStatus.java                     ← Enum
│
├── dto/                                   ← Data Transfer Objects
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── ProductRequest.java
│   │   ├── CategoryRequest.java
│   │   ├── CartItemRequest.java
│   │   ├── UpdateCartItemRequest.java
│   │   ├── UpdateOrderStatusRequest.java
│   │   └── CheckoutRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── ProductResponse.java
│       ├── CategoryResponse.java
│       ├── CartItemResponse.java
│       ├── CartResponse.java
│       ├── OrderResponse.java
│       └── OrderItemResponse.java
│
├── repository/                            ← Data Access Layer
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
│
├── service/                               ← Business Logic Layer
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── CartService.java
│   └── OrderService.java
│
├── controller/                            ← REST API Layer
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ProductController.java
│   ├── AdminProductController.java
│   ├── CategoryController.java
│   ├── AdminCategoryController.java
│   ├── CartController.java
│   ├── OrderController.java
│   └── AdminOrderController.java
│
└── exception/                             ← Error Handling
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── BadRequestException.java
```

---
---

## 📊 Design Summary

| Aspek | Jumlah | Detail |
|-------|--------|--------|
| **Tabel Database** | 6 | users, categories, products, cart_items, orders, order_items |
| **Enum** | 2 | Role, OrderStatus |
| **Entity Classes** | 8 | 6 entity + 2 enum |
| **Request DTOs** | 10 | Register, Login, Profile, Password, Product, Category, Cart, UpdateCart, Checkout, UpdateOrderStatus |
| **Response DTOs** | 9 | Api, Auth, User, Product, Category, CartItem, Cart, Order, OrderItem |
| **Controllers** | 9 | Termasuk pemisahan admin controller |
| **Services** | 6 | Auth, User, Product, Category, Cart, Order |
| **Repositories** | 6 | Semua extends JpaRepository |
| **API Endpoints** | ~21 | Public (5), Authenticated (10), Admin (8) |

> [!TIP]
> Dokumen ini bisa dijadikan referensi utama saat implementasi. Setiap diagram bisa langsung di-mapping ke kode Java yang akan dibuat.
