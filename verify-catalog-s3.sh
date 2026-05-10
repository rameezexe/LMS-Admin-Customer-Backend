#!/bin/bash

# Fetch tokens
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
USER1_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"user1","password":"User1@123"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# Create dummy image
echo "dummy image content" > dummy.jpg
echo "dummy pdf content" > dummy.pdf
dd if=/dev/zero of=large.jpg bs=1M count=6 2>/dev/null

echo "=== 1. POST /api/admin/books/1/cover with ADMIN token + image file ==="
curl -s -X POST "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg" | grep -o '"success":true'

echo -e "\n=== 2. Verify coverImageUrl is valid ==="
curl -s "http://localhost:8082/api/user/books/1" -H "Authorization: Bearer $USER1_TOKEN" | grep -o '"coverImageUrl":"[^"]*'

echo -e "\n=== 3. POST /api/admin/books/1/cover again (overwrite) ==="
curl -s -X POST "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg" | grep -o '"success":true'

echo -e "\n=== 4. POST /api/admin/books/1/cover with a PDF file ==="
curl -s -X POST "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -F "file=@dummy.pdf;type=application/pdf" | grep -o '"success":false'

echo -e "\n=== 5. POST /api/admin/books/1/cover with file > 5MB ==="
curl -s -X POST "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -F "file=@large.jpg;type=image/jpeg" | grep -o '"success":false'

echo -e "\n=== 6. POST /api/admin/books/1/cover with USER token ==="
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $USER1_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg"

echo -e "\n=== 7. DELETE /api/admin/books/1/cover ==="
curl -s -X DELETE "http://localhost:8082/api/admin/books/1/cover" \
     -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"success":true'

# Cleanup
rm dummy.jpg dummy.pdf large.jpg
