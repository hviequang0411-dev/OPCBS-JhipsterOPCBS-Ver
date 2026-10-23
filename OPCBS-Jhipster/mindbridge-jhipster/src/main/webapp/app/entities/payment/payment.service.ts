import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IServicePackage, IDoctorSubscription, IDoctorSubscriptionQuota } from './payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly apiEndpoint = 'api';

  getServicePackages(): Observable<IServicePackage[]> {
    return this.http.get<IServicePackage[]>(`${this.apiEndpoint}/service-packages`);
  }

  getMySubscription(): Observable<IDoctorSubscription> {
    return this.http.get<IDoctorSubscription>(`${this.apiEndpoint}/doctor/my-subscription`);
  }

  getSubscriptionQuota(): Observable<IDoctorSubscriptionQuota> {
    return this.http.get<IDoctorSubscriptionQuota>(`${this.apiEndpoint}/doctor/subscription-quota`);
  }

  getMySubscriptionHistory(): Observable<IDoctorSubscription[]> {
    return this.http.get<IDoctorSubscription[]>(`${this.apiEndpoint}/doctor/subscriptions/history`);
  }

  renewSubscription(packageId?: number): Observable<{
    subscriptionId: number;
    transactionCode: string;
    amount: number;
    packageName: string;
    status: string;
    isRenewal: boolean;
  }> {
    const params: any = {};
    if (packageId) {
      params.packageId = packageId.toString();
    }
    return this.http.post<any>(`${this.apiEndpoint}/doctor/subscriptions/renew`, null, { params });
  }

  subscribePackage(packageId: number): Observable<{
    subscriptionId: number;
    transactionCode: string;
    amount: number;
    packageName: string;
    status: string;
  }> {
    return this.http.post<any>(`${this.apiEndpoint}/doctor/subscriptions/subscribe`, null, {
      params: { packageId: packageId.toString() },
    });
  }

  createVnPayUrl(transactionCode: string, returnUrl?: string): Observable<{ paymentUrl: string; transactionCode: string }> {
    const params: any = { transactionCode };
    if (returnUrl) {
      params.returnUrl = returnUrl;
    }
    return this.http.post<{ paymentUrl: string; transactionCode: string }>(
      `${this.apiEndpoint}/payments/vnpay/create-url`,
      null,
      { params }
    );
  }

  mockCheckout(transactionCode: string): Observable<{
    success: boolean;
    status: string;
    transactionCode: string;
    message: string;
    startDate: string;
    expirationDate: string;
  }> {
    return this.http.post<any>(`${this.apiEndpoint}/payments/mock-checkout`, null, {
      params: { transactionCode },
    });
  }

  handleVnPayCallback(queryParams: any): Observable<{
    RspCode: string;
    Message: string;
    status: string;
    transactionCode: string;
    startDate?: string;
    expirationDate?: string;
    isIdempotentReplay?: boolean;
  }> {
    return this.http.get<any>(`${this.apiEndpoint}/payments/vnpay/callback`, {
      params: queryParams,
    });
  }
}
