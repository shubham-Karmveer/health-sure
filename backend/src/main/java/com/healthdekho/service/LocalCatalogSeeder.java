package com.healthdekho.service;

import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.InsuranceProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.catalog.enabled", havingValue = "true")
public class LocalCatalogSeeder implements ApplicationRunner {

    private final InsuranceProductRepository productRepository;

    public LocalCatalogSeeder(InsuranceProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<InsuranceProduct> products = List.of(
                new InsuranceProduct(
                        "HD-TERM-001",
                        "HealthDekho Secure Term",
                        "TERM",
                        240,
                        "MONTHLY",
                        new BigDecimal("1250.00"),
                        new BigDecimal("5000000.00"),
                        18,
                        60,
                        true
                ),
                new InsuranceProduct(
                        "HD-HEALTH-001",
                        "HealthDekho Family Health",
                        "HEALTH",
                        12,
                        "YEARLY",
                        new BigDecimal("18000.00"),
                        new BigDecimal("1000000.00"),
                        18,
                        65,
                        true
                ),
                new InsuranceProduct(
                        "HD-SAV-001",
                        "HealthDekho Smart Savings",
                        "SAVINGS",
                        120,
                        "QUARTERLY",
                        new BigDecimal("7500.00"),
                        new BigDecimal("1500000.00"),
                        21,
                        55,
                        true
                ),
                new InsuranceProduct(
                        "HD-LEGACY-001",
                        "Legacy Travel Cover",
                        "TRAVEL",
                        12,
                        "YEARLY",
                        new BigDecimal("2000.00"),
                        new BigDecimal("100000.00"),
                        18,
                        70,
                        false
                )
        );
        for (InsuranceProduct product : products) {
            if (!productRepository.existsByProductCode(product.getProductCode())) {
                productRepository.save(product);
            }
        }
    }
}
