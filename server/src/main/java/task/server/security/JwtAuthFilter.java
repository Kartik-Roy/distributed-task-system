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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null && path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        String token = auth.substring(7);

        try {
            io.jsonwebtoken.Claims claims = jwtService.parseClaims(token);

            String typ = claims.get("typ", String.class);
            String subject = claims.getSubject();

            if (typ == null || subject == null || subject.isBlank()) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if ("node".equals(typ)) {
                SecurityContextHolder.getContext().setAuthentication(new NodeAuthentication(subject));
                chain.doFilter(req, res);
                return;
            }

            if (typ.contains("user") || typ.contains("admin")) {
                Object role = claims.get("role", Object.class);
                if (role == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                var authToken = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
                chain.doFilter(req, res);
                return;
            }

            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception ex) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}

