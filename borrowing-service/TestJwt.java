import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class TestJwt {
    public static void main(String[] args) {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoidXNlcjEiLCJtZW1iZXJJZCI6MSwiaWF0IjoxNzc4MzM5MTU0LCJleHAiOjE3Nzg0MjU1NTR9.u3S2T1zMhB3r-B4KkZ01zR5X6E-E8Xz596z3z4Y4T_k";
        String secretKey = "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JKV1RBdXRoMjU2Qml0cw==";
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Current Time: " + new Date());
            System.out.println("Is Expired: " + claims.getExpiration().before(new Date()));
            System.out.println("MemberId: " + claims.get("memberId"));
            System.out.println("MemberId Class: " + claims.get("memberId").getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
