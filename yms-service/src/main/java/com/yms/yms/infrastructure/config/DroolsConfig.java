package com.yms.yms.infrastructure.config;

import org.kie.api.KieServices;
import org.kie.api.KieBase;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Configuration
public class DroolsConfig {
    @Bean
    public KieBase debtInterestKieBase() throws IOException {
        Resource rules = KieServices.Factory.get().getResources()
                .newClassPathResource("rules/debt-interest.drl", StandardCharsets.UTF_8.name());
        return new KieHelper().addResource(rules, ResourceType.DRL).build();
    }
}
