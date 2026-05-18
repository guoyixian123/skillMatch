package com.skillmatch.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor("SkillMatchXiaoXinLoveJiaHui99!774499".getBytes(StandardCharsets.UTF_8));

    public static String createToken(String id, String name) {
        Map<String, String> claims = new HashMap<>();
        claims.put("userId", id);
        claims.put("name", name);
        String jwtBuilder = Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))//1天
                .signWith(SignatureAlgorithm.HS256, JWT_KEY)
                .compact();
        return jwtBuilder;
    }

    public static Map<String, String> parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(JWT_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Map<String, String> map = new HashMap<>();
        map.put("userId", claims.get("userId", String.class));
        map.put("name", claims.get("name", String.class));

        return map;
    }
}

