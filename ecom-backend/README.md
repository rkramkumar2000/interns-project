# E-Commerce Application.

A full-stack e-commerce application built with Spring Boot and React, featuring a complete shopping experience with user authentication, product management, shopping cart, order processing, and payment integration.

## 🚀 Features

### Customer Features
- **User Authentication**: Register, login, and logout with JWT-based authentication
- **Product Browsing**: Browse products with pagination, filtering, and search
- **Shopping Cart**: Add/remove items, update quantities, persistent cart storage
- **Checkout Process**: Multi-step checkout with address management
- **Payment Integration**: Secure payment processing with Stripe
- **Order Management**: View order history and track order status
- **User Profile**: Manage delivery addresses and account details

### Admin Features
- **Dashboard**: Overview of sales, orders, and analytics
- **Product Management**: Add, edit, delete products with image upload
- **Category Management**: Organize products into categories
- **Order Management**: View and update order status
- **Seller Management**: Manage seller accounts and permissions
- **Analytics**: Business insights and reporting
- **Cache Management**: Clear and manage Redis cache regions

### Seller Features
- **Product Listing**: Add and manage own products
- **Order Fulfillment**: View and process orders for their products
- **Sales Analytics**: Track sales performance

### Performance Features
- **Redis Caching**: High-performance caching for products, categories, carts, and user data
- **Optimized Queries**: Reduced database load through intelligent caching
- **Cache Management API**: Admin endpoints to manage cache lifecycle

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 24
- **Database**: PostgreSQL
- **Cache**: Redis with Spring Cache
- **Security**: Spring Security with JWT
- **API Documentation**: Swagger/OpenAPI
- **Payment**: Stripe API
- **ORM**: Spring Data JPA/Hibernate
- **Build Tool**: Maven

### Frontend
- **Framework**: React 18.3.1
- **State Management**: Redux Toolkit
- **Routing**: React Router v7
- **UI Framework**: Tailwind CSS + Material UI
- **Build Tool**: Vite
- **Form Handling**: React Hook Form
- **HTTP Client**: Axios
- **Payment UI**: Stripe React

## 📋 Prerequisites

- Java 24 or higher
- Node.js 18 or higher
- PostgreSQL 14 or higher
- Redis 6.0 or higher
- Maven 3.8 or higher
- Stripe account (for payment processing)

## 🔧 Installation & Setup

### Backend Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd spring-boot-course-main/ecom-backend
```

2. Configure PostgreSQL database:
   - Create a database named `ecommerce`
   - Update database credentials in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. Set up Redis:
   - Install Redis:
     - **Windows**: Download from [GitHub](https://github.com/microsoftarchive/redis/releases)
     - **Mac**: `brew install redis`
     - **Linux**: `sudo apt-get install redis-server`
   - Start Redis server:
     ```bash
     redis-server
     ```
   - Redis configuration is already set in `application.properties` (default: localhost:6379)

4. Set up Stripe (for payment processing):
   - Get your Stripe secret key from [Stripe Dashboard](https://dashboard.stripe.com)
   - Set environment variable:
   ```bash
   export STRIPE_SECRET_KEY=your_stripe_secret_key
   ```

5. Run the backend:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd ../ecom-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Configure API endpoint (if needed) in `src/api/api.js`

4. Start the development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## 📁 Project Structure

```
spring-boot-course-main/
├── ecom-backend/
│   ├── src/main/java/com/ecommerce/project/
│   │   ├── config/          # Configuration classes (including RedisConfig)
│   │   ├── controller/      # REST API endpoints
│   │   ├── model/          # Entity classes
│   │   ├── payload/        # DTOs and response classes
│   │   ├── repositories/   # Data access layer
│   │   ├── security/       # Security configuration
│   │   ├── service/        # Business logic (with caching)
│   │   └── util/           # Utility classes
│   ├── src/main/resources/
│       └── application.properties
│   
│
├── ecom-frontend/
│   ├── src/
│   │   ├── api/            # API integration
│   │   ├── components/     # React components
│   │   │   ├── admin/      # Admin panel components
│   │   │   ├── auth/       # Authentication components
│   │   │   ├── cart/       # Shopping cart
│   │   │   ├── checkout/   # Checkout process
│   │   │   ├── home/       # Homepage
│   │   │   ├── products/   # Product listing
│   │   │   └── shared/     # Reusable components
│   │   ├── hooks/          # Custom React hooks
│   │   ├── store/          # Redux store configuration
│   │   └── utils/          # Utility functions
│   └── package.json
│
└── README.md
```


## 🗄️ Database Schema

### Entity Relationship Diagram

```
                                    ┌─────────────┐
                                    │    ROLE     │
                                    │─────────────│
                                    │ roleId (PK) │
                                    │ roleName    │
                                    └─────────────┘
                                           ║
                                           ║ Many-to-Many
                                           ║
                                    ┌─────────────┐
                      ┌─────────────┤    USER     ├─────────────┐
                      │             │─────────────│             │
                      │             │ userId (PK) │             │
                      │             │ userName    │             │
                      │             │ email       │             │
                      │             │ password    │             │
                      │             └─────────────┘             │
                      │                    ║                    │
                      │ 1:Many             ║ 1:1               │ 1:Many
                      │                    ║                    │
                      ▼                    ▼                    ▼
               ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
               │   ADDRESS   │      │    CART     │      │   PRODUCT   │
               │─────────────│      │─────────────│      │─────────────│
               │ addressId   │      │ cartId (PK) │      │ productId   │
               │ street      │      │ userId (FK) │      │ productName │
               │ city        │      │ totalPrice  │      │ price       │
               │ state       │      └─────────────┘      │ quantity    │
               │ zipCode     │             ║              │ sellerId(FK)│
               │ userId (FK) │             ║ 1:Many       │ categoryId  │
               └─────────────┘             ▼              └─────────────┘
                      │             ┌─────────────┐             ║
                      │             │  CART ITEM  │             ║ Many:1
                      │             │─────────────│             ▼
                      │             │ cartItemId  │      ┌─────────────┐
                      │             │ cartId (FK) │      │  CATEGORY   │
                      │             │ productId   │      │─────────────│
                      │             │ quantity    │      │ categoryId  │
                      │             │ price       │      │ categoryName│
                      │             └─────────────┘      └─────────────┘
                      │
                      │ Many:1
                      ▼
               ┌─────────────┐
               │    ORDER    │
               │─────────────│
               │ orderId (PK)│
               │ email       │
               │ orderDate   │
               │ totalAmount │──────────┐
               │ addressId   │          │ 1:1
               │ paymentId   │          ▼
               └─────────────┘   ┌─────────────┐
                      ║          │   PAYMENT   │
                      ║ 1:Many   │─────────────│
                      ▼          │ paymentId   │
               ┌─────────────┐   │ method      │
               │ ORDER ITEM  │   │ pgPaymentId │
               │─────────────│   │ pgStatus    │
               │ orderItemId │   └─────────────┘
               │ orderId(FK) │
               │ productId   │
               │ quantity    │
               │ price       │
               └─────────────┘

Legend:
───── : One-to-One relationship
═════ : One-to-Many relationship
║║║║║ : Many-to-Many relationship
```

### Relationship Summary

| From | To | Relationship Type | Description |
|------|-----|------------------|-------------|
| User | Role | Many-to-Many | Users can have multiple roles (USER, SELLER, ADMIN) |
| User | Cart | One-to-One | Each user has one shopping cart |
| User | Address | One-to-Many | Users can have multiple delivery addresses |
| User | Product | One-to-Many | Sellers (users) can list multiple products |
| User | Order | One-to-Many | Users can place multiple orders |
| Product | Category | Many-to-One | Products belong to one category |
| Product | CartItem | One-to-Many | A product can be in multiple cart items |
| Product | OrderItem | One-to-Many | A product can be in multiple order items |
| Cart | CartItem | One-to-Many | A cart contains multiple items |
| Order | OrderItem | One-to-Many | An order contains multiple items |
| Order | Payment | One-to-One | Each order has one payment |
| Order | Address | Many-to-One | Orders are delivered to one address |

### Core Entities

1. **User**
   - userId (PK)
   - username (unique)
   - email (unique)
   - password (encrypted)
   - roles (Many-to-Many with Role)
   - addresses (One-to-Many with Address)
   - cart (One-to-One with Cart)
   - products (One-to-Many with Product - for sellers)

2. **Product**
   - productId (PK)
   - productName
   - description
   - price
   - discount
   - quantity
   - image
   - category (Many-to-One with Category)
   - seller (Many-to-One with User)

3. **Order**
   - orderId (PK)
   - email
   - orderDate
   - totalAmount
   - orderStatus
   - orderItems (One-to-Many with OrderItem)
   - payment (One-to-One with Payment)
   - address (Many-to-One with Address)

4. **Cart**
   - cartId (PK)
   - user (One-to-One with User)
   - cartItems (One-to-Many with CartItem)
   - totalPrice

5. **Payment**
   - paymentId (PK)
   - paymentMethod
   - pgPaymentId (Payment Gateway ID)
   - pgStatus
   - pgResponseMessage
   - order (One-to-One with Order)

## 🔑 API Endpoints

### Authentication
- `POST /api/auth/signin` - User login
- `POST /api/auth/signup` - User registration
- `POST /api/auth/signout` - User logout
- `GET /api/auth/user` - Get current user details

### Products (Public)
- `GET /api/public/products` - Get all products (paginated)
- `GET /api/public/products/{id}` - Get product by ID
- `GET /api/public/categories` - Get all categories

### Cart (Authenticated)
- `POST /api/carts/products/{productId}/quantity/{quantity}` - Add to cart
- `GET /api/carts/users/cart` - Get user's cart
- `PUT /api/carts/products/{productId}/quantity/{quantity}` - Update cart item
- `DELETE /api/carts/{cartId}/product/{productId}` - Remove from cart

### Orders (Authenticated)
- `POST /api/order/users/payments/{paymentMethod}` - Place order
- `GET /api/order/users/orders` - Get user's orders
- `GET /api/order/users/orders/{orderId}` - Get order details

### Admin Endpoints
- `GET /api/admin/users` - Get all users
- `POST /api/admin/categories` - Create category
- `PUT /api/admin/categories/{id}` - Update category
- `DELETE /api/admin/categories/{id}` - Delete category
- `PUT /api/admin/orders/{orderId}/status` - Update order status

### Cache Management (Admin)
- `DELETE /api/cache/all` - Clear all caches
- `DELETE /api/cache/products` - Clear product caches
- `DELETE /api/cache/categories` - Clear category caches
- `DELETE /api/cache/carts` - Clear cart caches
- `DELETE /api/cache/orders` - Clear order caches

## 🔐 Security

- JWT-based authentication
- Role-based access control (USER, SELLER, ADMIN)
- Password encryption using BCrypt
- CORS configuration for frontend integration
- Secure payment processing with Stripe

## ⚡ Performance Optimization

### Redis Caching
The application uses Redis for caching frequently accessed data:

| Cache Region | TTL | Description |
|--------------|-----|-------------|
| products | 2 hours | Product listings and searches |
| categories | 6 hours | Category listings |
| carts | 30 minutes | User shopping carts |
| orders | 1 hour | Order information |
| userDetails | 15 minutes | User authentication data |
| sellers | 30 minutes | Seller listings |

Cache is automatically invalidated on data updates to ensure consistency.

## 🏃‍♂️ Default Users

The application creates default users on startup:

| Username | Password | Role |
|----------|----------|------|
| user1 | password1 | USER |
| seller1 | password2 | SELLER |
| admin | adminPass | ADMIN |


## 🧪 Testing

### Backend Tests
```bash
cd ecom-backend
mvn test
```

### Frontend Tests
```bash
cd ecom-frontend
npm test
```

## 📝 API Documentation

Once the backend is running, access the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

## 🚀 Deployment

### Backend Deployment
- Configure production database
- Set environment variables for sensitive data
- Build JAR: `mvn clean package`
- Deploy to your preferred platform (AWS, Heroku, etc.)

### Frontend Deployment
- Build production bundle: `npm run build`
- Deploy to static hosting (Netlify, Vercel, AWS S3, etc.)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
