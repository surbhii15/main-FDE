# E-Bookstore — Data Model

Derived from the capstone wireframes / customer journeys (slides 3–10).

## 1. Wireframe → entity mapping

| # | Customer journey step (wireframe) | Entities / features |
|---|-----------------------------------|---------------------|
| 1 | Login page | `User` — register, login (JWT) |
| 2 | User authentication | `User.role`, Spring Security + JWT |
| 3 | Product catalogue per category | `Product`, `Category` — paged search & filter |
| 4 | Order history + **Buy it again** | `Order`, `OrderItem` — re-add an order's items to the cart |
| 5 | Select category | `Category` |
| 6 | Browse brands | `Brand` (for books: publisher / imprint) |
| 7 | Select product, **tentative delivery date**, related products | `Product.deliveryDays` → computed `estimatedDeliveryDate`; related = same author or category |
| 8 | Add to basket | `Cart`, `CartItem` |
| — | **Recommendations** based on order history | computed from purchased categories / authors |
| 9 | Select delivery address | `Address` (address book); snapshotted onto `Order` |
| 10 | Initiate payment, **redeem gift points** | `Payment`, `GiftPointTransaction`, `User.giftPoints` |
| 11 | Payment confirmation | `Payment.status`, `Payment.transactionRef` |
| 12 | Purchase confirmation, **cancel within 48 h** | `Order.status`, `Order.placedAt`, refund + stock/points reversal |

## 2. ER diagram

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : has
    USERS ||--|| CARTS : owns
    CARTS ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : "added as"
    CATEGORIES ||--o{ PRODUCTS : groups
    BRANDS ||--o{ PRODUCTS : publishes
    USERS ||--o{ ORDERS : places
    ORDERS ||--|{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : "ordered as"
    ORDERS ||--o{ PAYMENTS : "paid by"
    USERS ||--o{ GIFT_POINT_TRANSACTIONS : earns
    ORDERS ||--o{ GIFT_POINT_TRANSACTIONS : "relates to"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar phone
        varchar role "CUSTOMER | ADMIN"
        int gift_points "current balance"
        timestamptz created_at
    }
    ADDRESSES {
        bigint id PK
        bigint user_id FK
        varchar label "Home, Office"
        varchar recipient_name
        varchar line1
        varchar line2
        varchar city
        varchar state
        varchar postal_code
        varchar country
        varchar phone
        boolean is_default
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar slug UK
        varchar description
    }
    BRANDS {
        bigint id PK
        varchar name UK
        varchar description
    }
    PRODUCTS {
        bigint id PK
        varchar sku UK
        varchar title
        varchar author
        varchar isbn UK
        text description
        numeric price
        int stock_quantity
        int delivery_days "lead time for tentative delivery date"
        numeric rating
        varchar image_url
        boolean active
        bigint category_id FK
        bigint brand_id FK
        timestamptz created_at
    }
    CARTS {
        bigint id PK
        bigint user_id FK,UK
        timestamptz updated_at
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }
    ORDERS {
        bigint id PK
        varchar order_number UK
        bigint user_id FK
        varchar status "PENDING_PAYMENT | CONFIRMED | CANCELLED"
        numeric subtotal
        int points_redeemed
        int points_earned
        numeric discount
        numeric total
        varchar ship_recipient_name "address snapshot..."
        varchar ship_line1
        varchar ship_city
        varchar ship_postal_code
        varchar ship_country
        date estimated_delivery_date
        timestamptz placed_at
        timestamptz cancelled_at
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar title "snapshot"
        numeric unit_price "snapshot"
        int quantity
        numeric line_total
    }
    PAYMENTS {
        bigint id PK
        bigint order_id FK
        varchar method "CREDIT_CARD | DEBIT_CARD | UPI | NET_BANKING | CASH_ON_DELIVERY"
        varchar status "SUCCESS | PENDING | FAILED | REFUNDED | CANCELLED"
        numeric amount
        varchar card_brand
        varchar card_last4 "never the full PAN"
        varchar transaction_ref
        varchar failure_reason
        timestamptz created_at
    }
    GIFT_POINT_TRANSACTIONS {
        bigint id PK
        bigint user_id FK
        bigint order_id FK
        int points "+ earned / - redeemed"
        varchar type "EARNED | REDEEMED | REFUNDED | REVERSED"
        varchar description
        timestamptz created_at
    }
```

## 3. Business rules

| Rule | Value / behaviour |
|------|-------------------|
| Tentative delivery date | `today + product.deliveryDays`; an order's date is the **latest** of its items |
| Related products | Same author first, then same category; excludes the product itself; active only |
| Recommendations | Products in the categories / by the authors the user has bought, excluding already-purchased; falls back to top-rated |
| Buy it again | Copies an order's still-available items into the cart (quantity capped by stock) |
| Stock | Reserved atomically (`UPDATE … WHERE stock >= qty`) when the order is placed, so concurrent checkouts can't oversell; released when the order is cancelled |
| Failed payment | Recorded with status `FAILED`; the order stays `PENDING_PAYMENT` (stock and points still reserved) so the customer can retry with another method |
| Cash on delivery | Payment `PENDING`, order `CONFIRMED`; voided (`CANCELLED`) if the order is cancelled |
| Gift points — redeem | 1 point = 1.00 currency unit off; max **50 %** of the subtotal; deducted at checkout |
| Gift points — earn | 1 point per 10.00 of the paid total, credited on successful payment |
| Payment | Simulated gateway. Cards must pass the Luhn check and must not be expired; a Luhn-valid card ending in `0002` (e.g. `4000 0000 0000 0002`) is declined, as is a UPI id ending `@fail`. Only brand and last 4 digits are stored; CVV is never persisted |
| Cancel order | Allowed within **48 h** of `placedAt` while not already cancelled. Refunds the payment, restores stock, gives back redeemed points and reverses earned points |
| Order snapshots | Item title/price and shipping address are copied onto the order so later catalogue/address edits don't change history |
