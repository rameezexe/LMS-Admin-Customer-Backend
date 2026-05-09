#!/bin/bash
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9BRE1JTiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzc4MzM5MTU0LCJleHAiOjE3Nzg0MjU1NTR9.P3BnCKZ4ZIQ7BGZLDdhLhdyCvSQlIvDZA3U95qp6qBU"

echo "=== 1. GET /api/user/books without token (Expect 403 or 401) ==="
curl -s -w "\nHTTP_CODE:%{http_code}\n" http://localhost:8082/api/user/books

echo "=== 2. POST /api/admin/books (Create a book) ==="
curl -s -X POST http://localhost:8082/api/admin/books \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Test Book",
    "author": "Test Author",
    "isbn": "123-456-789",
    "category": "Test",
    "description": "A test book",
    "status": "ACTIVE"
  }'

echo -e "\n=== 3. DELETE /api/admin/books/1 (Expect success since no active borrows) ==="
curl -s -X DELETE http://localhost:8082/api/admin/books/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

echo -e "\n=== 4. GET /api/user/books/available ==="
curl -s http://localhost:8082/api/user/books/available \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool | head -25
