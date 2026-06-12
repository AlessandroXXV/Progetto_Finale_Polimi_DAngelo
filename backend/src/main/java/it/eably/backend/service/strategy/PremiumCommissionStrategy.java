package it.eably.backend.service.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Premium commission strategy for premium users or large transactions.
 * 
 * DESIGN PATTERN: STRATEGY (Concrete Strategy)
 * 
 * COMMISSION MODEL:
 * - Percentage: 5% of transaction amount
 * - Fixed fee: €0.00 (no fixed fee)
 * - Formula: totalAmount * 0.05
 * 
 * USE CASE:
 * - Applied to premium/verified users
 * - Applied to high-value transactions (e.g., > €100)
 * - Incentivizes platform loyalty and larger bookings
 * 
 * EXAMPLE:
 * - Transaction: €100.00
 * - Commission: €100.00 * 0.05 = €5.00
 * - Provider receives: €95.00
 * 
 * COMPARISON WITH STANDARD:
 * - Standard on €100: (€100 * 0.10) + €0.50 = €10.50
 * - Premium on €100: €100 * 0.05 = €5.00
 * - Savings: €5.50 (52% reduction)
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class PremiumCommissionStrategy implements CommissionStrategy {
    
    private static final BigDecimal PERCENTAGE_RATE = new BigDecimal("0.05"); // 5%

    /**
     * Calculates commission using premium model: 5% only.
     *
     * @param totalAmount the total transaction amount
     * @return the commission amount
     */
    @Override
    public BigDecimal calculateCommission(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate percentage commission (no fixed fee)
        BigDecimal commission = totalAmount
                .multiply(PERCENTAGE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        
        return commission;
    }
    
    @Override
    public String getStrategyName() {
        return "Premium Commission (5% only)";
    }
}
