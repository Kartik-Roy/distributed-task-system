package task.server.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Skip login endpoints entirely
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // If already authenticated, don't overwrite (important when you add more auth mechanisms later)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
//        if (auth == null || !auth.startsWith("Bearer ")) {
//            chain.doFilter(req, res);
//            return;
//        }

        String token = auth.substring(7);

        try {
            // MUST validate signature + exp + issuer inside this method.
            io.jsonwebtoken.Claims claims = jwtService.parseClaims(token);

            String typ = claims.get("typ", String.class); // "node" or "user"
            String subject = claims.getSubject();         // nodeId or username

            if (typ == null || subject == null || subject.isBlank()) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if ("node".equals(typ)) {
                // Node identity = JWT subject
                SecurityContextHolder.getContext().setAuthentication(new NodeAuthentication(subject));
                chain.doFilter(req, res);
                return;
            }

            if (typ.contains("user") || typ.contains("admin")) {
                // User role is required to authorize /admin/** etc.
                Object role = claims.get("role", Object.class); // e.g. "ADMIN"
                if (role == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                var authToken = new UsernamePasswordAuthenticationToken(
                        subject, // principal (username or userId)
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                chain.doFilter(req, res);
                return;
            }

            // Unknown token type
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception ex) {
            // invalid/expired token
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}

