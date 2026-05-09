#!/bin/bash

# Tokens
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9BRE1JTiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzc4MzM5MTU0LCJleHAiOjE3Nzg0MjU1NTR9.P3BnCKZ4ZIQ7BGZLDdhLhdyCvSQlIvDZA3U95qp6qBU"
USER1_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoidXNlcjEiLCJtZW1iZXJJZCI6MSwiaWF0IjoxNzc4MzM5MTU0LCJleHAiOjE3Nzg0MjU1NTR9.u3S2T1zMhB3r-B4KkZ01zR5X6E-E8Xz596z3z4Y4T_k"

echo "=== 1. Admin gets all members ==="
curl -s "http://localhost:8083/api/admin/members" -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"success":true'

echo -e "\n=== 2. User gets all members (Should fail) ==="
curl -s -o /dev/null -w "%{http_code}" "http://localhost:8083/api/admin/members" -H "Authorization: Bearer $USER1_TOKEN"

echo -e "\n=== 3. User1 gets own profile ==="
curl -s "http://localhost:8083/api/user/profile/1" -H "Authorization: Bearer $USER1_TOKEN" | grep -o '"email":"user1@example.com"'

echo -e "\n=== 4. User1 tries to get User2 profile (Should fail) ==="
curl -s -o /dev/null -w "%{http_code}" "http://localhost:8083/api/user/profile/2" -H "Authorization: Bearer $USER1_TOKEN"

echo -e "\n=== 5. Admin gets User2 profile ==="
curl -s "http://localhost:8083/api/user/profile/2" -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"email":"user2@example.com"'

echo -e "\n=== 6. Admin gets stats ==="
curl -s "http://localhost:8083/api/admin/members/stats" -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"totalMembers":4'

echo -e "\n=== 7. Admin suspends User 1 ==="
curl -X PUT -s "http://localhost:8083/api/admin/members/1/deactivate" -H "Authorization: Bearer $ADMIN_TOKEN" | grep -o '"success":true'

echo -e "\n=== Verification Complete ==="
