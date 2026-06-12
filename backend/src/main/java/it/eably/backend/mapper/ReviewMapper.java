package it.eably.backend.mapper;

import it.eably.backend.dto.review.response.ReviewDetailResponseDTO;
import it.eably.backend.dto.review.response.ReviewResponseDTO;
import it.eably.backend.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Review entity to DTO conversion.
 *
 * MapStruct generates the implementation at compile time, providing:
 * - Type-safe mapping
 * - High performance (no reflection)
 * - Compile-time error checking
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    /**
     * Maps a {@link Review} to a basic {@link ReviewResponseDTO}.
     */
    @Mapping(source = "booking.id",   target = "bookingId")
    @Mapping(source = "reviewer.id",  target = "reviewerId")
    @Mapping(source = "reviewee.id",  target = "revieweeId")
    ReviewResponseDTO toResponseDTO(Review review);

    /**
     * Maps a {@link Review} to a detailed {@link ReviewDetailResponseDTO}
     * including the reviewee student's username and service title.
     */
    @Mapping(source = "reviewee.user.username", target = "studentUsername")
    @Mapping(source = "reviewee.title",         target = "serviceTitle")
    ReviewDetailResponseDTO toDetailResponseDTO(Review review);

    /**
     * Maps a list of {@link Review} entities to basic response DTOs.
     */
    List<ReviewResponseDTO> toResponseDTOList(List<Review> reviews);

    /**
     * Maps a list of {@link Review} entities to detailed response DTOs.
     */
    List<ReviewDetailResponseDTO> toDetailResponseDTOList(List<Review> reviews);
}
