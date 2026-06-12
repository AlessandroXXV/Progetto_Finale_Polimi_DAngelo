package it.eably.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity representing a single chat message exchanged within a booking conversation.
 *
 * <p>Each message belongs to a {@link Booking} and is sent by a {@link User} (the sender).
 * Messages are stored in the {@code chat_messages} table and inherit audit fields
 * ({@code created_at}, {@code updated_at}) from {@link BaseEntity}.</p>
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    /** The booking this message belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** The user who sent this message. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** The text content of the message. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Default no-arg constructor required by JPA. */
    public ChatMessage() {
        super();
    }

    /**
     * Full constructor for creating a new chat message.
     *
     * @param booking  the booking this message belongs to
     * @param sender   the user sending the message
     * @param content  the text content of the message
     */
    public ChatMessage(Booking booking, User sender, String content) {
        super();
        this.booking = booking;
        this.sender = sender;
        this.content = content;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public void validate() {
        if (booking == null) throw new it.eably.backend.exception.ValidationException("ChatMessage must be associated with a booking");
        if (sender == null) throw new it.eably.backend.exception.ValidationException("ChatMessage must have a sender");
        if (content == null || content.trim().isEmpty())
            throw new it.eably.backend.exception.ValidationException("Message content cannot be null or empty");
    }
}
