package wenhao.demo.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import wenhao.demo.service.RSA256Key;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {
    private String SECRET_KEY = "secret";

    public static final String KEY_ALGORITHM = "RSA";
    //RSA密钥长度
    public static final int KEY_SIZE = 1024;
    //唯一的密钥实例
    private static volatile RSA256Key rsa256Key;
    public static RSAPublicKey publicKey=null;
    public static RSAPrivateKey privateKey=null;



    public static RSA256Key generateRSA256Key() throws NoSuchAlgorithmException {

        if (rsa256Key == null) {
            //RSA256Key单例的双重校验锁
            synchronized(RSA256Key.class) {
                //第二次校验：防止锁竞争中自旋的线程，拿到系统资源时，重复创建实例
                if (rsa256Key == null) {
                    //密钥生成所需的随机数源
                    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
                    keyPairGen.initialize(KEY_SIZE);
                    //通过KeyPairGenerator生成密匙对KeyPair
                    KeyPair keyPair = keyPairGen.generateKeyPair();
                    //获取公钥和私钥
                     publicKey = (RSAPublicKey) keyPair.getPublic();
                     privateKey = (RSAPrivateKey) keyPair.getPrivate();
                    rsa256Key = new RSA256Key(publicKey,privateKey);
                }

            }
        }
        return rsa256Key;
    }

    public String extractUsername(String accessToken) {


        return extractClaim(accessToken, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {

        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        try {
            if (StringUtils.isEmpty(token)) {
                return null;
            }
            Jws<Claims> jws = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
            Claims claims = jws.getBody();
            return claims;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        //return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }



    public String generateToken(UserDetails userDetails) throws NoSuchAlgorithmException {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) throws NoSuchAlgorithmException {
        RSA256Key rsa256Key = generateRSA256Key();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.RS256,rsa256Key.getRSAPrivateKey() )
                .compact();

        return accessToken;


    }
    public static boolean TokenValidate(String accessToken) {
        return validatorToken( publicKey,  accessToken);
    }
    public static boolean validatorToken(RSAPublicKey publicKey, String accessToken) {
        try {
            if (StringUtils.isEmpty(accessToken)) {
                return false;
            }
            Jwts.parser().setSigningKey(publicKey).parseClaimsJws(accessToken);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        //return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token)&&validatorToken(publicKey,token));
    }
}

