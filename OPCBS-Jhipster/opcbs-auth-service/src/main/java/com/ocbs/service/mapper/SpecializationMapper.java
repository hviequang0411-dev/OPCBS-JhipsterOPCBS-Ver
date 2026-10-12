package com.ocbs.service.mapper;

import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.Specialization;
import com.ocbs.service.dto.DoctorProfileDTO;
import com.ocbs.service.dto.SpecializationDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Specialization} and its DTO {@link SpecializationDTO}.
 */
@Mapper(componentModel = "spring")
public interface SpecializationMapper extends EntityMapper<SpecializationDTO, Specialization> {
    @Mapping(target = "doctorses", source = "doctorses", qualifiedByName = "doctorProfileIdSet")
    SpecializationDTO toDto(Specialization s);

    @Mapping(target = "doctorses", ignore = true)
    @Mapping(target = "removeDoctors", ignore = true)
    Specialization toEntity(SpecializationDTO specializationDTO);

    @Named("doctorProfileId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    DoctorProfileDTO toDtoDoctorProfileId(DoctorProfile doctorProfile);

    @Named("doctorProfileIdSet")
    default Set<DoctorProfileDTO> toDtoDoctorProfileIdSet(Set<DoctorProfile> doctorProfile) {
        return doctorProfile.stream().map(this::toDtoDoctorProfileId).collect(Collectors.toSet());
    }
}
