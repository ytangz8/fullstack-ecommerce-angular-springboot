-- ─── Product Categories ────────────────────────────────────────────────────
INSERT INTO product_category (category_name) VALUES ('Books');
INSERT INTO product_category (category_name) VALUES ('Coffee Mugs');
INSERT INTO product_category (category_name) VALUES ('Mouse Pads');
INSERT INTO product_category (category_name) VALUES ('Luggage Tags');

-- ─── Books (category_id = 1) ────────────────────────────────────────────────
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

-- ─── Coffee Mugs (category_id = 2) ─────────────────────────────────────────
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

-- ─── Mouse Pads (category_id = 3) ──────────────────────────────────────────
INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated)
VALUES ('MOUSEPAD-1000', 'Mouse Pad - Express', 'Speed up with Express!', 'assets/images/products/mousepads/mousepad-luv2code-1000.png', true, 100, 17.99, 3, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated)
VALUES ('MOUSEPAD-1001', 'Mouse Pad - Spring', 'Code with Spring style!', 'assets/images/products/mousepads/mousepad-luv2code-1001.png', true, 100, 17.99, 3, NOW(), NOW());

-- ─── Luggage Tags (category_id = 4) ────────────────────────────────────────
INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated)
VALUES ('LUGTAG-1000', 'Luggage Tag - Spring', 'Travel in Spring style!', 'assets/images/products/luggagetags/luggagetag-luv2code-1000.png', true, 100, 9.99, 4, NOW(), NOW());

INSERT INTO product (sku, name, description, image_url, active, units_in_stock, unit_price, category_id, date_created, last_updated)
VALUES ('LUGTAG-1001', 'Luggage Tag - Express', 'Express yourself!', 'assets/images/products/luggagetags/luggagetag-luv2code-1001.png', true, 100, 9.99, 4, NOW(), NOW());
