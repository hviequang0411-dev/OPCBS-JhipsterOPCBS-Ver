import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IServicePackage, IDoctorSubscription } from './payment.model';

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
}
