import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export type TreatmentPackageStatus =
  | 'CREATED'
  | 'PROPOSED'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'EXPIRED';

export interface ITreatmentGoal {
  id?: number;
  title: string;
  description?: string | null;
  category?: 'EMOTION' | 'BEHAVIOR' | 'COGNITIVE' | 'SOCIAL' | 'OTHER' | string | null;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | string | null;
  status?: 'NOT_STARTED' | 'IN_PROGRESS' | 'ACHIEVED' | 'PAUSED' | 'CANCELLED' | string | null;
  progressPercent?: number | null;
  targetDate?: string | null;
  achievedDate?: string | null;
  doctorNotes?: string | null;
}

export interface ITreatmentSession {
  id?: number;
  sessionNumber?: number | null;
  title?: string | null;
  sessionSummary?: string | null;
  doctorClinicalAssessment?: string | null;
  patientFriendlySummary?: string | null;
  moodBefore?: number | null; // 1 - 10
  moodAfter?: number | null; // 1 - 10
  status?: 'PLANNED' | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW' | string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ITreatmentProgressSummary {
  packageId: number;
  packageName: string;
  status: string;
  isHoldRequested: boolean;
  holdStartDate?: string | null;
  holdEndDate?: string | null;
  totalHoldDays: number;
  totalSessions: number;
  remainingSessions: number;
  completedSessions: number;
  sessionProgressPercent: number;
  totalGoals: number;
  completedGoals: number;
  goalProgressPercent: number;
  recordedSessions: number;
}

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

  // === HOLD (BẢO LƯU) LIFECYCLE FIELDS ===
  isHoldRequested?: boolean | null;
  holdRequestedAt?: string | null;
  holdStartDate?: string | null;
  holdEndDate?: string | null;
  holdDurationDays?: number | null;
  holdReason?: string | null;
  holdApprovedAt?: string | null;
  holdApprovedByDoctorId?: number | null;
  holdRejectedAt?: string | null;
  holdRejectionReason?: string | null;
  totalHoldDays?: number | null;

  doctor?: IDoctorProfile | null;
  patient?: IPatientProfile | null;
  createdDate?: string | null;
}

export type NewTreatmentPackage = Omit<ITreatmentPackage, 'id'> & { id: null };
