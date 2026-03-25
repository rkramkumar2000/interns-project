#!/bin/bash
set -e

# Create multiple databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE ecommerce;
    CREATE DATABASE auth_db;
    CREATE DATABASE product_db;
    CREATE DATABASE order_db;
    CREATE DATABASE cart_db;
    CREATE DATABASE payment_db;
    CREATE DATABASE notification_db;
    CREATE DATABASE user_db;
EOSQL 