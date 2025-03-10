package br.com.pulseapi.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.param.ChargeCreateParams;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String apiKey;

    public String createCustomerAndCharge(String email, String token, double amountInCents) throws StripeException {
        Stripe.apiKey = apiKey;

        // Criar um cliente no Stripe
        Customer customer = Customer.create(Map.of(
            "email", email
        ));

        // Criar uma cobrança
        ChargeCreateParams params = ChargeCreateParams.builder()
            .setAmount((long) amountInCents) // Ex.: 999 para $9.99
            .setCurrency("usd")
            .setSource(token)
            .setCustomer(customer.getId())
            .setDescription("Upgrade para Pulse Pro")
            .build();

        Charge charge = Charge.create(params);
        return charge.getId();
    }
}
