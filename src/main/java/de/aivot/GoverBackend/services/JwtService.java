package de.aivot.GoverBackend.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.aivot.GoverBackend.models.JwtConfig;
import de.aivot.GoverBackend.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Date;

@Component
public class JwtService {
    private final JwtConfig jwtConfig;

    @Autowired
    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    private final String ISSUER = "gover";

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());

        Date issued = new Date(System.currentTimeMillis());
        Date expired = new Date(issued.getTime() + jwtConfig.getExpiration());

        return JWT
                .create()
                .withIssuer(ISSUER)
                .withClaim("id", user.getId())
                .withClaim("role", user.getId())
                .withIssuedAt(issued)
                .withExpiresAt(expired)
                .sign(algorithm);
    }

    @Nullable
    public Long validateToken(String token) {
        if (token == null || token.equals("null")) {
            return null;
        }

        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());

        JWTVerifier verifier = JWT
                .require(algorithm)
                .withIssuer(ISSUER)
                .build();

        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (Exception e) {
            return null;
        }

        Claim idClaim = jwt.getClaim("id");
        if (idClaim != null && !idClaim.isNull()) {
            return idClaim.asLong();
        }

        return null;
    }
}
