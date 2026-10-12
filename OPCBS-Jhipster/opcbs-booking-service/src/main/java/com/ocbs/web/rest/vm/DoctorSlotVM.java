package com.ocbs.web.rest.vm;

import com.ocbs.domain.enumeration.ConsultationMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * View Models for Doctor Schedule & Availability Slots Management.
 */
public class DoctorSlotVM {

    public static class CreateSlotVM {

        @NotNull
        private Instant startTime;

        @NotNull
        private Instant endTime;

        @NotNull
        private ConsultationMode consultationMode;

        @Size(max = 255)
        private String notes;

        public Instant getStartTime() {
            return startTime;
        }

        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public ConsultationMode getConsultationMode() {
            return consultationMode;
        }

        public void setConsultationMode(ConsultationMode consultationMode) {
            this.consultationMode = consultationMode;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class BatchCreateSlotVM {

        @NotNull
        private String startDate; // e.g. "2026-10-05"

        @NotNull
        private String endDate; // e.g. "2026-10-09"

        @NotNull
        private String dailyStartTime; // e.g. "08:30"

        @NotNull
        private String dailyEndTime; // e.g. "17:00"

        private int slotDurationMinutes = 60; // 30, 45, 60, etc.

        private List<Integer> daysOfWeek; // 1=Mon, 2=Tue, ..., 7=Sun

        @NotNull
        private ConsultationMode consultationMode;

        @Size(max = 255)
        private String notes;

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getDailyStartTime() {
            return dailyStartTime;
        }

        public void setDailyStartTime(String dailyStartTime) {
            this.dailyStartTime = dailyStartTime;
        }

        public String getDailyEndTime() {
            return dailyEndTime;
        }

        public void setDailyEndTime(String dailyEndTime) {
            this.dailyEndTime = dailyEndTime;
        }

        public int getSlotDurationMinutes() {
            return slotDurationMinutes;
        }

        public void setSlotDurationMinutes(int slotDurationMinutes) {
            this.slotDurationMinutes = slotDurationMinutes;
        }

        public List<Integer> getDaysOfWeek() {
            return daysOfWeek;
        }

        public void setDaysOfWeek(List<Integer> daysOfWeek) {
            this.daysOfWeek = daysOfWeek;
        }

        public ConsultationMode getConsultationMode() {
            return consultationMode;
        }

        public void setConsultationMode(ConsultationMode consultationMode) {
            this.consultationMode = consultationMode;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
