package it.eably.backend.service.strategy;

import java.math.BigDecimal;

/**
 * Strategy interface for commission calculation algorithms.
 * 
 * DESIGN PATTERN: STRATEGY (GoF Behavioral Pattern)
 * 
 * PURPOSE:
 * - Defines a family of algorithms (commission calculation strategies)
 * - Encapsulates each algorithm in a separate class
 * - Makes algorithms interchangeable at runtime
 * 
 * WHY STRATEGY PATTERN HERE?
 * - Different commission models for different user types or transaction sizes
 * - Easy to add new commission strategies without modifying existing code (Open/Closed Principle)
 * - Client code (PaymentService) doesn't need to know implementation details
 * - Eliminates conditional logic (if/else chains) for different commission types
 * 
 * PATTERN PARTICIPANTS:
 * - Strategy (this interface): Declares common interface for all algorithms
 * - ConcreteStrategy (StandardCommissionStrategy, PremiumCommissionStrategy): Implements specific algorithms
 * - Context (CommissionCalculator): Uses a Strategy to execute the algorithm
 * 
 * @author Alessandro D'Angelo
 * @version 1.0
 */
public interface CommissionStrategy {
    
    /**
     * Calculates the commission amount to be retained by the platform.
     * 
     * @param totalAmount the total transaction amount
     * @return the commission amount to be retained
     */
    BigDecimal calculateCommission(BigDecimal totalAmount);
    
    /**
     * Gets the name of this commission strategy.
     * Used for logging and debugging.
     * 
     * @return strategy name
     */
    String getStrategyName();
}
