import { ISpecialization, NewSpecialization } from './specialization.model';

export const sampleWithRequiredData: ISpecialization = {
  id: 30737,
  name: 'astride',
};

export const sampleWithPartialData: ISpecialization = {
  id: 15646,
  name: 'above',
};

export const sampleWithFullData: ISpecialization = {
  id: 24204,
  name: 'after meager',
  description: 'ouch',
};

export const sampleWithNewData: NewSpecialization = {
  name: 'than worth precious',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
