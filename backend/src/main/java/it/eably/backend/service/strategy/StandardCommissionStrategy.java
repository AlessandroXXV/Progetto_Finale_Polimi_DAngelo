package it.eably.backend.service.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Standard commission strategy for regular users.
 * 
 * DESIGN PATTERN: STRATEGY (Concrete Strategy)
 * 
 * COMMISSION MODEL:
 * - Percentage: 10% of transaction amount
 * - Fixed fee: €0.50 per transaction
 * - Formula: (totalAmount * 0.10) + 0.50
 * 
 * USE CASE:
 * - Applied to regular (non-premium) users
 * - Default commission model for the platform
 * - Covers platform operational costs and payment processing fees
 * 
 * EXAMPLE:
 * - Transaction: €20.00
 * - Commission: (€20.00 * 0.10) + €0.50 = €2.50
 * - Provider receives: €17.50
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class StandardCommissionStrategy implements CommissionStrategy {
    
    private static final BigDecimal PERCENTAGE_RATE = new BigDecimal("0.10"); // 10%
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.50"); // €0.50
    
    /**
     * Calculates commission using standard model: 10% + €0.50.
     * 
     * @param totalAmount the total transaction amount
     * @return the commission amount
     */
    @Override
    public BigDecimal calculateCommission(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate percentage commission
        BigDecimal percentageCommission = totalAmount
                .multiply(PERCENTAGE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Add fixed fee
        BigDecimal totalCommission = percentageCommission
                .add(FIXED_FEE)
                .setScale(2, RoundingMode.HALF_UP);
        
        return totalCommission;
    }
    
    @Override
    public String getStrategyName() {
        return "Standard Commission (10% + €0.50)";
    }
}
