package br.com.pulseapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.pulseapi.entities.PlanoEntity;
import br.com.pulseapi.repository.PlanoRepository;

@Service
public class PlanoService {

    @Autowired
    private PlanoRepository planRepository;

    public PlanoEntity findByName(String name) {
        return planRepository.findByName(name);
    }

    public int getEndpointLimit(String planName) {
        PlanoEntity plan = planRepository.findByName(planName);
        if (plan == null) {
            throw new RuntimeException("Plano não encontrado: " + planName);
        }
        return plan.getEndpointLimit();
    }

    public void initializePlans() {
        if (planRepository.count() == 0) {
            PlanoEntity starter = new PlanoEntity();
            starter.setName("Pulse Starter");
            starter.setEndpointLimit(3);
            starter.setPrice("Grátis");
            planRepository.save(starter);

            PlanoEntity pro = new PlanoEntity();
            pro.setName("Pulse Pro");
            pro.setEndpointLimit(Integer.MAX_VALUE); // Ilimitado
            pro.setPrice("$9,99/mês");
            planRepository.save(pro);
        }
    }
}
