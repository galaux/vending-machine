# vending-machine

Generated using Luminus version "4.38"

## Exercise brief

Design an API for a vending machine, allowing users with a "seller" role to
add, update or remove products, while users with a "buyer" role can deposit
coins into the machine and make purchases. Your vending machine should only
accept 5, 10, 20, 50 and 100 cent coins.

### Tasks

REST API should be implemented consuming and producing "application/json"
- Implement product model with amountAvailable, cost (should be in multiples of
  5), productName and sellerId ﬁelds
- Implement user model with username, password, deposit and role ﬁelds
- Implement an authentication method (basic, oAuth, JWT or something else, the
  choice is yours)
- All of the endpoints should be authenticated unless stated otherwise
- Implement CRUD for users (POST /user should not require authentication to
  allow new user registration)
- Implement CRUD for a product model (GET can be called by anyone, while POST,
  PUT and DELETE can be called only by the seller user who created the product)
- Implement /deposit endpoint so users with a "buyer" role can deposit only 5,
  10, 20, 50 and 100 cent coins into their vending machine account (one coin at
  the time)
- Implement /buy endpoint (accepts productId, amount of products) so users with
  a “buyer” role can buy a product (shouldn't be able to buy multiple diﬀerent
  products at the same time) with the money they’ve deposited. API should
  return total they’ve spent, the product they’ve purchased and their change if
  there’s any (in an array of 5, 10, 20, 50 and 100 cent coins)
- Implement /reset endpoint so users with a "buyer" role can reset their
  deposit back to 0
- Take time to think about possible edge cases and access issues that should be
  solved

### Evaluation criteria

- Language/Framework of choice best practices
- Edge cases covered
- Write tests for /deposit, /buy and one CRUD endpoint of your choice
- Code readability and optimization

### Bonus

- If somebody is already logged in with the same credentials, the user should
  be given a message "There is already an active session using your account".
  In this case the user should be able to terminate all the active sessions on
  their account via an endpoint i.e. /logout/all
- Attention to security

### Deliverables

A Github repository with public access. Please have the solution running and a
Postman / Swagger collection ready on your computer so the domain expert can
tell you which tests to run on the API.

### Miscellaneous

- _How long do I have to do this?_ You should deliver it in 7 days at latest.
- _What languages should the interface be in?_ English only


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To run the test, run: `lein test`.

To start a web server for the application, run: `lein run`, then check [the API
documentation](http://localhost:3000/).

**IMPORTANT**: at leaset `POST` on `/api/user` and `/api/login` MUST be
encrypted as they take the user password.

### Example data for manual check

```bash
# Seller creates profile
curl \
  -X POST \
  'http://localhost:3000/api/user' \
  -H "Content-Type: application/json" \
  --data '{"username": "Rick", "password": "k843k!#8ki32", "role": 1}'
# {"id":"MQTSHDXOZGMQBTFYBHAX","username":"Rick","role":1,"deposit":0}

# Seller logs in
curl \
  -X POST \
  'http://localhost:3000/api/login' \
  -H "Content-Type: application/json" \
  --data '{"username": "Rick", "password": "k843k!#8ki32"}'
# {"token":"…"}

# Seller creates product
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${SELLER_TOKEN}" \
  'http://localhost:3000/api/product' \
  --data '{"product_name": "almond", "amount_available": 10, "cost": 25}'
# {"id":"PRFHYHYBXKMQJANMDLUS",
#  "product_name":"almond",
#  "amount_available":10,
#  "cost":25,
#  "seller_id":"MQTSHDXOZGMQBTFYBHAX"}

# Buyer creates profile
curl \
  -X POST \
  'http://localhost:3000/api/user' \
  -H "Content-Type: application/json" \
  --data '{"username": "Rick", "password": "k843k!#8ki32", "role": 1}'
# {"id":"MQTSHDXOZGMQBTFYBHAX","username":"Morty","role":0,"deposit":0}

# Buyer logs in
curl \
  -X POST \
  'http://localhost:3000/api/login' \
  -H "Content-Type: application/json" \
  --data '{"username": "Morty", "password": "k843k!#8ki32"}'
# {"token":"…"}

# Buyer deposits 100
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${BUYER_TOKEN}" \
  'http://localhost:3000/api/deposit' \
  --data '{"coin": 100}'
# {"id":"MQTSHDXOZGMQBTFYBHAX","username":"Morty","role":0,"deposit":100}

# Seller tries to buy
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${SELLER_TOKEN}" \
  'http://localhost:3000/api/buy' \
  --data '{"id": "PRFHYHYBXKMQJANMDLUS", "amount": 2}'
# "User doesn't have buyer role"

# Buyer tries to buy more than available
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${BUYER_TOKEN}" \
  'http://localhost:3000/api/buy' \
  --data '{"id": "PRFHYHYBXKMQJANMDLUS", "amount": 185}'
# "Not enough product in stock"

# Buyer tries to buy but not enough cash
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${BUYER_TOKEN}" \
  'http://localhost:3000/api/buy' \
  --data '{"id": "PRFHYHYBXKMQJANMDLUS", "amount": 5}'
# "Insufficient deposit"

# Buyer successfully buys product
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Token ${BUYER_TOKEN}" \
  'http://localhost:3000/api/buy' \
  --data '{"id": "PRFHYHYBXKMQJANMDLUS", "amount": 3}'
# {
#   "total_spent": 75,
#   "purchased": {
#     "product": {
#       "id": "PRFHYHYBXKMQJANMDLUS",
#       "product_name": "almond"
#     },
#     "amount": 3
#   },
#   "change": [50, 20, 5]
# }
```

## License

Copyright © 2022 Guillaume ALAUX
