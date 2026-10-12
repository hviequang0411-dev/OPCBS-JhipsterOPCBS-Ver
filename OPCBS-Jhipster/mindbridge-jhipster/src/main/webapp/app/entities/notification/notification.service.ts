import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { INotification } from './notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = 'api/notifications';

  unreadCount = signal<number>(0);

  loadUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.resourceUrl}/unread-count`).pipe(
      tap(res => this.unreadCount.set(res.unreadCount || 0))
    );
  }

  getNotifications(unreadOnly = false): Observable<INotification[]> {
    return this.http.get<INotification[]>(this.resourceUrl, {
      params: { unreadOnly: unreadOnly.toString() },
    });
  }

  markAsRead(id: number): Observable<INotification> {
    return this.http.put<INotification>(`${this.resourceUrl}/${id}/read`, {}).pipe(
      tap(() => {
        this.unreadCount.update(c => Math.max(0, c - 1));
      })
    );
  }

  markAllAsRead(): Observable<{ success: boolean; updatedCount: number }> {
    return this.http.put<{ success: boolean; updatedCount: number }>(`${this.resourceUrl}/read-all`, {}).pipe(
      tap(() => {
        this.unreadCount.set(0);
      })
    );
  }
}
