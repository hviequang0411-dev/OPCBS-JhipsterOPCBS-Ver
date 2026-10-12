import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export interface IEmotionJournal {
  id?: number;
  title?: string | null;
  content?: string | null;
  moodScale?: number | null; // 1 to 5
  stressScale?: number | null; // 1 to 5
  sleepHours?: number | null;
  depressionScale?: number | null; // 1 to 5
  isShared?: boolean | null;
  patient?: IPatientProfile | null;
  createdDate?: string | null;
}

export interface IJournalStats {
  totalEntries: number;
  averageMood: number;
  averageStress: number;
  averageSleepHours: number;
  moodDistribution: Record<number, number>;
}
