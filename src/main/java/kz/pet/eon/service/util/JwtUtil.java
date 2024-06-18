package kz.pet.eon.service.util;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${gateway.app.jwtSecret}")
    private String key;

    private Key signingKey;

    @PostConstruct
    public void init() {
        signingKey = new SecretKeySpec(
                DatatypeConverter.parseBase64Binary(key), SignatureAlgorithm.HS256.getJcaName()
        );
    }

    public Claims validateToken(final String token) {
        return Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token).getBody();
    }

    public List<String> getAuthorities(Claims jwt) {
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(gson.toJson(jwt.get("roles")), String[].class));
    }
}
