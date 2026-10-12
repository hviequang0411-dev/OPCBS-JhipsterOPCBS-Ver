import dayjs from 'dayjs/esm';

import { Gender } from 'app/entities/enumerations/gender.model';
import { IUser } from 'app/entities/user/user.model';

export interface IPatientProfile {
  id: number;
  fullName?: string | null;
  phone?: string | null;
  dateOfBirth?: dayjs.Dayjs | null;
  gender?: keyof typeof Gender | null;
  address?: string | null;
  medicalHistory?: string | null;
  allergies?: string | null;
  emergencyContactPhone?: string | null;
  bloodGroup?: string | null;
  user?: Pick<IUser, 'id' | 'login'> | null;
}

export type NewPatientProfile = Omit<IPatientProfile, 'id'> & { id: null };
