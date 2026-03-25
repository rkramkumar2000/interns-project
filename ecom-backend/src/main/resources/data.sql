-- Sample Data for E-Commerce Application
-- This script populates the database with sample data for testing

-- Clear existing data (optional - comment out if you want to keep existing data)
-- Note: Order matters due to foreign key constraints
DELETE FROM order_items;
DELETE FROM payments;
DELETE FROM orders;
DELETE FROM cart_items;
DELETE FROM carts;
DELETE FROM addresses;
DELETE FROM user_role;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users;
DELETE FROM roles;

-- Reset auto-increment counters (PostgreSQL syntax)
-- ALTER SEQUENCE users_user_id_seq RESTART WITH 1;
-- ALTER SEQUENCE roles_role_id_seq RESTART WITH 1;
-- ALTER SEQUENCE categories_category_id_seq RESTART WITH 1;
-- ALTER SEQUENCE products_product_id_seq RESTART WITH 1;
-- ALTER SEQUENCE addresses_address_id_seq RESTART WITH 1;
-- ALTER SEQUENCE carts_cart_id_seq RESTART WITH 1;
-- ALTER SEQUENCE cart_items_cart_item_id_seq RESTART WITH 1;
-- ALTER SEQUENCE orders_order_id_seq RESTART WITH 1;
-- ALTER SEQUENCE order_items_order_item_id_seq RESTART WITH 1;
-- ALTER SEQUENCE payments_payment_id_seq RESTART WITH 1;

-- Insert Roles
INSERT INTO roles (role_id, role_name) VALUES 
(1, 'ROLE_USER'),
(2, 'ROLE_SELLER'),
(3, 'ROLE_ADMIN');

-- Insert Users (password is bcrypt hash of 'password123' for all users)
INSERT INTO users (user_id, username, email, password) VALUES
(1, 'john_doe', 'john.doe@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(2, 'jane_smith', 'jane.smith@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(3, 'admin_user', 'admin@ecommerce.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(4, 'seller1', 'seller1@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(5, 'seller2', 'seller2@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(6, 'mary_jones', 'mary.jones@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(7, 'bob_wilson', 'bob.wilson@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O'),
(8, 'alice_brown', 'alice.brown@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O');

-- Assign roles to users
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), -- john_doe is USER
(2, 1), -- jane_smith is USER
(3, 3), -- admin_user is ADMIN
(4, 2), -- seller1 is SELLER
(4, 1), -- seller1 is also USER
(5, 2), -- seller2 is SELLER
(5, 1), -- seller2 is also USER
(6, 1), -- mary_jones is USER
(7, 1), -- bob_wilson is USER
(8, 1); -- alice_brown is USER

-- Insert Categories
INSERT INTO categories (category_id, category_name) VALUES
(1, 'Electronics'),
(2, 'Clothing & Fashion'),
(3, 'Home & Kitchen'),
(4, 'Books & Media'),
(5, 'Sports & Outdoors'),
(6, 'Beauty & Personal Care'),
(7, 'Toys & Games'),
(8, 'Food & Grocery');

-- Insert Products (with seller assignments)
INSERT INTO products (product_id, product_name, description, price, discount, quantity, image, category_id, seller_id, special_price) VALUES
-- Electronics (seller1)
(1, 'iPhone 14 Pro', 'Latest Apple smartphone with advanced camera system and A16 Bionic chip', 999.99, 10, 50, 'iphone14pro.jpg', 1, 4, 899.99),
(2, 'Samsung Galaxy S23', 'Premium Android smartphone with stunning display and powerful performance', 899.99, 15, 30, 'galaxys23.jpg', 1, 4, 764.99),
(3, 'MacBook Air M2', 'Lightweight laptop with M2 chip for incredible performance', 1199.99, 5, 20, 'macbookairm2.jpg', 1, 4, 1139.99),
(4, 'Sony WH-1000XM5', 'Industry-leading noise canceling wireless headphones', 399.99, 20, 100, 'sonywh1000xm5.jpg', 1, 4, 319.99),
(5, 'iPad Pro 12.9"', 'Powerful tablet with M2 chip and stunning Liquid Retina display', 1099.99, 8, 25, 'ipadpro.jpg', 1, 4, 1011.99),

-- Clothing (seller2)
(6, 'Levi\'s 501 Original Jeans', 'Classic straight fit jeans in premium denim', 79.99, 25, 200, 'levis501.jpg', 2, 5, 59.99),
(7, 'Nike Air Max 270', 'Comfortable running shoes with Max Air unit', 150.00, 30, 150, 'nikeairmax270.jpg', 2, 5, 105.00),
(8, 'Adidas Hoodie', 'Comfortable cotton blend hoodie with iconic 3-stripes', 65.00, 20, 100, 'adidashoodie.jpg', 2, 5, 52.00),
(9, 'Ray-Ban Aviator Sunglasses', 'Classic aviator sunglasses with UV protection', 168.00, 15, 50, 'raybanaviatior.jpg', 2, 5, 142.80),
(10, 'The North Face Jacket', 'Waterproof and breathable outdoor jacket', 299.99, 10, 40, 'northfacejacket.jpg', 2, 5, 269.99),

-- Home & Kitchen (seller1)
(11, 'Instant Pot Duo 7-in-1', 'Multi-use pressure cooker for fast and easy meals', 89.99, 35, 80, 'instantpot.jpg', 3, 4, 58.49),
(12, 'Dyson V15 Detect', 'Powerful cordless vacuum with laser dust detection', 749.99, 10, 15, 'dysonv15.jpg', 3, 4, 674.99),
(13, 'Nespresso Vertuo Coffee Maker', 'Premium coffee and espresso machine', 189.99, 25, 60, 'nespresso.jpg', 3, 4, 142.49),
(14, 'All-Clad Cookware Set', 'Professional 10-piece stainless steel cookware set', 699.99, 20, 20, 'allcladset.jpg', 3, 4, 559.99),
(15, 'KitchenAid Stand Mixer', 'Professional 5-quart stand mixer in various colors', 379.99, 15, 35, 'kitchenaidmixer.jpg', 3, 4, 322.99),

-- Books (seller2)
(16, 'Atomic Habits', 'James Clear - Transform your life with tiny changes', 27.99, 40, 500, 'atomichabits.jpg', 4, 5, 16.79),
(17, 'The Psychology of Money', 'Morgan Housel - Timeless lessons on wealth and happiness', 24.99, 35, 300, 'psychologyofmoney.jpg', 4, 5, 16.24),
(18, 'Educated: A Memoir', 'Tara Westover - Inspiring story of education and family', 28.99, 30, 200, 'educated.jpg', 4, 5, 20.29),
(19, 'Project Hail Mary', 'Andy Weir - Science fiction adventure from the author of The Martian', 29.99, 25, 250, 'projecthailmary.jpg', 4, 5, 22.49),
(20, 'The Midnight Library', 'Matt Haig - A novel about all the lives you could have lived', 26.99, 20, 350, 'midnightlibrary.jpg', 4, 5, 21.59),

-- Sports & Outdoors (seller1)
(21, 'Yeti Tumbler 30oz', 'Insulated stainless steel tumbler keeps drinks cold or hot', 35.00, 15, 200, 'yetitumbler.jpg', 5, 4, 29.75),
(22, 'Fitbit Charge 5', 'Advanced fitness and health tracker', 149.99, 20, 100, 'fitbitcharge5.jpg', 5, 4, 119.99),
(23, 'Coleman Camping Tent', '6-person dome tent for family camping', 199.99, 30, 40, 'colemantent.jpg', 5, 4, 139.99),
(24, 'Hydro Flask Water Bottle', '32oz insulated water bottle in multiple colors', 44.95, 10, 150, 'hydroflask.jpg', 5, 4, 40.46),
(25, 'Yoga Mat Premium', 'Extra thick non-slip exercise mat', 79.99, 40, 100, 'yogamat.jpg', 5, 4, 47.99);

-- Insert Addresses for users
INSERT INTO addresses (address_id, street, building_name, city, state, country, pincode, user_id) VALUES
(1, '123 Main Street', 'Sunshine Apartments', 'New York', 'NY', 'USA', '10001', 1),
(2, '456 Oak Avenue', 'Green Valley Complex', 'Los Angeles', 'CA', 'USA', '90001', 1),
(3, '789 Elm Road', 'Blue Sky Tower', 'Chicago', 'IL', 'USA', '60601', 2),
(4, '321 Pine Street', 'Admin Building', 'San Francisco', 'CA', 'USA', '94101', 3),
(5, '654 Market Street', 'Commerce Center', 'Seattle', 'WA', 'USA', '98101', 4),
(6, '987 Fifth Avenue', 'Plaza Residences', 'Miami', 'FL', 'USA', '33101', 5),
(7, '147 Broadway', 'City Heights', 'Boston', 'MA', 'USA', '02101', 6),
(8, '258 Washington St', 'Liberty Tower', 'Denver', 'CO', 'USA', '80201', 7),
(9, '369 Lake Drive', 'Waterfront Condos', 'Austin', 'TX', 'USA', '78701', 8);

-- Insert Carts for users (empty carts for some users)
INSERT INTO carts (cart_id, user_id, total_price) VALUES
(1, 1, 289.97),  -- john_doe has items
(2, 2, 159.98),  -- jane_smith has items
(3, 3, 0.0),     -- admin_user empty cart
(4, 4, 0.0),     -- seller1 empty cart
(5, 5, 0.0),     -- seller2 empty cart
(6, 6, 52.00),   -- mary_jones has items
(7, 7, 0.0),     -- bob_wilson empty cart
(8, 8, 0.0);     -- alice_brown empty cart

-- Insert Cart Items for active carts
INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity, discount, product_price) VALUES
-- John's cart
(1, 1, 4, 1, 20, 319.99),    -- Sony headphones
(2, 1, 16, 2, 40, 16.79),    -- 2x Atomic Habits books
-- Jane's cart
(3, 2, 6, 1, 25, 59.99),     -- Levi's jeans
(4, 2, 11, 1, 35, 58.49),    -- Instant Pot
-- Mary's cart
(5, 6, 8, 1, 20, 52.00);     -- Adidas hoodie

-- Insert sample completed orders
INSERT INTO payments (payment_id, payment_method, pg_payment_id, pg_status, pg_response_message, pg_name) VALUES
(1, 'CARD', 'pi_1234567890abcdef', 'COMPLETED', 'Payment successful', 'Stripe'),
(2, 'CARD', 'pi_0987654321fedcba', 'COMPLETED', 'Payment successful', 'Stripe'),
(3, 'COD', 'COD_ORDER_001', 'PENDING', 'Cash on delivery', 'COD'),
(4, 'CARD', 'pi_1111222233334444', 'COMPLETED', 'Payment successful', 'Stripe');

INSERT INTO orders (order_id, email, order_date, total_amount, order_status, address_id, payment_id) VALUES
(1, 'john.doe@example.com', '2024-01-15', 1219.97, 'DELIVERED', 1, 1),
(2, 'jane.smith@example.com', '2024-01-20', 265.00, 'SHIPPED', 3, 2),
(3, 'mary.jones@example.com', '2024-01-25', 47.99, 'PROCESSING', 7, 3),
(4, 'bob.wilson@example.com', '2024-01-28', 142.80, 'PENDING', 8, 4);

-- Insert Order Items
INSERT INTO order_items (order_item_id, order_id, product_id, quantity, discount, ordered_product_price) VALUES
-- Order 1 (John's order)
(1, 1, 1, 1, 10, 899.99),    -- iPhone 14 Pro
(2, 1, 4, 1, 20, 319.99),    -- Sony headphones
-- Order 2 (Jane's order)
(3, 2, 7, 1, 30, 105.00),    -- Nike shoes
(4, 2, 10, 1, 10, 269.99),   -- North Face jacket (price mismatch fixed)
-- Order 3 (Mary's order)
(5, 3, 25, 1, 40, 47.99),    -- Yoga mat
-- Order 4 (Bob's order)
(6, 4, 9, 1, 15, 142.80);    -- Ray-Ban sunglasses

-- Update sequences (PostgreSQL)
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('roles_role_id_seq', (SELECT MAX(role_id) FROM roles));
SELECT setval('categories_category_id_seq', (SELECT MAX(category_id) FROM categories));
SELECT setval('products_product_id_seq', (SELECT MAX(product_id) FROM products));
SELECT setval('addresses_address_id_seq', (SELECT MAX(address_id) FROM addresses));
SELECT setval('carts_cart_id_seq', (SELECT MAX(cart_id) FROM carts));
SELECT setval('cart_items_cart_item_id_seq', (SELECT MAX(cart_item_id) FROM cart_items));
SELECT setval('orders_order_id_seq', (SELECT MAX(order_id) FROM orders));
SELECT setval('order_items_order_item_id_seq', (SELECT MAX(order_item_id) FROM order_items));
SELECT setval('payments_payment_id_seq', (SELECT MAX(payment_id) FROM payments));

-- Summary of test data:
-- Users: 8 (1 admin, 2 sellers who are also users, 5 regular users)
-- Categories: 8
-- Products: 25 (distributed across categories and sellers)
-- Addresses: 9
-- Active Carts: 3 (with items)
-- Orders: 4 (various statuses)
-- All users have password: password123 