import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';

export interface ISpecialization {
  id: number;
  name?: string | null;
  description?: string | null;
  doctorses?: Pick<IDoctorProfile, 'id'>[] | null;
}

export type NewSpecialization = Omit<ISpecialization, 'id'> & { id: null };
