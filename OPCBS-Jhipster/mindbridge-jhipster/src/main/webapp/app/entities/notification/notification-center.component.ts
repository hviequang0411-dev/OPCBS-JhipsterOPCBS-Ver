import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService } from './notification.service';
import { INotification } from './notification.model';
import { FormatMediumDatetimePipe } from 'app/shared/date';

type NotificationFilter = 'ALL' | 'UNREAD' | 'APPOINTMENT' | 'TREATMENT' | 'CLINICAL';

@Component({
  selector: 'mb-notification-center',
  standalone: true,
  imports: [CommonModule, RouterModule, FormatMediumDatetimePipe],
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.scss'],
})
export class NotificationCenterComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);

  allNotifications = signal<INotification[]>([]);
  isLoading = signal<boolean>(false);
  activeTab = signal<NotificationFilter>('ALL');

  unreadCount = computed(() => this.allNotifications().filter(n => !n.isRead).length);

  filteredNotifications = computed(() => {
    const list = this.allNotifications();
    const tab = this.activeTab();
    if (tab === 'UNREAD') {
      return list.filter(n => !n.isRead);
    }
    if (tab === 'APPOINTMENT') {
      return list.filter(n => (n.notificationType || n.type || '').toUpperCase().includes('APPOINTMENT'));
    }
    if (tab === 'TREATMENT') {
      return list.filter(n => (n.notificationType || n.type || '').toUpperCase().includes('TREATMENT') || (n.notificationType || n.type || '').toUpperCase().includes('PACKAGE'));
    }
    if (tab === 'CLINICAL') {
      return list.filter(n => (n.notificationType || n.type || '').toUpperCase().includes('CLINICAL') || (n.notificationType || n.type || '').toUpperCase().includes('CONSULTATION') || (n.notificationType || n.type || '').toUpperCase().includes('NOTE'));
    }
    return list;
  });

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.isLoading.set(true);
    this.notificationService.getNotifications(false).subscribe({
      next: data => {
        this.allNotifications.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
    this.notificationService.loadUnreadCount().subscribe();
  }

  switchTab(tab: NotificationFilter): void {
    this.activeTab.set(tab);
  }

  markAsRead(item: INotification, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (item.isRead) return;

    this.notificationService.markAsRead(item.id).subscribe({
      next: () => {
        item.isRead = true;
        this.allNotifications.update(list => [...list]);
      },
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.allNotifications.update(list =>
          list.map(n => ({ ...n, isRead: true }))
        );
      },
    });
  }

  getTypeMeta(notification: INotification): { icon: string; bg: string; color: string; label: string } {
    const rawType = (notification.notificationType || notification.type || '').toUpperCase();
    
    if (rawType.includes('APPOINTMENT') || rawType.includes('BOOKED') || rawType.includes('ACCEPTED')) {
      return { icon: 'bi-calendar-check-fill', bg: 'bg-emerald-subtle text-emerald', color: '#0d9488', label: 'Lịch Hẹn' };
    }
    if (rawType.includes('TREATMENT') || rawType.includes('PACKAGE')) {
      return { icon: 'bi-box2-heart-fill', bg: 'bg-teal-subtle text-teal', color: '#0f766e', label: 'Gói Trị Liệu' };
    }
    if (rawType.includes('CLINICAL') || rawType.includes('NOTE') || rawType.includes('CONSULTATION') || rawType.includes('RECORD')) {
      return { icon: 'bi-journal-medical', bg: 'bg-indigo-subtle text-indigo', color: '#4f46e5', label: 'Hồ Sơ Bệnh Án' };
    }
    if (rawType.includes('CANCEL') || rawType.includes('REJECT')) {
      return { icon: 'bi-calendar-x-fill', bg: 'bg-rose-subtle text-rose', color: '#e11d48', label: 'Lịch Hủy' };
    }
    if (rawType.includes('PAYMENT') || rawType.includes('SUBSCRIPTION')) {
      return { icon: 'bi-cash-stack', bg: 'bg-amber-subtle text-amber', color: '#d97706', label: 'Thanh Toán' };
    }
    if (rawType.includes('PSYCHOMETRIC')) {
      return { icon: 'bi-clipboard2-pulse-fill', bg: 'bg-purple-subtle text-purple', color: '#9333ea', label: 'Trắc Nghiệm' };
    }
    return { icon: 'bi-bell-fill', bg: 'bg-slate-subtle text-slate', color: '#64748b', label: 'Hệ Thống' };
  }

  getTimeAgo(dateStr?: string): string {
    if (!dateStr) return '';
    const now = new Date();
    const date = new Date(dateStr);
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
    if (diff < 60) return 'Vừa xong';
    if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
    if (diff < 604800) return `${Math.floor(diff / 86400)} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  }
}
