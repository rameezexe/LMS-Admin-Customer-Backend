#!/bin/bash
echo "=== 1. Admin Login ==="
ADMIN_RESP=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123"}')
echo "$ADMIN_RESP" | python3 -m json.tool
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
REFRESH_TOKEN=$(echo "$ADMIN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['refreshToken'])")

echo ""
echo "=== 2. Wrong Password ==="
curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"wrong"}' | python3 -m json.tool

echo ""
echo "=== 3. Refresh Token ==="
curl -s -X POST http://localhost:8081/api/auth/refresh -H "Content-Type: application/json" -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}" | python3 -m json.tool

echo ""
echo "=== 4. Admin GET /users (should succeed) ==="
curl -s http://localhost:8081/api/admin/auth/users -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

echo ""
echo "=== 5. User1 Login ==="
USER_RESP=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"user1","password":"User1@123"}')
echo "$USER_RESP" | python3 -m json.tool
USER_TOKEN=$(echo "$USER_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo ""
echo "=== 6. User GET /admin/auth/users (should 403) ==="
curl -s -w "\nHTTP_CODE:%{http_code}\n" http://localhost:8081/api/admin/auth/users -H "Authorization: Bearer $USER_TOKEN"

echo ""
echo "=== 7. Gateway: Admin GET /admin/auth/users (should succeed) ==="
curl -s http://localhost:8080/api/admin/auth/users -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

echo ""
echo "=== 8. Gateway: User GET /admin/auth/users (should 403) ==="
curl -s -w "\nHTTP_CODE:%{http_code}\n" http://localhost:8080/api/admin/auth/users -H "Authorization: Bearer $USER_TOKEN"

echo ""
echo "=== 9. Logout ==="
curl -s -X POST http://localhost:8081/api/auth/logout -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool

echo ""
echo "=== ALL TESTS COMPLETE ==="
