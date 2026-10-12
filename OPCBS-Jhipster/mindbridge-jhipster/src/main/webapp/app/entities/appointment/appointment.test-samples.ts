import dayjs from 'dayjs/esm';

import { IAppointment, NewAppointment } from './appointment.model';

export const sampleWithRequiredData: IAppointment = {
  id: 13399,
  bookingCode: 'fog since',
  scheduledStart: dayjs('2026-09-27T17:01'),
  scheduledEnd: dayjs('2026-09-28T05:39'),
  consultationMode: 'BOTH',
  status: 'ACCEPTED',
  createdAt: dayjs('2026-09-27T12:53'),
};

export const sampleWithPartialData: IAppointment = {
  id: 18912,
  bookingCode: 'ugh',
  scheduledStart: dayjs('2026-09-28T05:26'),
  scheduledEnd: dayjs('2026-09-27T14:23'),
  consultationMode: 'ONLINE',
  status: 'REJECTED',
  createdAt: dayjs('2026-09-28T09:40'),
  updatedAt: dayjs('2026-09-27T16:14'),
};

export const sampleWithFullData: IAppointment = {
  id: 5963,
  bookingCode: 'austere',
  scheduledStart: dayjs('2026-09-27T16:43'),
  scheduledEnd: dayjs('2026-09-27T17:46'),
  consultationMode: 'IN_PERSON',
  status: 'COMPLETED',
  reason: 'hierarchy light politely',
  cancellationReason: 'jaywalk',
  createdAt: dayjs('2026-09-27T22:11'),
  updatedAt: dayjs('2026-09-27T21:09'),
};

export const sampleWithNewData: NewAppointment = {
  bookingCode: 'but fortunately bleakly',
  scheduledStart: dayjs('2026-09-28T00:00'),
  scheduledEnd: dayjs('2026-09-27T13:38'),
  consultationMode: 'ONLINE',
  status: 'REJECTED',
  createdAt: dayjs('2026-09-28T00:00'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
