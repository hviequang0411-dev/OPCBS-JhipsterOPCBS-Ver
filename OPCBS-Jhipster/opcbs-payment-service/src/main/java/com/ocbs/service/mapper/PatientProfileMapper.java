package com.ocbs.service.mapper;

import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.service.dto.PatientProfileDTO;
import com.ocbs.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PatientProfile} and its DTO {@link PatientProfileDTO}.
 */
@Mapper(componentModel = "spring")
public interface PatientProfileMapper extends EntityMapper<PatientProfileDTO, PatientProfile> {
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    PatientProfileDTO toDto(PatientProfile s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
