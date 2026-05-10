#!/bin/bash

echo "Starting verification for member-service S3..."

# NOTE: The tokens won't work unless auth-service is running.
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin@123"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
USER1_TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"user1","password":"User1@123"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "dummy image content" > dummy.jpg
dd if=/dev/zero of=large.jpg bs=1M count=6 2>/dev/null

echo "=== 1. POST /api/user/profile/1/photo with user1 token + image ==="
curl -s -X POST "http://localhost:8083/api/user/profile/1/photo" \
     -H "Authorization: Bearer $USER1_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg" | grep -o '"success":true'

echo -e "\n=== 2. POST /api/user/profile/2/photo with user1 token ==="
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8083/api/user/profile/2/photo" \
     -H "Authorization: Bearer $USER1_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg"

echo -e "\n=== 3. POST /api/admin/members/2/photo with ADMIN token ==="
curl -s -X POST "http://localhost:8083/api/admin/members/2/photo" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -F "file=@dummy.jpg;type=image/jpeg" | grep -o '"success":true'

echo -e "\n=== 4. GET /api/user/profile/1 ==="
curl -s "http://localhost:8083/api/user/profile/1" -H "Authorization: Bearer $USER1_TOKEN" | grep -o '"profilePhotoUrl":"[^"]*'

echo -e "\n=== 5. DELETE /api/user/profile/1/photo with user1 token ==="
curl -s -X DELETE "http://localhost:8083/api/user/profile/1/photo" \
     -H "Authorization: Bearer $USER1_TOKEN" | grep -o '"success":true'

echo -e "\n=== 6. Upload > 5MB ==="
curl -s -X POST "http://localhost:8083/api/user/profile/1/photo" \
     -H "Authorization: Bearer $USER1_TOKEN" \
     -F "file=@large.jpg;type=image/jpeg" | grep -o '"success":false'

rm dummy.jpg large.jpg
