package it.eably.backend.mapper;

import it.eably.backend.dto.profile.response.ProfilePublicDTO;
import it.eably.backend.dto.profile.response.ProfileResponseDTO;
import it.eably.backend.model.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Profile entity to DTO conversion.
 * 
 * MapStruct generates implementation at compile time, providing:
 * - Type-safe mapping
 * - High performance (no reflection)
 * - Compile-time error checking
 *
 * @author Alessandro D'Angelo
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface ProfileMapper {
    
    /**
     * Maps Profile entity to ProfileResponseDTO.
     * 
     * MapStruct automatically maps fields with matching names.
     * @Mapping annotations handle nested properties.
     * 
     * @param profile the profile entity
     * @return the profile response DTO
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    ProfileResponseDTO toResponseDTO(Profile profile);
    
    /**
     * Maps list of Profile entities to list of ProfileResponseDTOs.
     * For authenticated owner use only.
     *
     * @param profiles list of profile entities
     * @return list of profile response DTOs
     */
    List<ProfileResponseDTO> toResponseDTOList(List<Profile> profiles);

    /**
     * Maps Profile entity to ProfilePublicDTO.
     * Excludes sensitive fields: email, userId, createdAt, updatedAt.
     * Use for public endpoints.
     *
     * @param profile the profile entity
     * @return the public profile DTO
     */
    @Mapping(source = "user.username", target = "username")
    ProfilePublicDTO toPublicDTO(Profile profile);

    /**
     * Maps list of Profile entities to list of ProfilePublicDTOs.
     * Use for public endpoints.
     *
     * @param profiles list of profile entities
     * @return list of public profile DTOs
     */
    List<ProfilePublicDTO> toPublicDTOList(List<Profile> profiles);
}
