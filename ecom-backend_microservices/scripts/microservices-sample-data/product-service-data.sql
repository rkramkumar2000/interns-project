-- Sample Data for Product Service (product_db)
-- This script populates the product database with categories and products

-- Clear existing data
DELETE FROM products;
DELETE FROM categories;

-- Reset sequences (PostgreSQL)
ALTER SEQUENCE categories_category_id_seq RESTART WITH 1;
ALTER SEQUENCE products_product_id_seq RESTART WITH 1;

-- Insert Categories (with parent-child hierarchy)
INSERT INTO categories (category_id, category_name, description, parent_id, active, created_at, updated_at) VALUES
(1, 'Electronics', 'Electronic devices and accessories', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Clothing & Fashion', 'Apparel, shoes, and fashion accessories', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Home & Kitchen', 'Home appliances and kitchen essentials', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Books & Media', 'Books, e-books, and digital media', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Sports & Outdoors', 'Sports equipment and outdoor gear', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Beauty & Personal Care', 'Beauty products and personal care items', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Toys & Games', 'Toys, games, and entertainment', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'Food & Grocery', 'Food items and grocery essentials', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Sub-categories
(9, 'Smartphones', 'Mobile phones and accessories', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Laptops', 'Notebook computers and accessories', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Audio', 'Headphones, speakers, and audio equipment', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Men''s Clothing', 'Clothing for men', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Women''s Clothing', 'Clothing for women', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Footwear', 'Shoes and footwear accessories', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Products
-- Note: seller_id references users in Auth Service, but we don't have foreign key constraint
-- seller_id 4 = seller1, seller_id 5 = seller2
INSERT INTO products (product_id, product_name, description, brand, price, quantity, category_id, sku, image_url, seller_id, discount_percentage, rating, review_count, active, featured, created_at, updated_at) VALUES
-- Electronics (seller1 = user_id 4)
(1, 'iPhone 14 Pro', 'Latest Apple smartphone with advanced camera system and A16 Bionic chip', 'Apple', 999.99, 50, 9, 'APPL-IPH14P-128', 'https://example.com/images/iphone14pro.jpg', 4, 10.0, 4.8, 1250, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Samsung Galaxy S23', 'Premium Android smartphone with stunning display and powerful performance', 'Samsung', 899.99, 30, 9, 'SAMS-GS23-256', 'https://example.com/images/galaxys23.jpg', 4, 15.0, 4.7, 890, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'MacBook Air M2', 'Lightweight laptop with M2 chip for incredible performance', 'Apple', 1199.99, 20, 10, 'APPL-MBA-M2-256', 'https://example.com/images/macbookairm2.jpg', 4, 5.0, 4.9, 670, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Sony WH-1000XM5', 'Industry-leading noise canceling wireless headphones', 'Sony', 399.99, 100, 11, 'SONY-WH1000XM5', 'https://example.com/images/sonywh1000xm5.jpg', 4, 20.0, 4.7, 2100, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'iPad Pro 12.9"', 'Powerful tablet with M2 chip and stunning Liquid Retina display', 'Apple', 1099.99, 25, 1, 'APPL-IPADP-12.9', 'https://example.com/images/ipadpro.jpg', 4, 8.0, 4.8, 450, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Dell XPS 13', 'Premium ultrabook with InfinityEdge display', 'Dell', 1299.99, 15, 10, 'DELL-XPS13-512', 'https://example.com/images/dellxps13.jpg', 4, 12.0, 4.6, 380, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'AirPods Pro 2', 'Premium wireless earbuds with active noise cancellation', 'Apple', 249.99, 200, 11, 'APPL-APP2-2023', 'https://example.com/images/airpodspro2.jpg', 4, 10.0, 4.6, 3200, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Clothing (seller2 = user_id 5)
(8, 'Levi''s 501 Original Jeans', 'Classic straight fit jeans in premium denim', 'Levi''s', 79.99, 200, 12, 'LEVI-501-BLU-32', 'https://example.com/images/levis501.jpg', 5, 25.0, 4.5, 890, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Nike Air Max 270', 'Comfortable running shoes with Max Air unit', 'Nike', 150.00, 150, 14, 'NIKE-AM270-BLK-10', 'https://example.com/images/nikeairmax270.jpg', 5, 30.0, 4.7, 1560, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Adidas Hoodie', 'Comfortable cotton blend hoodie with iconic 3-stripes', 'Adidas', 65.00, 100, 12, 'ADID-HOOD-GRY-L', 'https://example.com/images/adidashoodie.jpg', 5, 20.0, 4.4, 670, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Ray-Ban Aviator Sunglasses', 'Classic aviator sunglasses with UV protection', 'Ray-Ban', 168.00, 50, 2, 'RAYB-AVI-GLD', 'https://example.com/images/raybanaviator.jpg', 5, 15.0, 4.8, 2340, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'The North Face Jacket', 'Waterproof and breathable outdoor jacket', 'The North Face', 299.99, 40, 5, 'TNF-JACK-BLK-L', 'https://example.com/images/northfacejacket.jpg', 5, 10.0, 4.6, 450, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Zara Dress', 'Elegant evening dress with modern design', 'Zara', 89.99, 80, 13, 'ZARA-DRS-BLK-M', 'https://example.com/images/zaradress.jpg', 5, 35.0, 4.3, 230, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Home & Kitchen (seller1 = user_id 4)
(14, 'Instant Pot Duo 7-in-1', 'Multi-use pressure cooker for fast and easy meals', 'Instant Pot', 89.99, 80, 3, 'INST-POT-DUO7', 'https://example.com/images/instantpot.jpg', 4, 35.0, 4.7, 5600, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 'Dyson V15 Detect', 'Powerful cordless vacuum with laser dust detection', 'Dyson', 749.99, 15, 3, 'DYSN-V15-DET', 'https://example.com/images/dysonv15.jpg', 4, 10.0, 4.8, 890, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 'Nespresso Vertuo Coffee Maker', 'Premium coffee and espresso machine', 'Nespresso', 189.99, 60, 3, 'NESP-VERT-BLK', 'https://example.com/images/nespresso.jpg', 4, 25.0, 4.5, 1200, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(17, 'All-Clad Cookware Set', 'Professional 10-piece stainless steel cookware set', 'All-Clad', 699.99, 20, 3, 'ALLC-COOK-10P', 'https://example.com/images/allcladset.jpg', 4, 20.0, 4.9, 340, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(18, 'KitchenAid Stand Mixer', 'Professional 5-quart stand mixer in various colors', 'KitchenAid', 379.99, 35, 3, 'KAID-MIX-5QT', 'https://example.com/images/kitchenaidmixer.jpg', 4, 15.0, 4.8, 2100, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Books (seller2 = user_id 5)
(19, 'Atomic Habits', 'James Clear - Transform your life with tiny changes', 'Penguin', 27.99, 500, 4, 'BOOK-ATOM-HAB', 'https://example.com/images/atomichabits.jpg', 5, 40.0, 4.9, 12000, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 'The Psychology of Money', 'Morgan Housel - Timeless lessons on wealth and happiness', 'Harriman House', 24.99, 300, 4, 'BOOK-PSY-MON', 'https://example.com/images/psychologyofmoney.jpg', 5, 35.0, 4.8, 8900, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 'Educated: A Memoir', 'Tara Westover - Inspiring story of education and family', 'Random House', 28.99, 200, 4, 'BOOK-EDU-MEM', 'https://example.com/images/educated.jpg', 5, 30.0, 4.7, 6700, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 'Project Hail Mary', 'Andy Weir - Science fiction adventure from the author of The Martian', 'Ballantine', 29.99, 250, 4, 'BOOK-PROJ-HM', 'https://example.com/images/projecthailmary.jpg', 5, 25.0, 4.8, 4500, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 'The Midnight Library', 'Matt Haig - A novel about all the lives you could have lived', 'Viking', 26.99, 350, 4, 'BOOK-MID-LIB', 'https://example.com/images/midnightlibrary.jpg', 5, 20.0, 4.6, 5600, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Sports & Outdoors (seller1 = user_id 4)
(24, 'Yeti Tumbler 30oz', 'Insulated stainless steel tumbler keeps drinks cold or hot', 'Yeti', 35.00, 200, 5, 'YETI-TUMB-30', 'https://example.com/images/yetitumbler.jpg', 4, 15.0, 4.7, 3400, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 'Fitbit Charge 5', 'Advanced fitness and health tracker', 'Fitbit', 149.99, 100, 5, 'FITB-CHG5-BLK', 'https://example.com/images/fitbitcharge5.jpg', 4, 20.0, 4.5, 2100, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 'Coleman Camping Tent', '6-person dome tent for family camping', 'Coleman', 199.99, 40, 5, 'COLE-TENT-6P', 'https://example.com/images/colemantent.jpg', 4, 30.0, 4.4, 890, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 'Hydro Flask Water Bottle', '32oz insulated water bottle in multiple colors', 'Hydro Flask', 44.95, 150, 5, 'HYDR-FLASK-32', 'https://example.com/images/hydroflask.jpg', 4, 10.0, 4.6, 4500, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 'Yoga Mat Premium', 'Extra thick non-slip exercise mat', 'Manduka', 79.99, 100, 5, 'MAND-YOGA-MAT', 'https://example.com/images/yogamat.jpg', 4, 40.0, 4.8, 1200, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update sequences
SELECT setval('categories_category_id_seq', (SELECT MAX(category_id) FROM categories));
SELECT setval('products_product_id_seq', (SELECT MAX(product_id) FROM products));

-- Summary:
-- Categories: 14 (8 main + 6 sub-categories)
-- Products: 28
-- Seller 1 (user_id 4): Electronics, Home & Kitchen, Sports products
-- Seller 2 (user_id 5): Clothing, Books products
-- All products have ratings, reviews, and various discount percentages 