package br.com.pulseapi.controller;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.pulseapi.entities.UserEntity;
import br.com.pulseapi.service.PaymentService;
import br.com.pulseapi.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final PaymentService paymentService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Key secretKey;

    @Autowired
    public UserController(
            UserService userService,
            PaymentService paymentService,
            BCryptPasswordEncoder passwordEncoder,
            @Autowired Key secretKey) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.passwordEncoder = passwordEncoder;
        this.secretKey = secretKey;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        UserEntity user = userService.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email ou senha inválidos"));
        }

        String token = Jwts.builder()
            .setSubject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("plan", user.getPlan())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
            .signWith(secretKey, SignatureAlgorithm.HS512)
            .compact();

        System.out.println("Token gerado: " + token);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("plan", user.getPlan());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<String> upgradeToPro(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
        try {
            String jwt = token.replace("Bearer ", "");
            Long userId = Long.parseLong(Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody()
                .getSubject());
            String paymentToken = payload.get("paymentToken");

            paymentService.createCustomerAndCharge(userService.findById(userId).getEmail(), paymentToken, 999);
            userService.updatePlan(userId, "Pulse Pro");
            return ResponseEntity.ok("Plano atualizado para Pulse Pro");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody Map<String, String> signupRequest) {
        String email = signupRequest.get("email");
        String password = signupRequest.get("password");

        if (userService.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email já cadastrado."));
        }

        UserEntity user = new UserEntity(email, password);
        userService.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuário registrado com sucesso.");
        return ResponseEntity.ok(response);
    }
}
