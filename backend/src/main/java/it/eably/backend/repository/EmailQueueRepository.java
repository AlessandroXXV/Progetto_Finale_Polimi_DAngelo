package it.eably.backend.repository;

import it.eably.backend.model.EmailQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for {@link it.eably.backend.model.EmailQueue} entity.
 *
 * Provides queries for managing the outbound email queue used by the retry scheduler.
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {

    /**
     * Finds all pending emails that are eligible for (re)sending.
     * An email is eligible when its status is {@code PENDING} and its retry count
     * has not yet reached the configured maximum.
     * Results are ordered by creation time ascending to process oldest entries first.
     *
     * @return list of pending emails eligible for retry, ordered by createdAt ascending
     */
    @Query("SELECT e FROM EmailQueue e WHERE e.status = 'PENDING' AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC")
    List<EmailQueue> findPendingEmailsToRetry();
}
