export interface IServicePackage {
  id: number;
  name: string;
  description?: string;
  price: number;
  durationDays: number;
  maxAppointmentsPerMonth: number;
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
  status: 'ACTIVE' | 'PENDING_PAYMENT' | 'EXPIRED' | 'CANCELLED';
  startDate?: string;
  expirationDate?: string;
  transactionCode?: string;
  createdDate?: string;
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
