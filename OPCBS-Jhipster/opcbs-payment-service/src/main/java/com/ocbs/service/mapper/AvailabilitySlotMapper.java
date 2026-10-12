package com.ocbs.service.mapper;

import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.dto.DoctorProfileDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AvailabilitySlot} and its DTO {@link AvailabilitySlotDTO}.
 */
@Mapper(componentModel = "spring")
public interface AvailabilitySlotMapper extends EntityMapper<AvailabilitySlotDTO, AvailabilitySlot> {
    @Mapping(target = "doctor", source = "doctor", qualifiedByName = "doctorProfileFullName")
    AvailabilitySlotDTO toDto(AvailabilitySlot s);

    @Named("doctorProfileFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    DoctorProfileDTO toDtoDoctorProfileFullName(DoctorProfile doctorProfile);
}
