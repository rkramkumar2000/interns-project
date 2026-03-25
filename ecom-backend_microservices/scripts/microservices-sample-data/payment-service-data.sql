-- Sample Data for Payment Service (payment_db)
-- This script populates the payment database with sample payments and transactions

-- Clear existing data
DELETE FROM transactions;
DELETE FROM payments;

-- Reset sequences (PostgreSQL)
ALTER SEQUENCE payments_payment_id_seq RESTART WITH 1;
ALTER SEQUENCE transactions_transaction_id_seq RESTART WITH 1;

-- Insert Payments
-- Note: order_id and user_id reference data in other services
INSERT INTO payments (payment_id, order_id, user_id, payment_method, payment_provider, 
                     transaction_id, stripe_payment_intent_id, stripe_payment_method_id,
                     amount, currency, refunded_amount, payment_status, description,
                     metadata, customer_email, customer_name, billing_address,
                     failure_reason, failure_code, created_at, updated_at, completed_at,
                     refund_count, last_refund_at) VALUES
-- John's successful payment
(1, 1, 1, 'CARD', 'STRIPE', 
 'pi_1234567890abcdef', 'pi_1234567890abcdef', 'pm_1234567890abcdef',
 1222.78, 'USD', 0.00, 'COMPLETED', 'Order #1 - Electronics purchase',
 '{"orderId": 1, "items": 2}', 'john.doe@example.com', 'John Doe',
 '{"addressLine1": "123 Main Street", "addressLine2": "Sunshine Apartments", "city": "New York", "state": "NY", "country": "USA", "postalCode": "10001"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days',
 0, NULL),

-- Jane's successful payment
(2, 2, 2, 'CARD', 'STRIPE',
 'pi_0987654321fedcba', 'pi_0987654321fedcba', 'pm_0987654321fedcba',
 315.31, 'USD', 0.00, 'COMPLETED', 'Order #2 - Fashion items with discount',
 '{"orderId": 2, "items": 2, "coupon": "SAVE10"}', 'jane.smith@example.com', 'Jane Smith',
 '{"addressLine1": "789 Elm Road", "addressLine2": "Blue Sky Tower", "city": "Chicago", "state": "IL", "country": "USA", "postalCode": "60601"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days',
 0, NULL),

-- Mary's COD payment (pending)
(3, 3, 6, 'COD', 'MANUAL',
 'COD_ORDER_003', NULL, NULL,
 139.76, 'USD', 0.00, 'PENDING', 'Order #3 - Cash on delivery',
 '{"orderId": 3, "items": 3, "coupon": "FIRST20"}', 'mary.jones@example.com', 'Mary Jones',
 '{"addressLine1": "147 Broadway", "addressLine2": "City Heights", "city": "Boston", "state": "MA", "country": "USA", "postalCode": "02101"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL,
 0, NULL),

-- Bob's processing payment
(4, 4, 7, 'CARD', 'STRIPE',
 'pi_1111222233334444', 'pi_1111222233334444', 'pm_1111222233334444',
 169.08, 'USD', 0.00, 'PROCESSING', 'Order #4 - Sunglasses',
 '{"orderId": 4, "items": 1}', 'bob.wilson@example.com', 'Bob Wilson',
 '{"addressLine1": "258 Washington St", "addressLine2": "Liberty Tower", "city": "Denver", "state": "CO", "country": "USA", "postalCode": "80201"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', NULL,
 0, NULL),

-- Admin's refunded payment
(5, 5, 3, 'CARD', 'STRIPE',
 'pi_5555666677778888', 'pi_5555666677778888', 'pm_5555666677778888',
 2395.98, 'USD', 2395.98, 'REFUNDED', 'Order #5 - Cancelled MacBook order',
 '{"orderId": 5, "items": 1, "reason": "stock_unavailable"}', 'admin@ecommerce.com', 'Admin User',
 '{"addressLine1": "321 Pine Street", "addressLine2": "Admin Building", "city": "San Francisco", "state": "CA", "country": "USA", "postalCode": "94101"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '20 days',
 1, CURRENT_TIMESTAMP - INTERVAL '18 days'),

-- Alice's recent payment
(6, 6, 8, 'CARD', 'STRIPE',
 'pi_9999000011112222', 'pi_9999000011112222', 'pm_9999000011112222',
 87.26, 'USD', 0.00, 'COMPLETED', 'Order #6 - Books purchase',
 '{"orderId": 6, "items": 3}', 'alice.brown@example.com', 'Alice Brown',
 '{"addressLine1": "369 Lake Drive", "addressLine2": "Waterfront Condos", "city": "Austin", "state": "TX", "country": "USA", "postalCode": "78701"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour',
 0, NULL),

-- Failed payment example
(7, NULL, 2, 'CARD', 'STRIPE',
 'pi_failed123456789', 'pi_failed123456789', 'pm_failed123456789',
 199.99, 'USD', 0.00, 'FAILED', 'Failed payment attempt',
 '{"attempt": 1}', 'jane.smith@example.com', 'Jane Smith',
 '{"addressLine1": "789 Elm Road", "addressLine2": "Blue Sky Tower", "city": "Chicago", "state": "IL", "country": "USA", "postalCode": "60601"}',
 'Your card was declined', 'card_declined', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', NULL,
 0, NULL),

-- Partial refund example
(8, 2, 2, 'CARD', 'STRIPE',
 'pi_partial_refund123', 'pi_partial_refund123', 'pm_partial_refund123',
 599.99, 'USD', 150.00, 'PARTIALLY_REFUNDED', 'Order with partial refund',
 '{"orderId": 2, "refund_reason": "item_damaged"}', 'jane.smith@example.com', 'Jane Smith',
 '{"addressLine1": "789 Elm Road", "addressLine2": "Blue Sky Tower", "city": "Chicago", "state": "IL", "country": "USA", "postalCode": "60601"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '10 days',
 1, CURRENT_TIMESTAMP - INTERVAL '7 days');

-- Insert Transactions
INSERT INTO transactions (transaction_id, payment_id, transaction_type, amount, currency,
                         external_transaction_id, status, request_data, response_data,
                         error_message, error_code, created_at, completed_at) VALUES
-- Successful charge transactions
(1, 1, 'CHARGE', 1222.78, 'USD',
 'ch_1234567890abcdef', 'COMPLETED',
 '{"amount": 122278, "currency": "usd", "payment_method": "pm_1234567890abcdef"}',
 '{"id": "ch_1234567890abcdef", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),

(2, 2, 'CHARGE', 315.31, 'USD',
 'ch_0987654321fedcba', 'COMPLETED',
 '{"amount": 31531, "currency": "usd", "payment_method": "pm_0987654321fedcba"}',
 '{"id": "ch_0987654321fedcba", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Refund transactions
(3, 5, 'CHARGE', 2395.98, 'USD',
 'ch_5555666677778888', 'COMPLETED',
 '{"amount": 239598, "currency": "usd", "payment_method": "pm_5555666677778888"}',
 '{"id": "ch_5555666677778888", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),

(4, 5, 'REFUND', 2395.98, 'USD',
 're_5555666677778888', 'COMPLETED',
 '{"charge": "ch_5555666677778888", "amount": 239598, "reason": "requested_by_customer"}',
 '{"id": "re_5555666677778888", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days'),

-- Processing transaction
(5, 4, 'CHARGE', 169.08, 'USD',
 'ch_1111222233334444', 'PENDING',
 '{"amount": 16908, "currency": "usd", "payment_method": "pm_1111222233334444"}',
 '{"id": "ch_1111222233334444", "status": "processing"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1 day', NULL),

-- Failed transaction
(6, 7, 'CHARGE', 199.99, 'USD',
 'ch_failed123456789', 'FAILED',
 '{"amount": 19999, "currency": "usd", "payment_method": "pm_failed123456789"}',
 '{"id": "ch_failed123456789", "status": "failed", "failure_code": "card_declined"}',
 'Your card was declined', 'card_declined', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Partial refund transaction
(7, 8, 'CHARGE', 599.99, 'USD',
 'ch_partial_refund123', 'COMPLETED',
 '{"amount": 59999, "currency": "usd", "payment_method": "pm_partial_refund123"}',
 '{"id": "ch_partial_refund123", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),

(8, 8, 'PARTIAL_REFUND', 150.00, 'USD',
 're_partial_150', 'COMPLETED',
 '{"charge": "ch_partial_refund123", "amount": 15000, "reason": "requested_by_customer"}',
 '{"id": "re_partial_150", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Recent successful transaction
(9, 6, 'CHARGE', 87.26, 'USD',
 'ch_9999000011112222', 'COMPLETED',
 '{"amount": 8726, "currency": "usd", "payment_method": "pm_9999000011112222"}',
 '{"id": "ch_9999000011112222", "status": "succeeded"}',
 NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour');

-- Update sequences
SELECT setval('payments_payment_id_seq', (SELECT MAX(payment_id) FROM payments));
SELECT setval('transactions_transaction_id_seq', (SELECT MAX(transaction_id) FROM transactions));

-- Summary:
-- Total Payments: 8
-- Payment Statuses: COMPLETED (3), PROCESSING (1), PENDING (1), FAILED (1), REFUNDED (1), PARTIALLY_REFUNDED (1)
-- Payment Methods: CARD (7), COD (1)
-- Payment Providers: STRIPE (7), MANUAL (1)
-- Total Transactions: 9
-- Transaction Types: CHARGE (6), REFUND (2), PARTIAL_REFUND (1)
-- Refunded Payments: 2 (1 full, 1 partial) 