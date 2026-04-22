--celeste_pim.sql
CREATE DATABASE celeste_pim;
USE celeste_pim;

--products
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100),
    category VARCHAR(100),
    quantity INT,
    price DOUBLE,
    supplier VARCHAR(100),
    reorder_level INT
);

--suppliers
CREATE TABLE suppliers(
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100),
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    status VARCHAR(20)
);

--orders
CREATE TABLE purchase_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier VARCHAR(100),
    product_name VARCHAR(100),
    quantity INT,
    order_date DATE,
    status VARCHAR(50)
);