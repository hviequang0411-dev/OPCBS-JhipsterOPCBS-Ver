import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';
import { IPatientProfile } from 'app/entities/patient-profile/patient-profile.model';

export interface IPsychometricQuestion {
  id?: number;
  questionText?: string | null;
  questionNumber?: number | null;
  category?: string | null;
  questionType?: string | null;
  optionsJson?: string | null;
}

export interface IPsychometricTest {
  id?: number;
  title?: string | null;
  description?: string | null;
  testType?: string | null;
  category?: string | null;
  purpose?: string | null;
  doctor?: IDoctorProfile | null;
  createdByDoctor?: IDoctorProfile | null;
  isSystemTemplate?: boolean | null;
  instructions?: string | null;
  scoreRangesJson?: string | null;
  sourceUrl?: string | null;
  isActive?: boolean | null;
  questions?: IPsychometricQuestion[];
}

export interface IPsychometricAnswer {
  id?: number;
  question?: IPsychometricQuestion | null;
  score?: number | null;
  answerText?: string | null;
}

export interface IPsychometricSubmission {
  id?: number;
  test?: IPsychometricTest | null;
  patient?: IPatientProfile | null;
  assignedByDoctor?: IDoctorProfile | null;
  totalScore?: number | null;
  scoreDataJson?: string | null;
  interpretation?: string | null;
  doctorNotes?: string | null;
  dueDate?: string | null;
  status?: string | null;
  completedAt?: string | null;
  answers?: IPsychometricAnswer[];
  createdDate?: string | null;
}

export interface ISubmitAnswerVM {
  questionId: number;
  score: number;
  answerText?: string;
}

export interface ISubmissionRequestVM {
  assignedSubmissionId?: number;
  appointmentId?: number;
  answers: ISubmitAnswerVM[];
}

export interface IAssignTestVM {
  testId: number;
  patientId: number;
  dueDate?: string;
  doctorNotes?: string;
}
