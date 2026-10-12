import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export type TreatmentPackageStatus = 'CREATED' | 'PROPOSED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED';

export interface ITreatmentPackage {
  id?: number;
  name?: string | null;
  description?: string | null;
  targetOutcome?: string | null;
  recommendedExercises?: string | null;
  instructions?: string | null;
  sessionQuantity?: number | null;
  remainingSessions?: number | null;
  validityDays?: number | null;
  recommendedSessionsPerWeek?: number | null;
  expirationDate?: string | null;
  price?: number | null;
  status?: TreatmentPackageStatus | null;
  assignedDate?: string | null;
  acceptedDate?: string | null;
  acceptanceExpiresAt?: string | null;
  activeDate?: string | null;
  expiredAt?: string | null;
  rejectionReason?: string | null;
  cancellationReason?: string | null;
  cancellationRequestedAt?: string | null;
  doctor?: IDoctorProfile | null;
  patient?: IPatientProfile | null;
  createdDate?: string | null;
}

export type NewTreatmentPackage = Omit<ITreatmentPackage, 'id'> & { id: null };
