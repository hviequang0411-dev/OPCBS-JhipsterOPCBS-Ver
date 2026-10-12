import dayjs from 'dayjs/esm';

import { IAvailabilitySlot } from 'app/entities/availability-slot/availability-slot.model';
import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { AppointmentStatus } from 'app/entities/enumerations/appointment-status.model';
import { ConsultationMode } from 'app/entities/enumerations/consultation-mode.model';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export interface IAppointment {
  id: number;
  bookingCode?: string | null;
  scheduledStart?: dayjs.Dayjs | null;
  scheduledEnd?: dayjs.Dayjs | null;
  consultationMode?: keyof typeof ConsultationMode | null;
  status?: keyof typeof AppointmentStatus | null;
  reason?: string | null;
  cancellationReason?: string | null;
  diagnosis?: string | null;
  consultationSummary?: string | null;
  therapyPlan?: string | null;
  recommendations?: string | null;
  followUpNotes?: string | null;
  nextAppointmentRecommendedDate?: dayjs.Dayjs | null;
  completedAt?: dayjs.Dayjs | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  patient?: IPatientProfile | null;
  doctor?: IDoctorProfile | null;
  slot?: IAvailabilitySlot | null;
}

export type NewAppointment = Omit<IAppointment, 'id'> & { id: null };
