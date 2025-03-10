package br.com.pulseapi.config;

import java.io.IOException;
import java.security.Key;

import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final Key secretKey;

    public JwtAuthorizationFilter(Key secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        System.out.println("Header Authorization no filtro de autorização: " + header);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.replace("Bearer ", "");
            try {
                Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token);
                System.out.println("Token validado com sucesso no JwtAuthorizationFilter");
            } catch (Exception e) {
                System.out.println("Erro de autorização: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado: Token inválido");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}