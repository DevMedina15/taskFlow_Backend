package com.taskflow.api.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${api.security.jwt.secret-key}")
    private String secretKey;

    @Value("${api.security.jwt.expiration}")
    private long jwtExpiration;

    // 1. Extraemos el email (userName) del token
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. Generar un nuevo token a partir del usuario
    public String generateToken(UserDetails userDetails) {

        return Jwts.builder()
                .setSubject(userDetails.getUsername()) // se guarda el email
                .setIssuedAt(new Date(System.currentTimeMillis())) // Fecha de creacion
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Fecha de donde muere el token
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Firma
                .compact(); // Ensamblamiento  del string
    }

    // 3. Validamos si el token es legitimo y no ha expirado
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Traduce el texto secreto a una llave criptográfica real
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


}
