-- Insert Product Categories
INSERT INTO product_category (category_name) VALUES ('Books');
INSERT INTO product_category (category_name) VALUES ('Coffee Mugs');
INSERT INTO product_category (category_name) VALUES ('Mouse Pads');
INSERT INTO product_category (category_name) VALUES ('Luggage Tags');

-- Insert Products for Books Category
INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('BOOK-TECH-1000', 'JavaScript - The Fun Parts', 'Learn JavaScript', 'assets/images/products/books/book-luv2code-1000.png', true, 100, 19.99, 1, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('BOOK-TECH-1001', 'Spring Framework Tutorial', 'Learn Spring', 'assets/images/products/books/book-luv2code-1001.png', true, 100, 29.99, 1, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('BOOK-TECH-1002', 'Kubernetes - Deploying Containers', 'Learn Kubernetes', 'assets/images/products/books/book-luv2code-1002.png', true, 100, 24.99, 1, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('BOOK-TECH-1003', 'Internet of Things (IoT) - Getting Started', 'Learn IoT', 'assets/images/products/books/book-luv2code-1003.png', true, 100, 29.99, 1, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('BOOK-TECH-1004', 'The Go Programming Language: A to Z', 'Learn Go', 'assets/images/products/books/book-luv2code-1004.png', true, 100, 24.99, 1, NOW(), NOW());

-- Insert Products for Coffee Mugs Category
INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('MUG-1000', 'Coffee Mug - Express', 'Do you love Express? Then you will love this mug!', 'assets/images/products/coffeemugs/coffeemug-luv2code-1000.png', true, 100, 18.99, 2, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('MUG-1001', 'Coffee Mug - Crash Course', 'Crash Course in style!', 'assets/images/products/coffeemugs/coffeemug-luv2code-1001.png', true, 100, 18.99, 2, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('MUG-1002', 'Coffee Mug - Javascript Guru', 'For the JavaScript Guru!', 'assets/images/products/coffeemugs/coffeemug-luv2code-1002.png', true, 100, 18.99, 2, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('MUG-1003', 'Coffee Mug - Coding in Progress', 'Coding in Progress...', 'assets/images/products/coffeemugs/coffeemug-luv2code-1003.png', true, 100, 18.99, 2, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated) 
VALUES ('MUG-1004', 'Coffee Mug - Loop', 'Loop de loop!', 'assets/images/products/coffeemugs/coffeemug-luv2code-1004.png', true, 100, 18.99, 2, NOW(), NOW());

-- Insert Countries
INSERT INTO country (code, name) VALUES ('US', 'United States');
INSERT INTO country (code, name) VALUES ('CA', 'Canada');
INSERT INTO country (code, name) VALUES ('IN', 'India');
INSERT INTO country (code, name) VALUES ('BR', 'Brazil');

-- Insert States for United States
INSERT INTO state (name, country_id) VALUES ('Alabama', 1);
INSERT INTO state (name, country_id) VALUES ('Alaska', 1);
INSERT INTO state (name, country_id) VALUES ('Arizona', 1);
INSERT INTO state (name, country_id) VALUES ('California', 1);
INSERT INTO state (name, country_id) VALUES ('Colorado', 1);
INSERT INTO state (name, country_id) VALUES ('Florida', 1);
INSERT INTO state (name, country_id) VALUES ('Georgia', 1);
INSERT INTO state (name, country_id) VALUES ('New York', 1);
INSERT INTO state (name, country_id) VALUES ('Texas', 1);
INSERT INTO state (name, country_id) VALUES ('Washington', 1);

-- Insert States for Canada
INSERT INTO state (name, country_id) VALUES ('Alberta', 2);
INSERT INTO state (name, country_id) VALUES ('British Columbia', 2);
INSERT INTO state (name, country_id) VALUES ('Ontario', 2);
INSERT INTO state (name, country_id) VALUES ('Quebec', 2);

-- Insert States for India
INSERT INTO state (name, country_id) VALUES ('Maharashtra', 3);
INSERT INTO state (name, country_id) VALUES ('Karnataka', 3);
INSERT INTO state (name, country_id) VALUES ('Tamil Nadu', 3);
INSERT INTO state (name, country_id) VALUES ('Delhi', 3);

-- Insert States for Brazil
INSERT INTO state (name, country_id) VALUES ('Sao Paulo', 4);
INSERT INTO state (name, country_id) VALUES ('Rio de Janeiro', 4);
INSERT INTO state (name, country_id) VALUES ('Brasilia', 4);
