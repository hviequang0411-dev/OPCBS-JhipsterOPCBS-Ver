import dayjs from 'dayjs/esm';

import { IPatientProfile, NewPatientProfile } from './patient-profile.model';

export const sampleWithRequiredData: IPatientProfile = {
  id: 11483,
  fullName: 'repurpose awkwardly bogus',
  phone: '200.447.0072',
};

export const sampleWithPartialData: IPatientProfile = {
  id: 16467,
  fullName: 'courageous gosh',
  phone: '918-422-6482',
  dateOfBirth: dayjs('2026-09-27'),
  gender: 'MALE',
};

export const sampleWithFullData: IPatientProfile = {
  id: 32738,
  fullName: 'incomplete',
  phone: '1-581-942-7068 x24639',
  dateOfBirth: dayjs('2026-09-28'),
  gender: 'MALE',
  address: 'yowza outside mockingly',
  medicalHistory: '../fake-data/blob/hipster.txt',
};

export const sampleWithNewData: NewPatientProfile = {
  fullName: 'jut psst',
  phone: '402-348-5175 x85797',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
