# API One Pager — inventory-reservation

Base URL (local): `http://localhost:8080`  
Related story: [SCRUM-5](https://pkvanga1508.atlassian.net/browse/SCRUM-5)  
ADR: [ADR-001 — In-memory per-product locking with repository boundary](https://pkvanga1508.atlassian.net/wiki/spaces/SD/pages/622593)

---

## Endpoints

### Products

#### `GET /api/v1/products`

List all products and available quantities.

**Response `200`**
```json
[
  { "productId": "SKU-100", "name": "Wireless Headphones", "availableQuantity": 50 },
  { "productId": "SKU-200", "name": "USB-C Cable", "availableQuantity": 100 }
]
```

#### `GET /api/v1/products/{productId}`

**Response `200`**
```json
{ "productId": "SKU-100", "name": "Wireless Headphones", "availableQuantity": 48 }
```

**Errors:** `404` `PRODUCT_NOT_FOUND`

#### `POST /api/v1/products`

Seed or overwrite a product (MVP helper; no auth).

**Request**
```json
{
  "productId": "SKU-300",
  "name": "Phone Case",
  "availableQuantity": 25
}
```

| Field | Rules |
|-------|--------|
| `productId` | required, non-blank |
| `name` | required, non-blank |
| `availableQuantity` | required, `>= 0` |

**Response `201`** — same shape as product stock response.

**Errors:** `400` `VALIDATION_ERROR`

---

### Reservations

#### `POST /api/v1/reservations`

Hold inventory for checkout.

**Request**
```json
{ "productId": "SKU-100", "quantity": 2 }
```

| Field | Rules |
|-------|--------|
| `productId` | required, non-blank |
| `quantity` | required, `>= 1` |

**Response `201`**
```json
{
  "id": "a1b2c3d4-...",
  "productId": "SKU-100",
  "quantity": 2,
  "status": "ACTIVE",
  "createdAt": "2026-07-28T18:00:00Z",
  "expiresAt": "2026-07-28T18:10:00Z"
}
```

Decrements `availableQuantity` by `quantity`. TTL defaults to `reservation.ttl-seconds` (600).

**Errors:** `400` validation · `404` `PRODUCT_NOT_FOUND` · `409` `INSUFFICIENT_STOCK`

#### `POST /api/v1/reservations/{id}/confirm`

Permanently consume reserved units (status → `CONFIRMED`). Does **not** restore stock.

**Response `200`** — `ReservationResponse` with `status: "CONFIRMED"`

**Errors:** `404` `RESERVATION_NOT_FOUND` · `409` `INVALID_RESERVATION_STATE`

#### `POST /api/v1/reservations/{id}/cancel`

Release held units back to available stock (status → `CANCELLED`).

**Response `200`** — `ReservationResponse` with `status: "CANCELLED"`

**Errors:** `404` `RESERVATION_NOT_FOUND` · `409` `INVALID_RESERVATION_STATE`

---

## Reservation statuses

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Hold in effect until confirm, cancel, or expiry |
| `CONFIRMED` | Purchased; stock permanently consumed |
| `CANCELLED` | Released by client |
| `EXPIRED` | Released by scheduled sweep after `expiresAt` |

---

## Error envelope

All mapped errors use:

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Human-readable explanation",
  "details": null
}
```

Validation failures include field details:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": { "quantity": "quantity must be at least 1" }
}
```

| HTTP | Code | When |
|------|------|------|
| 400 | `VALIDATION_ERROR` | Bean Validation failed |
| 400 | `BAD_REQUEST` | Illegal argument |
| 404 | `PRODUCT_NOT_FOUND` | Unknown product id |
| 404 | `RESERVATION_NOT_FOUND` | Unknown reservation id |
| 409 | `INSUFFICIENT_STOCK` | Not enough available units |
| 409 | `INVALID_RESERVATION_STATE` | Confirm/cancel on non-ACTIVE reservation |

---

## Behaviour notes (from code)

- Thread safety: per-product `ReentrantLock` in `ReservationService`
- Expiry: `@Scheduled` sweep every `reservation.expiry-sweep-ms` releases `ACTIVE` reservations past `expiresAt`
- Persistence: repository interfaces + in-memory implementations only (see ADR-001)
- Auth: none (MVP)
