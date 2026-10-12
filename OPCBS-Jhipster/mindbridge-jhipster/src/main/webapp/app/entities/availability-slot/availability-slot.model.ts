import dayjs from 'dayjs/esm';

import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { ConsultationMode } from 'app/entities/enumerations/consultation-mode.model';
import { SlotStatus } from 'app/entities/enumerations/slot-status.model';

export interface IAvailabilitySlot {
  id: number;
  startTime?: dayjs.Dayjs | null;
  endTime?: dayjs.Dayjs | null;
  consultationMode?: keyof typeof ConsultationMode | null;
  status?: keyof typeof SlotStatus | null;
  notes?: string | null;
  doctor?: Pick<IDoctorProfile, 'id' | 'fullName'> | null;
  appointmentId?: number | null;
  appointmentBookingCode?: string | null;
  patientName?: string | null;
  patientPhone?: string | null;
  appointmentStatus?: string | null;
}

export type NewAvailabilitySlot = Omit<IAvailabilitySlot, 'id'> & { id: null };
