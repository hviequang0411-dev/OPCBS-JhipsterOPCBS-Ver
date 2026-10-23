package com.ocbs.service.mapper;

import com.ocbs.domain.Appointment;
import com.ocbs.domain.AvailabilitySlot;
import com.ocbs.domain.DoctorProfile;
import com.ocbs.domain.PatientProfile;
import com.ocbs.domain.User;
import com.ocbs.service.dto.AppointmentDTO;
import com.ocbs.service.dto.AvailabilitySlotDTO;
import com.ocbs.service.dto.DoctorProfileDTO;
import com.ocbs.service.dto.PatientProfileDTO;
import com.ocbs.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Appointment} and its DTO {@link AppointmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface AppointmentMapper extends EntityMapper<AppointmentDTO, Appointment> {
    @Mapping(target = "patient", source = "patient", qualifiedByName = "patientProfileFullName")
    @Mapping(target = "doctor", source = "doctor", qualifiedByName = "doctorProfileFullName")
    @Mapping(target = "slot", source = "slot", qualifiedByName = "availabilitySlotId")
    AppointmentDTO toDto(Appointment s);

    @Named("patientProfileFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "medicalHistory", source = "medicalHistory")
    @Mapping(target = "allergies", source = "allergies")
    @Mapping(target = "bloodGroup", source = "bloodGroup")
    @Mapping(target = "emergencyContactPhone", source = "emergencyContactPhone")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    PatientProfileDTO toDtoPatientProfileFullName(PatientProfile patientProfile);

    @Named("doctorProfileFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    DoctorProfileDTO toDtoDoctorProfileFullName(DoctorProfile doctorProfile);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("availabilitySlotId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    AvailabilitySlotDTO toDtoAvailabilitySlotId(AvailabilitySlot availabilitySlot);
}
