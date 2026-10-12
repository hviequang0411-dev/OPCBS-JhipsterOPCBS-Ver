import { IDoctorProfile, NewDoctorProfile } from './doctor-profile.model';

export const sampleWithRequiredData: IDoctorProfile = {
  id: 22589,
  fullName: 'if',
  isActive: false,
};

export const sampleWithPartialData: IDoctorProfile = {
  id: 8227,
  fullName: 'plus whenever obstruct',
  bio: '../fake-data/blob/hipster.txt',
  consultationFee: 6893.32,
  gender: 'OTHER',
  isActive: false,
};

export const sampleWithFullData: IDoctorProfile = {
  id: 26765,
  fullName: 'playfully light',
  title: 'that',
  bio: '../fake-data/blob/hipster.txt',
  experienceYears: 22487,
  consultationFee: 27559.87,
  phone: '(200) 256-3259 x7004',
  gender: 'OTHER',
  isActive: false,
};

export const sampleWithNewData: NewDoctorProfile = {
  fullName: 'castanet for dense',
  isActive: false,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
