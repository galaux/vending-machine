CREATE TABLE users
  (id VARCHAR(20) PRIMARY KEY,
   username VARCHAR(30) NOT NULL UNIQUE,
   password VARCHAR(300) NOT NULL,
   role INT NOT NULL,
   deposit INT NOT NULL);

CREATE TABLE products
  (id VARCHAR(20) PRIMARY KEY,
   product_name VARCHAR(30) NOT NULL UNIQUE,
   amount_available INT NOT NULL,
   cost INT NOT NULL,
   seller_id VARCHAR(20),
   CONSTRAINT FK_user_product FOREIGN KEY (seller_id) REFERENCES users(id));
