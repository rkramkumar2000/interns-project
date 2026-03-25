-- Sample Data for Auth Service (auth_db)
-- This script populates the auth database with sample users and roles

-- Clear existing data (in correct order due to foreign key constraints)
DELETE FROM user_roles;
DELETE FROM users;
DELETE FROM roles;

-- Reset sequences (PostgreSQL)
ALTER SEQUENCE roles_role_id_seq RESTART WITH 1;
ALTER SEQUENCE users_user_id_seq RESTART WITH 1;

-- Insert Roles
INSERT INTO roles (role_id, role_name) VALUES 
(1, 'ROLE_USER'),
(2, 'ROLE_SELLER'),
(3, 'ROLE_ADMIN');

-- Insert Users (password is bcrypt hash of 'password123' for all users)
-- The hash: $2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O
INSERT INTO users (user_id, username, email, password, first_name, last_name, mobile_number, active, email_verified, created_at, updated_at) VALUES
(1, 'john_doe', 'john.doe@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'John', 'Doe', '+1234567890', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'jane_smith', 'jane.smith@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Jane', 'Smith', '+1234567891', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'admin_user', 'admin@ecommerce.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Admin', 'User', '+1234567892', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'seller1', 'seller1@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'First', 'Seller', '+1234567893', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'seller2', 'seller2@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Second', 'Seller', '+1234567894', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'mary_jones', 'mary.jones@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Mary', 'Jones', '+1234567895', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'bob_wilson', 'bob.wilson@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Bob', 'Wilson', '+1234567896', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'alice_brown', 'alice.brown@example.com', '$2a$10$rFRPkTAUVUfLqzQ3Vgwdxug7.i6eYoFBvWfUPjJb7pQdJxFwPgQ0O', 'Alice', 'Brown', '+1234567897', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- john_doe is USER
(2, 1), -- jane_smith is USER
(3, 1), -- admin_user is USER
(3, 3), -- admin_user is also ADMIN
(4, 1), -- seller1 is USER
(4, 2), -- seller1 is also SELLER
(5, 1), -- seller2 is USER
(5, 2), -- seller2 is also SELLER
(6, 1), -- mary_jones is USER
(7, 1), -- bob_wilson is USER
(8, 1); -- alice_brown is USER

-- Update sequences to continue from the last inserted value
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('roles_role_id_seq', (SELECT MAX(role_id) FROM roles));

-- Summary:
-- Total Users: 8
-- Admin Users: 1 (admin_user)
-- Sellers: 2 (seller1, seller2)
-- Regular Users: 5
-- All passwords: password123
-- All users are active and email verified 