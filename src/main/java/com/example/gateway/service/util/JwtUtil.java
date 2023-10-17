package com.example.gateway.service.util;

import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import jakarta.xml.bind.DatatypeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtUtil {
    @Value("${gateway.app.jwtSecret}")
    private String key;

    private static final Key signingKey = new SecretKeySpec(
            DatatypeConverter.parseBase64Binary("413F4428472B4B6250655368566D5970337336763979244226452948404D6351"), SignatureAlgorithm.HS256.getJcaName()
    );

    public static Claims validateToken(final String token) {
        return Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token).getBody();
    }

    public static List<String> getAuthorities(Claims jwt) {
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(gson.toJson(jwt.get("roles")), String[].class));
    }
}
