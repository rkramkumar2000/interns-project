# Microservices Sample Data

This directory contains SQL scripts to populate all microservice databases with sample data for testing and demonstration purposes.

## 📁 Files

- `auth-service-data.sql` - Users and roles for Auth Service
- `product-service-data.sql` - Categories and products for Product Service  
- `cart-service-data.sql` - Shopping carts and cart items for Cart Service
- `order-service-data.sql` - Orders and order items for Order Service
- `payment-service-data.sql` - Payments and transactions for Payment Service

## 🚀 Quick Start

### Prerequisites
1. Docker Desktop must be running
2. All infrastructure services must be started:
   ```bash
   cd ecom-backend
   docker-compose -f docker-compose-microservices.yml up -d
   ```
3. Wait 30 seconds for databases to initialize

### Loading Sample Data

#### For Windows (PowerShell):
```powershell
cd ecom-backend\scripts
.\load-sample-data.ps1
```

#### For Mac/Linux:
```bash
cd ecom-backend/scripts
chmod +x load-sample-data.sh
./load-sample-data.sh
```

## 📊 Sample Data Summary

### Users (8 total)
| Email | Password | Roles | Description |
|-------|----------|-------|-------------|
| admin@ecommerce.com | password123 | ADMIN, USER | Administrator |
| seller1@example.com | password123 | SELLER, USER | First seller |
| seller2@example.com | password123 | SELLER, USER | Second seller |
| john.doe@example.com | password123 | USER | Regular user with orders |
| jane.smith@example.com | password123 | USER | Regular user with cart |
| mary.jones@example.com | password123 | USER | User with COD order |
| bob.wilson@example.com | password123 | USER | User with pending order |
| alice.brown@example.com | password123 | USER | User with recent order |

### Products (28 total)
- **Electronics**: 7 products (iPhone, Samsung Galaxy, MacBook, etc.)
- **Clothing**: 6 products (Jeans, shoes, jackets, etc.)
- **Home & Kitchen**: 5 products (Instant Pot, Dyson, coffee makers, etc.)
- **Books**: 5 products (Best sellers)
- **Sports & Outdoors**: 5 products (Fitness trackers, camping gear, etc.)

### Shopping Carts (11 total)
- 3 active carts with items (john_doe, jane_smith, mary_jones)
- 5 empty user carts
- 3 anonymous session-based carts
- 2 carts have coupon codes applied

### Orders (6 total)
- **Delivered**: 1 (john_doe - electronics)
- **Shipped**: 1 (jane_smith - fashion items)
- **Processing**: 1 (mary_jones - COD)
- **Pending**: 1 (bob_wilson)
- **Cancelled**: 1 (admin_user - refunded)
- **Confirmed**: 1 (alice_brown - recent)

### Payments (8 total)
- 3 completed payments
- 1 processing payment
- 1 pending COD payment
- 1 failed payment
- 1 fully refunded payment
- 1 partially refunded payment

## 🧪 Testing Scenarios

### 1. Existing User with Cart
- Login as `john.doe@example.com`
- Cart already has 3 items
- Can proceed to checkout

### 2. Seller Access
- Login as `seller1@example.com`
- Can create/update products
- Owns electronics and home products

### 3. Admin Operations
- Login as `admin@ecommerce.com`
- Can manage all entities
- Has a cancelled/refunded order

### 4. Anonymous Shopping
- Session `sess_abc123xyz` has items
- Can be merged after login

### 5. Payment Testing
- Various payment states available
- Stripe test payment intents included
- Refund examples present

## 🔄 Re-running Scripts

The scripts are idempotent - they clear existing data before inserting new data. You can safely re-run them anytime.

## ⚠️ Important Notes

1. **Development Only**: This data is for development/testing only
2. **Passwords**: All users have the same password: `password123`
3. **Stripe Keys**: Payment data uses test Stripe IDs
4. **Timestamps**: Orders and payments have realistic historical timestamps
5. **References**: Data maintains logical references between services (user IDs, product IDs, etc.)

## 🛠️ Customization

To modify sample data:
1. Edit the appropriate SQL file
2. Maintain ID references between services
3. Re-run the load script

## 📝 SQL File Structure

Each SQL file follows this pattern:
1. Clear existing data (DELETE statements)
2. Reset sequences
3. Insert sample data
4. Update sequences to correct values
5. Summary comments

## 🐛 Troubleshooting

### Container name mismatch
Edit the container name in the load script:
- PowerShell: `$POSTGRES_CONTAINER = "your-container-name"`
- Bash: `POSTGRES_CONTAINER="your-container-name"`

### Permission denied (Linux/Mac)
```bash
chmod +x load-sample-data.sh
```

### Foreign key errors
Ensure you load data in the correct order (as the script does):
1. Auth Service (users)
2. Product Service (products)
3. Cart Service
4. Order Service
5. Payment Service 