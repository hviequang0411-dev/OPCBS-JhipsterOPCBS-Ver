export interface IServicePackage {
  id: number;
  name: string;
  description?: string;
  price: number;
  durationDays: number;
  maxAppointmentsPerMonth?: number;
  maxPatientCapacity?: number | null;
  maxDailySlotsCapacity?: number | null;
  features?: string;
  badge?: string;
  isActive: boolean;
  displayOrder?: number;
}

export interface IDoctorSubscription {
  id: number;
  doctorLogin: string;
  servicePackageId: number;
  servicePackageName: string;
  price: number;
  status: 'ACTIVE' | 'PENDING_PAYMENT' | 'EXPIRED' | 'CANCELLED' | 'EXTENDED' | 'PAYMENT_FAILED';
  startDate?: string;
  expirationDate?: string;
  transactionCode?: string;
  createdDate?: string;
  durationDays?: number;
  maxAppointmentsPerMonth?: number;
  maxPatientCapacity?: number | null;
  maxDailySlotsCapacity?: number | null;
}

export interface IDoctorSubscriptionQuota {
  hasActiveSubscription: boolean;
  subscriptionId?: number;
  servicePackageId?: number;
  servicePackageName?: string;
  status?: string;
  startDate?: string;
  expirationDate?: string;
  daysRemaining?: number;
  maxPatientCapacity?: number | null;
  currentPatientCount?: number;
  remainingPatientCapacity?: number | null;
  maxDailySlotsCapacity?: number | null;
  message?: string;
}

export interface IPaymentTransaction {
  id: number;
  subscriptionId: number;
  transactionCode: string;
  amount: number;
  paymentMethod: string;
  paymentStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
  paidAt?: string;
  responseCode?: string;
  responseMessage?: string;
  createdDate?: string;
}
