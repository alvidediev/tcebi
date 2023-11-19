package com.twoa.tcebi.securiry;

import com.twoa.tcebi.domain.entity.UserEntity;
import com.twoa.tcebi.securiry.password_encoder.PBKDF2PasswordEncoder;
import com.twoa.tcebi.securiry.token.TokenDetails;
import com.twoa.tcebi.service.user_registration.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SecurityService {
    private final UserService userService;
    private final PBKDF2PasswordEncoder passwordEncoder;

    @Value("${jwt.token.secret}")
    private String secret;
    @Value("${jwt.token.expiration}")
    private Integer expirationInSeconds;
    @Value("${jwt.token.issuer}")
    private String issuer;


    private TokenDetails generateToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>() {{
            put("role", user.getUserRole());
            put("username", user.getEmail());
        }};
        return generateToken(claims, user.getId().toString());
    }

    private TokenDetails generateToken(Map<String, Object> claims, String subject) {
        Long expirationTimeInMillis = expirationInSeconds * 1000L;
        Date expirationDate = new Date(new Date().getTime() + expirationTimeInMillis);

        return generateToken(expirationDate, claims, subject);
    }

    private TokenDetails generateToken(Date expirationDate, Map<String, Object> claims, String subject) {
        Date createdDate = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(issuer)
                .setSubject(subject)
                .setIssuedAt(createdDate)
                .setId(UUID.randomUUID().toString())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder().encodeToString(secret.getBytes()))
                .compact();

        return TokenDetails.builder()
                .token(token)
                .issuedAt(createdDate)
                .expiresAt(expirationDate)
                .build();
    }

    public Mono<TokenDetails> authenticate(String username, String password) {
        return userService.findByEmail(username)
                .flatMap(user -> {
//                    if (!user.isEnabled()) {
//                        return Mono.error(new AuthException("Account disabled", "PROSELYTE_USER_ACCOUNT_DISABLED"));
//                    }

//                    if (!passwordEncoder.matches(password, user.getPassword())) {
//                        return Mono.error(new AuthException("Invalid password", "PROSELYTE_INVALID_PASSWORD"));
//                    }

                    return Mono.just(generateToken(user).toBuilder()
                            .userId(user.getId())
                            .build());
                });
               // .switchIfEmpty(Mono.error(new AuthException("Invalid username", "PROSELYTE_INVALID_USERNAME")));
    }
}
