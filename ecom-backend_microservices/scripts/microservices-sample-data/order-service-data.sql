-- Sample Data for Order Service (order_db)
-- This script populates the order database with sample orders

-- Clear existing data
DELETE FROM order_items;
DELETE FROM orders;

-- Reset sequences (PostgreSQL)
ALTER SEQUENCE orders_order_id_seq RESTART WITH 1;
ALTER SEQUENCE order_items_order_item_id_seq RESTART WITH 1;

-- Insert Orders
-- Note: Data is denormalized - user info and addresses stored as JSON
INSERT INTO orders (order_id, user_id, user_email, user_name, order_date, delivery_date, order_status, 
                   payment_id, payment_method, payment_status, subtotal, discount, tax, shipping_cost, 
                   total_amount, delivery_address, billing_address, order_notes, tracking_number, 
                   coupon_code, created_at, updated_at) VALUES
-- John's delivered order
(1, 1, 'john.doe@example.com', 'John Doe', 
 CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'DELIVERED',
 1, 'CARD', 'COMPLETED', 1219.97, 121.99, 109.80, 15.00, 1222.78,
 '{"addressLine1": "123 Main Street", "addressLine2": "Sunshine Apartments", "city": "New York", "state": "NY", "country": "USA", "postalCode": "10001", "phoneNumber": "+1234567890"}',
 '{"addressLine1": "123 Main Street", "addressLine2": "Sunshine Apartments", "city": "New York", "state": "NY", "country": "USA", "postalCode": "10001", "phoneNumber": "+1234567890"}',
 'Please leave at doorstep', 'TRK-2024-001-JD', NULL,
 CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),

-- Jane's shipped order with coupon
(2, 2, 'jane.smith@example.com', 'Jane Smith',
 CURRENT_TIMESTAMP - INTERVAL '8 days', NULL, 'SHIPPED',
 2, 'CARD', 'COMPLETED', 374.99, 96.25, 26.57, 10.00, 315.31,
 '{"addressLine1": "789 Elm Road", "addressLine2": "Blue Sky Tower", "city": "Chicago", "state": "IL", "country": "USA", "postalCode": "60601", "phoneNumber": "+1234567891"}',
 '{"addressLine1": "789 Elm Road", "addressLine2": "Blue Sky Tower", "city": "Chicago", "state": "IL", "country": "USA", "postalCode": "60601", "phoneNumber": "+1234567891"}',
 'Ring doorbell twice', 'TRK-2024-002-JS', 'SAVE10',
 CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Mary's processing order
(3, 6, 'mary.jones@example.com', 'Mary Jones',
 CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, 'PROCESSING',
 3, 'COD', 'PENDING', 171.98, 52.20, 11.98, 8.00, 139.76,
 '{"addressLine1": "147 Broadway", "addressLine2": "City Heights", "city": "Boston", "state": "MA", "country": "USA", "postalCode": "02101", "phoneNumber": "+1234567895"}',
 '{"addressLine1": "147 Broadway", "addressLine2": "City Heights", "city": "Boston", "state": "MA", "country": "USA", "postalCode": "02101", "phoneNumber": "+1234567895"}',
 'Call before delivery', 'TRK-2024-003-MJ', 'FIRST20',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Bob's pending order
(4, 7, 'bob.wilson@example.com', 'Bob Wilson',
 CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'PENDING',
 4, 'CARD', 'PROCESSING', 168.00, 25.20, 14.28, 12.00, 169.08,
 '{"addressLine1": "258 Washington St", "addressLine2": "Liberty Tower", "city": "Denver", "state": "CO", "country": "USA", "postalCode": "80201", "phoneNumber": "+1234567896"}',
 '{"addressLine1": "258 Washington St", "addressLine2": "Liberty Tower", "city": "Denver", "state": "CO", "country": "USA", "postalCode": "80201", "phoneNumber": "+1234567896"}',
 NULL, NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Admin's cancelled order
(5, 3, 'admin@ecommerce.com', 'Admin User',
 CURRENT_TIMESTAMP - INTERVAL '20 days', NULL, 'CANCELLED',
 5, 'CARD', 'REFUNDED', 2399.98, 240.00, 216.00, 20.00, 2395.98,
 '{"addressLine1": "321 Pine Street", "addressLine2": "Admin Building", "city": "San Francisco", "state": "CA", "country": "USA", "postalCode": "94101", "phoneNumber": "+1234567892"}',
 '{"addressLine1": "321 Pine Street", "addressLine2": "Admin Building", "city": "San Francisco", "state": "CA", "country": "USA", "postalCode": "94101", "phoneNumber": "+1234567892"}',
 'Cancelled due to stock unavailability', NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '18 days'),

-- Alice's confirmed order
(6, 8, 'alice.brown@example.com', 'Alice Brown',
 CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL, 'CONFIRMED',
 6, 'CARD', 'COMPLETED', 89.94, 18.98, 6.30, 10.00, 87.26,
 '{"addressLine1": "369 Lake Drive", "addressLine2": "Waterfront Condos", "city": "Austin", "state": "TX", "country": "USA", "postalCode": "78701", "phoneNumber": "+1234567897"}',
 '{"addressLine1": "369 Lake Drive", "addressLine2": "Waterfront Condos", "city": "Austin", "state": "TX", "country": "USA", "postalCode": "78701", "phoneNumber": "+1234567897"}',
 'Gift wrap please', NULL, NULL,
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour');

-- Insert Order Items (denormalized product data)
INSERT INTO order_items (order_item_id, order_id, product_id, product_name, product_sku, 
                        product_image, product_description, unit_price, quantity, discount, 
                        discount_percentage, total_price, notes, created_at, updated_at) VALUES
-- Order 1 (John's order) - Electronics
(1, 1, 1, 'iPhone 14 Pro', 'APPL-IPH14P-128', 
 'https://example.com/images/iphone14pro.jpg', 'Latest Apple smartphone', 
 999.99, 1, 99.99, 10.0, 899.99, NULL, 
 CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
(2, 1, 4, 'Sony WH-1000XM5', 'SONY-WH1000XM5',
 'https://example.com/images/sonywh1000xm5.jpg', 'Noise canceling headphones',
 399.99, 1, 80.00, 20.0, 319.99, 'Black color',
 CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),

-- Order 2 (Jane's order) - Mixed
(3, 2, 9, 'Nike Air Max 270', 'NIKE-AM270-BLK-10',
 'https://example.com/images/nikeairmax270.jpg', 'Running shoes',
 150.00, 1, 45.00, 30.0, 105.00, 'Size 8',
 CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(4, 2, 12, 'The North Face Jacket', 'TNF-JACK-BLK-L',
 'https://example.com/images/northfacejacket.jpg', 'Waterproof jacket',
 299.99, 1, 30.00, 10.0, 269.99, 'Medium size',
 CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Order 3 (Mary's order) - Mixed with COD
(5, 3, 28, 'Yoga Mat Premium', 'MAND-YOGA-MAT',
 'https://example.com/images/yogamat.jpg', 'Non-slip exercise mat',
 79.99, 1, 32.00, 40.0, 47.99, 'Purple color',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(6, 3, 10, 'Adidas Hoodie', 'ADID-HOOD-GRY-L',
 'https://example.com/images/adidashoodie.jpg', 'Cotton blend hoodie',
 65.00, 1, 13.00, 20.0, 52.00, NULL,
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(7, 3, 23, 'The Midnight Library', 'BOOK-MID-LIB',
 'https://example.com/images/midnightlibrary.jpg', 'Novel by Matt Haig',
 26.99, 3, 5.40, 20.0, 64.77, 'Gift wrap each book',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Order 4 (Bob's order) - Single item
(8, 4, 11, 'Ray-Ban Aviator Sunglasses', 'RAYB-AVI-GLD',
 'https://example.com/images/raybanaviator.jpg', 'Classic aviator sunglasses',
 168.00, 1, 25.20, 15.0, 142.80, NULL,
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Order 5 (Admin's cancelled order) - High value
(9, 5, 3, 'MacBook Air M2', 'APPL-MBA-M2-256',
 'https://example.com/images/macbookairm2.jpg', 'Laptop with M2 chip',
 1199.99, 2, 120.00, 5.0, 2279.98, 'Space Gray',
 CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),

-- Order 6 (Alice's order) - Books
(10, 6, 19, 'Atomic Habits', 'BOOK-ATOM-HAB',
 'https://example.com/images/atomichabits.jpg', 'Book by James Clear',
 27.99, 1, 11.20, 40.0, 16.79, NULL,
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(11, 6, 20, 'The Psychology of Money', 'BOOK-PSY-MON',
 'https://example.com/images/psychologyofmoney.jpg', 'Book by Morgan Housel',
 24.99, 2, 8.75, 35.0, 32.48, 'Signed copies if available',
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(12, 6, 27, 'Hydro Flask Water Bottle', 'HYDR-FLASK-32',
 'https://example.com/images/hydroflask.jpg', '32oz insulated bottle',
 44.95, 1, 4.50, 10.0, 40.45, 'Blue color',
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours');

-- Update sequences
SELECT setval('orders_order_id_seq', (SELECT MAX(order_id) FROM orders));
SELECT setval('order_items_order_item_id_seq', (SELECT MAX(order_item_id) FROM order_items));

-- Summary:
-- Total Orders: 6
-- Order Statuses: DELIVERED (1), SHIPPED (1), PROCESSING (1), PENDING (1), CANCELLED (1), CONFIRMED (1)
-- Payment Methods: CARD (5), COD (1)
-- Payment Statuses: COMPLETED (3), PROCESSING (1), PENDING (1), REFUNDED (1)
-- Orders with Coupons: 2 (jane_smith: SAVE10, mary_jones: FIRST20)
-- Total Order Items: 12
-- Average Items per Order: 2 