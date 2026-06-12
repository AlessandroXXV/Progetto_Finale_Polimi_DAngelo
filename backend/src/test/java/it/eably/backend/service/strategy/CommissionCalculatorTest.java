package it.eably.backend.service.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CommissionCalculatorTest {

    private CommissionCalculator calculator;
    private StandardCommissionStrategy standardStrategy;
    private PremiumCommissionStrategy premiumStrategy;

    @BeforeEach
    void setUp() {
        calculator = new CommissionCalculator();
        standardStrategy = new StandardCommissionStrategy();
        premiumStrategy = new PremiumCommissionStrategy();
    }

    // ========== CALCULATE WITH ==========

    @Test
    void calculateWith_StandardStrategy_ComputesCorrectly() {
        // Standard: 10% + €0.50 → on €50.00: 5.50
        BigDecimal commission = calculator.calculateWith(new BigDecimal("50.00"), standardStrategy);
        assertEquals(new BigDecimal("5.50"), commission);
    }

    @Test
    void calculateWith_PremiumStrategy_ComputesCorrectly() {
        // Premium: 5% → on €50.00: 2.50
        BigDecimal commission = calculator.calculateWith(new BigDecimal("50.00"), premiumStrategy);
        assertEquals(new BigDecimal("2.50"), commission);
    }

    @Test
    void calculateWith_StandardOnLargeAmount() {
        // Standard: 10% + €0.50 → on €100.00: 10.50
        BigDecimal commission = calculator.calculateWith(new BigDecimal("100.00"), standardStrategy);
        assertEquals(new BigDecimal("10.50"), commission);
    }

    @Test
    void calculateWith_PremiumOnSmallAmount() {
        // Premium: 5% → on €20.00: 1.00
        BigDecimal commission = calculator.calculateWith(new BigDecimal("20.00"), premiumStrategy);
        assertEquals(new BigDecimal("1.00"), commission);
    }

    // ========== STANDARD STRATEGY EDGE CASES ==========

    @Test
    void standardStrategy_ZeroAmount_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, standardStrategy.calculateCommission(BigDecimal.ZERO));
    }

    @Test
    void standardStrategy_NullAmount_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, standardStrategy.calculateCommission(null));
    }

    // ========== PREMIUM STRATEGY EDGE CASES ==========

    @Test
    void premiumStrategy_ZeroAmount_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, premiumStrategy.calculateCommission(BigDecimal.ZERO));
    }

    @Test
    void premiumStrategy_NullAmount_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, premiumStrategy.calculateCommission(null));
    }

    // ========== STRATEGY NAMES ==========

    @Test
    void standardStrategy_GetStrategyName_ReturnsName() {
        assertNotNull(standardStrategy.getStrategyName());
        assertFalse(standardStrategy.getStrategyName().isEmpty());
    }

    @Test
    void premiumStrategy_GetStrategyName_ReturnsName() {
        assertNotNull(premiumStrategy.getStrategyName());
        assertFalse(premiumStrategy.getStrategyName().isEmpty());
    }
}
