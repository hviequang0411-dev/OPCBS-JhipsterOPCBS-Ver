import dayjs from 'dayjs/esm';

import { IAvailabilitySlot, NewAvailabilitySlot } from './availability-slot.model';

export const sampleWithRequiredData: IAvailabilitySlot = {
  id: 26359,
  startTime: dayjs('2026-09-28T01:57'),
  endTime: dayjs('2026-09-28T00:13'),
  consultationMode: 'BOTH',
  status: 'BOOKED',
};

export const sampleWithPartialData: IAvailabilitySlot = {
  id: 339,
  startTime: dayjs('2026-09-28T01:43'),
  endTime: dayjs('2026-09-27T16:27'),
  consultationMode: 'BOTH',
  status: 'BOOKED',
};

export const sampleWithFullData: IAvailabilitySlot = {
  id: 4866,
  startTime: dayjs('2026-09-28T06:44'),
  endTime: dayjs('2026-09-27T22:38'),
  consultationMode: 'IN_PERSON',
  status: 'CANCELLED',
  notes: 'quaintly circular',
};

export const sampleWithNewData: NewAvailabilitySlot = {
  startTime: dayjs('2026-09-28T02:52'),
  endTime: dayjs('2026-09-27T16:36'),
  consultationMode: 'BOTH',
  status: 'AVAILABLE',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
