-- Sample Data for Cart Service (cart_db)
-- This script populates the cart database with sample carts and items

-- Clear existing data
DELETE FROM cart_items;
DELETE FROM carts;

-- Reset sequences (PostgreSQL)
ALTER SEQUENCE carts_cart_id_seq RESTART WITH 1;
ALTER SEQUENCE cart_items_cart_item_id_seq RESTART WITH 1;

-- Insert Carts for users (some with items, some empty)
-- Note: user_id references users in Auth Service, product_id references products in Product Service
INSERT INTO carts (cart_id, user_id, session_id, status, coupon_code, coupon_discount, created_at, updated_at) VALUES
(1, 1, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),     -- john_doe has items
(2, 2, NULL, 'ACTIVE', 'SAVE10', 10.0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP), -- jane_smith has items with coupon
(3, 3, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP),    -- admin_user empty cart
(4, 4, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP),     -- seller1 empty cart
(5, 5, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP),     -- seller2 empty cart
(6, 6, NULL, 'ACTIVE', 'FIRST20', 20.0, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP), -- mary_jones has items with coupon
(7, 7, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),      -- bob_wilson empty cart
(8, 8, NULL, 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP),     -- alice_brown empty cart
-- Anonymous carts (session-based)
(9, NULL, 'sess_abc123xyz', 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP),
(10, NULL, 'sess_def456uvw', 'ABANDONED', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(11, NULL, 'sess_ghi789rst', 'ACTIVE', NULL, 0.0, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP);

-- Insert Cart Items
-- Prices match the discounted prices from Product Service
INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity, price, discount_percentage, created_at, updated_at) VALUES
-- John's cart (cart_id: 1)
(1, 1, 4, 1, 399.99, 20.0, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP),    -- Sony headphones
(2, 1, 19, 2, 27.99, 40.0, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP),    -- 2x Atomic Habits books
(3, 1, 7, 1, 249.99, 10.0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),    -- AirPods Pro 2

-- Jane's cart (cart_id: 2) - has 10% coupon
(4, 2, 8, 1, 79.99, 25.0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),     -- Levi's jeans
(5, 2, 14, 1, 89.99, 35.0, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP),    -- Instant Pot
(6, 2, 25, 1, 149.99, 20.0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),    -- Fitbit Charge 5

-- Mary's cart (cart_id: 6) - has 20% coupon
(7, 6, 10, 1, 65.00, 20.0, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP),    -- Adidas hoodie
(8, 6, 23, 1, 26.99, 20.0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),     -- The Midnight Library
(9, 6, 28, 1, 79.99, 40.0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),     -- Yoga mat

-- Anonymous cart 1 (cart_id: 9)
(10, 9, 1, 1, 999.99, 10.0, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP),  -- iPhone 14 Pro
(11, 9, 9, 1, 150.00, 30.0, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP),   -- Nike Air Max 270

-- Anonymous cart 2 (cart_id: 10) - abandoned
(12, 10, 3, 1, 1199.99, 5.0, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'), -- MacBook Air M2

-- Anonymous cart 3 (cart_id: 11)
(13, 11, 20, 1, 24.99, 35.0, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP),  -- The Psychology of Money
(14, 11, 27, 2, 44.95, 10.0, CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP); -- 2x Hydro Flask

-- Update sequences
SELECT setval('carts_cart_id_seq', (SELECT MAX(cart_id) FROM carts));
SELECT setval('cart_items_cart_item_id_seq', (SELECT MAX(cart_item_id) FROM cart_items));

-- Summary:
-- Total Carts: 11 (8 user carts, 3 anonymous carts)
-- Active User Carts with Items: 3 (john_doe, jane_smith, mary_jones)
-- Empty User Carts: 5
-- Anonymous Carts: 3 (2 active, 1 abandoned)
-- Carts with Coupons: 2 (jane_smith: SAVE10, mary_jones: FIRST20)
-- Total Cart Items: 14 