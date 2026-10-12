import { Gender } from 'app/entities/enumerations/gender.model';
import { ISpecialization } from 'app/entities/specialization/specialization.model';
import { IUser } from 'app/entities/user/user.model';

export interface IDoctorProfile {
  id: number;
  fullName?: string | null;
  title?: string | null;
  bio?: string | null;
  experienceYears?: number | null;
  consultationFee?: number | null;
  phone?: string | null;
  gender?: keyof typeof Gender | null;
  isActive?: boolean | null;
  user?: Pick<IUser, 'id' | 'login'> | null;
  specializationses?: Pick<ISpecialization, 'id' | 'name'>[] | null;
}

export type NewDoctorProfile = Omit<IDoctorProfile, 'id'> & { id: null };
