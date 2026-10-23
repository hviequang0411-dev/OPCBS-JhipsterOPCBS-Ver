package com.ocbs.web.rest.vm;

import com.ocbs.domain.enumeration.ConsultationMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * View Models for Booking & Appointment operations.
 */
public class BookingVM {

    public static class CreateBookingVM {

        @NotNull
        private Long slotId;

        @NotNull
        private ConsultationMode consultationMode;

        @Size(max = 1000)
        private String reason;

        @Size(max = 1000)
        private String symptoms;

        @Size(max = 1000)
        private String medicalHistory;

        @Size(max = 1000)
        private String expectations;

        @Size(max = 20)
        private String contactPhone;

        @Size(max = 20)
        private String contactZalo;

        @Size(max = 255)
        private String patientAddress;

        public Long getSlotId() {
            return slotId;
        }

        public void setSlotId(Long slotId) {
            this.slotId = slotId;
        }

        public ConsultationMode getConsultationMode() {
            return consultationMode;
        }

        public void setConsultationMode(ConsultationMode consultationMode) {
            this.consultationMode = consultationMode;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getSymptoms() {
            return symptoms;
        }

        public void setSymptoms(String symptoms) {
            this.symptoms = symptoms;
        }

        public String getMedicalHistory() {
            return medicalHistory;
        }

        public void setMedicalHistory(String medicalHistory) {
            this.medicalHistory = medicalHistory;
        }

        public String getExpectations() {
            return expectations;
        }

        public void setExpectations(String expectations) {
            this.expectations = expectations;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public String getContactZalo() {
            return contactZalo;
        }

        public void setContactZalo(String contactZalo) {
            this.contactZalo = contactZalo;
        }

        public String getPatientAddress() {
            return patientAddress;
        }

        public void setPatientAddress(String patientAddress) {
            this.patientAddress = patientAddress;
        }
    }

    public static class CancelBookingVM {

        @Size(max = 500)
        private String cancellationReason;

        public String getCancellationReason() {
            return cancellationReason;
        }

        public void setCancellationReason(String cancellationReason) {
            this.cancellationReason = cancellationReason;
        }
    }

    public static class RejectAppointmentVM {

        @Size(max = 500)
        private String rejectionReason;

        public String getRejectionReason() {
            return rejectionReason;
        }

        public void setRejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
        }
    }

    public static class AppointmentStatsVM {

        private long total;
        private long pending;
        private long accepted;
        private long inProgress;
        private long completed;
        private long cancelled;
        private long rejected;

        public AppointmentStatsVM() {}

        public AppointmentStatsVM(long total, long pending, long accepted, long inProgress, long completed, long cancelled, long rejected) {
            this.total = total;
            this.pending = pending;
            this.accepted = accepted;
            this.inProgress = inProgress;
            this.completed = completed;
            this.cancelled = cancelled;
            this.rejected = rejected;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getPending() {
            return pending;
        }

        public void setPending(long pending) {
            this.pending = pending;
        }

        public long getAccepted() {
            return accepted;
        }

        public void setAccepted(long accepted) {
            this.accepted = accepted;
        }

        public long getInProgress() {
            return inProgress;
        }

        public void setInProgress(long inProgress) {
            this.inProgress = inProgress;
        }

        public long getCompleted() {
            return completed;
        }

        public void setCompleted(long completed) {
            this.completed = completed;
        }

        public long getCancelled() {
            return cancelled;
        }

        public void setCancelled(long cancelled) {
            this.cancelled = cancelled;
        }

        public long getRejected() {
            return rejected;
        }

        public void setRejected(long rejected) {
            this.rejected = rejected;
        }
    }

    public static class SaveConsultationNoteVM {

        @Size(max = 500)
        private String diagnosis;

        private String consultationSummary;

        private String therapyPlan;

        private String recommendations;

        private String followUpNotes;

        private java.time.Instant nextAppointmentRecommendedDate;

        private boolean markCompleted = true;

        public String getDiagnosis() {
            return diagnosis;
        }

        public void setDiagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
        }

        public String getConsultationSummary() {
            return consultationSummary;
        }

        public void setConsultationSummary(String consultationSummary) {
            this.consultationSummary = consultationSummary;
        }

        public String getTherapyPlan() {
            return therapyPlan;
        }

        public void setTherapyPlan(String therapyPlan) {
            this.therapyPlan = therapyPlan;
        }

        public String getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(String recommendations) {
            this.recommendations = recommendations;
        }

        public String getFollowUpNotes() {
            return followUpNotes;
        }

        public void setFollowUpNotes(String followUpNotes) {
            this.followUpNotes = followUpNotes;
        }

        public java.time.Instant getNextAppointmentRecommendedDate() {
            return nextAppointmentRecommendedDate;
        }

        public void setNextAppointmentRecommendedDate(java.time.Instant nextAppointmentRecommendedDate) {
            this.nextAppointmentRecommendedDate = nextAppointmentRecommendedDate;
        }

        public boolean isMarkCompleted() {
            return markCompleted;
        }

        public void setMarkCompleted(boolean markCompleted) {
            this.markCompleted = markCompleted;
        }
    }

    public static class DoctorPatientSummaryVM {

        private String email;
        private boolean isGuest;
        private java.time.Instant createdAt;
        private Long patientId;
        private String fullName;
        private String phone;
        private com.ocbs.domain.enumeration.Gender gender;
        private java.time.LocalDate dateOfBirth;
        private String address;
        private String medicalHistory;
        private String allergies;
        private String bloodGroup;
        private long totalAppointments;
        private long completedAppointments;
        private long upcomingAppointments;
        private java.time.Instant lastAppointmentDate;
        private String lastDiagnosis;

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public com.ocbs.domain.enumeration.Gender getGender() {
            return gender;
        }

        public void setGender(com.ocbs.domain.enumeration.Gender gender) {
            this.gender = gender;
        }

        public java.time.LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(java.time.LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getMedicalHistory() {
            return medicalHistory;
        }

        public void setMedicalHistory(String medicalHistory) {
            this.medicalHistory = medicalHistory;
        }

        public String getAllergies() {
            return allergies;
        }

        public void setAllergies(String allergies) {
            this.allergies = allergies;
        }

        public String getBloodGroup() {
            return bloodGroup;
        }

        public void setBloodGroup(String bloodGroup) {
            this.bloodGroup = bloodGroup;
        }

        public long getTotalAppointments() {
            return totalAppointments;
        }

        public void setTotalAppointments(long totalAppointments) {
            this.totalAppointments = totalAppointments;
        }

        public long getCompletedAppointments() {
            return completedAppointments;
        }

        public void setCompletedAppointments(long completedAppointments) {
            this.completedAppointments = completedAppointments;
        }

        public long getUpcomingAppointments() {
            return upcomingAppointments;
        }

        public void setUpcomingAppointments(long upcomingAppointments) {
            this.upcomingAppointments = upcomingAppointments;
        }

        public java.time.Instant getLastAppointmentDate() {
            return lastAppointmentDate;
        }

        public void setLastAppointmentDate(java.time.Instant lastAppointmentDate) {
            this.lastAppointmentDate = lastAppointmentDate;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isGuest() {
            return isGuest;
        }

        public void setGuest(boolean guest) {
            isGuest = guest;
        }

        public java.time.Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(java.time.Instant createdAt) {
            this.createdAt = createdAt;
        }

        public String getLastDiagnosis() {
            return lastDiagnosis;
        }

        public void setLastDiagnosis(String lastDiagnosis) {
            this.lastDiagnosis = lastDiagnosis;
        }
    }

    public static class PatientDetailRecordVM {

        private com.ocbs.service.dto.PatientProfileDTO patient;
        private long totalAppointments;
        private long completedAppointments;
        private java.util.List<com.ocbs.service.dto.AppointmentDTO> appointments;
        private java.util.List<com.ocbs.domain.PsychometricSubmission> psychometricSubmissions;
        private java.util.List<com.ocbs.domain.EmotionJournal> sharedJournals;
        private java.util.List<com.ocbs.domain.TreatmentPackage> treatmentPackages;
        private boolean hasNegativeRisk;
        private String warningMessage;
        private java.util.Map<String, Long> moodDistribution;
        private java.util.Map<String, Long> testSeverityDistribution;

        public PatientDetailRecordVM() {}

        public PatientDetailRecordVM(
            com.ocbs.service.dto.PatientProfileDTO patient,
            long totalAppointments,
            long completedAppointments,
            java.util.List<com.ocbs.service.dto.AppointmentDTO> appointments
        ) {
            this.patient = patient;
            this.totalAppointments = totalAppointments;
            this.completedAppointments = completedAppointments;
            this.appointments = appointments;
        }

        public com.ocbs.service.dto.PatientProfileDTO getPatient() {
            return patient;
        }

        public void setPatient(com.ocbs.service.dto.PatientProfileDTO patient) {
            this.patient = patient;
        }

        public long getTotalAppointments() {
            return totalAppointments;
        }

        public void setTotalAppointments(long totalAppointments) {
            this.totalAppointments = totalAppointments;
        }

        public long getCompletedAppointments() {
            return completedAppointments;
        }

        public void setCompletedAppointments(long completedAppointments) {
            this.completedAppointments = completedAppointments;
        }

        public java.util.List<com.ocbs.service.dto.AppointmentDTO> getAppointments() {
            return appointments;
        }

        public void setAppointments(java.util.List<com.ocbs.service.dto.AppointmentDTO> appointments) {
            this.appointments = appointments;
        }

        public java.util.List<com.ocbs.domain.PsychometricSubmission> getPsychometricSubmissions() {
            return psychometricSubmissions;
        }

        public void setPsychometricSubmissions(java.util.List<com.ocbs.domain.PsychometricSubmission> psychometricSubmissions) {
            this.psychometricSubmissions = psychometricSubmissions;
        }

        public java.util.List<com.ocbs.domain.EmotionJournal> getSharedJournals() {
            return sharedJournals;
        }

        public void setSharedJournals(java.util.List<com.ocbs.domain.EmotionJournal> sharedJournals) {
            this.sharedJournals = sharedJournals;
        }

        public java.util.List<com.ocbs.domain.TreatmentPackage> getTreatmentPackages() {
            return treatmentPackages;
        }

        public void setTreatmentPackages(java.util.List<com.ocbs.domain.TreatmentPackage> treatmentPackages) {
            this.treatmentPackages = treatmentPackages;
        }

        public boolean isHasNegativeRisk() {
            return hasNegativeRisk;
        }

        public void setHasNegativeRisk(boolean hasNegativeRisk) {
            this.hasNegativeRisk = hasNegativeRisk;
        }

        public String getWarningMessage() {
            return warningMessage;
        }

        public void setWarningMessage(String warningMessage) {
            this.warningMessage = warningMessage;
        }

        public java.util.Map<String, Long> getMoodDistribution() {
            return moodDistribution;
        }

        public void setMoodDistribution(java.util.Map<String, Long> moodDistribution) {
            this.moodDistribution = moodDistribution;
        }

        public java.util.Map<String, Long> getTestSeverityDistribution() {
            return testSeverityDistribution;
        }

        public void setTestSeverityDistribution(java.util.Map<String, Long> testSeverityDistribution) {
            this.testSeverityDistribution = testSeverityDistribution;
        }
    }
}
