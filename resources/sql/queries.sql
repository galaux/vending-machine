-- Users

-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, username, password, role, deposit)
VALUES (:id, :username, :password, :role, :deposit)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET role = :role
WHERE username = :username

-- :name set-user-deposit! :! :n
-- :doc updates an existing user deposit
UPDATE users
SET deposit = :deposit
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the username
SELECT * FROM users
WHERE username = :username

-- :name get-user-by-id :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
 WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name get-users
SELECT * FROM users


-- Products

-- :name get-products
SELECT * FROM products

-- :name create-product! :! :n
-- :doc creates a new product record
INSERT INTO products
(id, product_name, amount_available, cost, seller_id)
VALUES (:id, :product_name, :amount_available, :cost, :seller_id)

-- :name get-product :? :1
-- :doc retrieves a product by id
SELECT * FROM products
 WHERE id = :id

-- :name get-product-by-name :? :1
-- :doc retrieves a product by name
SELECT * FROM products
WHERE product_name = :product_name

-- :name update-product! :! :n
-- :doc updates an existing product record
UPDATE products
SET product_name = :product_name,
    amount_available = :amount_available,
    cost = :cost
WHERE id = :id
