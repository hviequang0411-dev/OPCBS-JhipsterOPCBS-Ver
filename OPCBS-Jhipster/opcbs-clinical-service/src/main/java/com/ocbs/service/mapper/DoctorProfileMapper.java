package com.ocbs.service.mapper;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Specialization;
import com.ocbs.domain.User;
import com.ocbs.service.dto.DoctorProfileDTO;
import com.ocbs.service.dto.SpecializationDTO;
import com.ocbs.service.dto.UserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link DoctorProfile} and its DTO {@link DoctorProfileDTO}.
 */
@Mapper(componentModel = "spring")
public interface DoctorProfileMapper extends EntityMapper<DoctorProfileDTO, DoctorProfile> {
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    @Mapping(target = "specializationses", source = "specializationses", qualifiedByName = "specializationNameSet")
    DoctorProfileDTO toDto(DoctorProfile s);

    @Mapping(target = "removeSpecializations", ignore = true)
    DoctorProfile toEntity(DoctorProfileDTO doctorProfileDTO);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("specializationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    SpecializationDTO toDtoSpecializationName(Specialization specialization);

    @Named("specializationNameSet")
    default Set<SpecializationDTO> toDtoSpecializationNameSet(Set<Specialization> specialization) {
        return specialization.stream().map(this::toDtoSpecializationName).collect(Collectors.toSet());
    }
}
