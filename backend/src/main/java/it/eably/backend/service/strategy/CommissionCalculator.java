package it.eably.backend.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Context class for commission calculation using Strategy Pattern.
 *
 * DESIGN PATTERN: STRATEGY (Context)
 *
 * RESPONSIBILITY:
 * - Delegates commission calculation to a given CommissionStrategy
 * - Stateless: strategy is passed per-call, avoiding concurrency issues
 *
 * PATTERN BENEFITS DEMONSTRATED:
 * 1. FLEXIBILITY: Different strategies can be passed without changing this class
 * 2. TESTABILITY: Easy to test with mock strategies
 * 3. EXTENSIBILITY: New strategies can be added without changing existing code
 * 4. THREAD SAFETY: No mutable strategy field — safe for singleton use
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Component
public class CommissionCalculator {

    private static final Logger log = LoggerFactory.getLogger(CommissionCalculator.class);

    /**
     * Calculates commission using the provided strategy.
     *
     * @param totalAmount the total transaction amount
     * @param strategy    the commission strategy to apply
     * @return the calculated commission
     */
    public BigDecimal calculateWith(BigDecimal totalAmount, CommissionStrategy strategy) {
        log.debug("Calculating commission for amount {} using strategy: {}",
                totalAmount, strategy.getStrategyName());

        BigDecimal commission = strategy.calculateCommission(totalAmount);

        log.debug("Calculated commission: {}", commission);
        return commission;
    }
}
