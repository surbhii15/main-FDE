"""End-to-end smoke test of the running API: python scripts/smoke_test.py [base_url]

Walks the customer journey (browse, cart, address, checkout with points, declined + successful
payment, history, recommendations, buy again, cancel) plus admin CRUD and security checks.
Safe to re-run: it cancels the order it places and soft-deletes the product it creates."""
import json, sys, urllib.request, urllib.error, uuid
BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
SKU = "BK-SMOKE-" + uuid.uuid4().hex[:6].upper()
def call(method, path, body=None, token=None):
    req=urllib.request.Request(BASE+path, method=method, data=json.dumps(body).encode() if body is not None else None)
    req.add_header("Content-Type","application/json")
    if token: req.add_header("Authorization","Bearer "+token)
    try:
        with urllib.request.urlopen(req) as r: return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read() or b"null")
def show(label, res, keys=None):
    s,b=res
    out = {k:b.get(k) for k in keys} if keys and isinstance(b,dict) else b
    print(f"{s} {label}: {json.dumps(out, default=str)[:300]}")
    return b
show("categories", call("GET","/api/categories"))
p=show("search q=austen sort=PRICE_ASC", call("GET","/api/products?q=austen&sort=PRICE_ASC"), ["totalElements"])
d=show("product 1 detail", call("GET","/api/products/1"))
print("   related:", [r["title"] for r in d["relatedProducts"]], "eta:", d["product"]["estimatedDeliveryDate"])
show("cart w/o token", call("GET","/api/cart"))
a=show("login reader", call("POST","/api/auth/login",{"email":"reader@bookstore.local","password":"Reader@123"}), ["tokenType","expiresInSeconds"])
t=a["accessToken"]
show("bad login", call("POST","/api/auth/login",{"email":"reader@bookstore.local","password":"nope"}))
show("recommendations (no history)", call("GET","/api/recommendations?limit=3",token=t))
show("add 2x product 1", call("POST","/api/cart/items",{"productId":1,"quantity":2},t), ["itemCount","subtotal"])
show("add product 10", call("POST","/api/cart/items",{"productId":10,"quantity":1},t), ["itemCount","subtotal","estimatedDeliveryDate"])
show("add out-of-stock Peter Pan (20)", call("POST","/api/cart/items",{"productId":20,"quantity":1},t))
show("validation error", call("POST","/api/cart/items",{"productId":1,"quantity":0},t))
addr=show("add address", call("POST","/api/addresses",{"label":"Home","recipientName":"Demo Reader","line1":"12 MG Road","city":"Bengaluru","state":"KA","postalCode":"560001","country":"India","phone":"+91 9800000000"},t), ["id","isDefault"])
show("preview 150 pts", call("GET","/api/checkout/preview?pointsToRedeem=150",token=t), ["subtotal","giftPointBalance","maxRedeemablePoints","discount","total","pointsToEarn"])
show("preview too many pts", call("GET","/api/checkout/preview?pointsToRedeem=500",token=t))
o=show("checkout 150 pts", call("POST","/api/orders",{"addressId":addr["id"],"pointsToRedeem":150},t), ["orderNumber","status","subtotal","discount","total","estimatedDeliveryDate"])
show("cart after checkout", call("GET","/api/cart",token=t), ["itemCount"])
card={"number":"4000 0000 0000 0002","holderName":"Demo Reader","expiryMonth":12,"expiryYear":2030,"cvv":"123"}
f=show("pay declined card", call("POST",f"/api/orders/{o['id']}/payments",{"method":"CREDIT_CARD","card":card},t), ["message"])
card["number"]="4111 1111 1111 1111"
r=show("pay good card", call("POST",f"/api/orders/{o['id']}/payments",{"method":"CREDIT_CARD","card":card},t), ["message"])
print("   payment:", {k:r["payment"][k] for k in ("status","cardBrand","cardLast4","amount")}, "order:", r["order"]["status"])
show("pay again", call("POST",f"/api/orders/{o['id']}/payments",{"method":"UPI","upiId":"me@okbank"},t))
show("points", call("GET","/api/users/me/gift-points",token=t), ["balance"])
show("history", call("GET","/api/orders",token=t), ["totalElements","content"])
show("recommendations (with history)", call("GET","/api/recommendations?limit=3",token=t))
b=show("buy again", call("POST",f"/api/orders/{o['id']}/buy-again",token=t), ["added","skipped"])
c=show("cancel", call("POST",f"/api/orders/{o['id']}/cancel",token=t), ["status","message"])
show("cancel twice", call("POST",f"/api/orders/{o['id']}/cancel",token=t))
show("points after cancel", call("GET","/api/users/me/gift-points",token=t), ["balance"])
show("product 1 stock after cancel", call("GET","/api/products/1"))
adm=call("POST","/api/auth/login",{"email":"admin@bookstore.local","password":"Admin@123"})[1]["accessToken"]
show("customer -> admin endpoint", call("DELETE","/api/admin/products/5",token=t))
np=show("admin create product", call("POST","/api/admin/products",{"sku":SKU,"title":"Test Book","author":"Q. Tester","price":100,"stockQuantity":5,"deliveryDays":2,"categoryId":1,"brandId":1},adm), ["id","title","estimatedDeliveryDate"])
show("admin update product", call("PUT",f"/api/admin/products/{np['id']}",{"sku":SKU,"title":"Test Book 2e","author":"Q. Tester","price":120,"stockQuantity":5,"deliveryDays":2,"categoryId":1,"brandId":1},adm), ["title","price"])
show("admin delete product", call("DELETE",f"/api/admin/products/{np['id']}",token=adm))
show("deleted product hidden", call("GET",f"/api/products/{np['id']}"))
show("openapi", call("GET","/v3/api-docs"), ["openapi"])
