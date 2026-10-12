package com.ocbs.service.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A DTO representing the unified user profile (User + PatientProfile / DoctorProfile).
 */
public class UserProfileDTO implements Serializable {

    private Long userId;
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private boolean activated;
    private String langKey;
    private Set<String> authorities = new HashSet<>();
    private String primaryRole; // "PATIENT", "DOCTOR", "ADMIN", "USER"

    private PatientProfileDTO patientProfile;
    private DoctorProfileDTO doctorProfile;

    public UserProfileDTO() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(String primaryRole) {
        this.primaryRole = primaryRole;
    }

    public PatientProfileDTO getPatientProfile() {
        return patientProfile;
    }

    public void setPatientProfile(PatientProfileDTO patientProfile) {
        this.patientProfile = patientProfile;
    }

    public DoctorProfileDTO getDoctorProfile() {
        return doctorProfile;
    }

    public void setDoctorProfile(DoctorProfileDTO doctorProfile) {
        this.doctorProfile = doctorProfile;
    }
}
