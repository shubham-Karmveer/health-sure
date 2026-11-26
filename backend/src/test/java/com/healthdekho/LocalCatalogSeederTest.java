package com.healthdekho;

import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.InsuranceProductRepository;
import com.healthdekho.service.LocalCatalogSeeder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalCatalogSeederTest {

    @Test
    void seedsSourceCatalogValuesAndKeepsInactiveExample() {
        InsuranceProductRepository productRepository = mock(InsuranceProductRepository.class);
        when(productRepository.existsByProductCode(anyString())).thenReturn(false);
        when(productRepository.save(any(InsuranceProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LocalCatalogSeeder seeder = new LocalCatalogSeeder(productRepository);

        seeder.run(null);

        org.mockito.ArgumentCaptor<InsuranceProduct> productCaptor =
                org.mockito.ArgumentCaptor.forClass(InsuranceProduct.class);
        org.mockito.Mockito.verify(productRepository, org.mockito.Mockito.times(4))
                .save(productCaptor.capture());
        Map<String, InsuranceProduct> products = productCaptor.getAllValues().stream()
                .collect(Collectors.toMap(InsuranceProduct::getProductCode, Function.identity()));

        assertSourceProduct(
                products.get("HD-TERM-001"),
                "HealthDekho Secure Term",
                "TERM",
                240,
                "MONTHLY",
                "1250.00",
                "5000000.00",
                18,
                60
        );
        assertSourceProduct(
                products.get("HD-HEALTH-001"),
                "HealthDekho Family Health",
                "HEALTH",
                12,
                "YEARLY",
                "18000.00",
                "1000000.00",
                18,
                65
        );
        assertSourceProduct(
                products.get("HD-SAV-001"),
                "HealthDekho Smart Savings",
                "SAVINGS",
                120,
                "QUARTERLY",
                "7500.00",
                "1500000.00",
                21,
                55
        );
        assertThat(products.get("HD-LEGACY-001").isActive()).isFalse();
    }

    private void assertSourceProduct(
            InsuranceProduct product,
            String name,
            String type,
            int termMonths,
            String frequency,
            String premium,
            String sumInsured,
            int minAge,
            int maxAge
    ) {
        assertThat(product.getPolicyName()).isEqualTo(name);
        assertThat(product.getPolicyType()).isEqualTo(type);
        assertThat(product.getTermMonths()).isEqualTo(termMonths);
        assertThat(product.getPremiumFrequency()).isEqualTo(frequency);
        assertThat(product.getPremiumAmount()).isEqualByComparingTo(new BigDecimal(premium));
        assertThat(product.getSumInsured()).isEqualByComparingTo(new BigDecimal(sumInsured));
        assertThat(product.getMinEntryAge()).isEqualTo(minAge);
        assertThat(product.getMaxEntryAge()).isEqualTo(maxAge);
        assertThat(product.isActive()).isTrue();
    }
}
