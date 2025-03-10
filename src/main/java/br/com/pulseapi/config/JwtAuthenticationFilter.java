package br.com.pulseapi.config;

import java.io.IOException;
import java.security.Key;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Key secretKey;

    public JwtAuthenticationFilter(Key secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        System.out.println("Header Authorization recebido: " + header);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.replace("Bearer ", "");
            System.out.println("Token a ser validado: " + token);
            try {
                String email = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .get("email", String.class);
                System.out.println("Email extraído do claim: " + email);

                if (email != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, null);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("Usuário autenticado: " + email);
                }
            } catch (Exception e) {
                System.out.println("Erro ao validar token: " + e.getMessage());
                System.out.println("Detalhes do erro: " + e.getCause());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
                return;
            }
        } else {
            System.out.println("Nenhum header Authorization encontrado ou formato inválido.");
        }

        chain.doFilter(request, response);
    }
}
