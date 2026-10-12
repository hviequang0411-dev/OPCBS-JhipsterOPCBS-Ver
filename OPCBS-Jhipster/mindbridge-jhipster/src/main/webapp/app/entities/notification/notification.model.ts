export interface INotification {
  id: number;
  userLogin: string;
  userId?: number;
  title: string;
  message: string;
  notificationType?: string;
  type?: string;
  relatedEntityId?: number;
  relatedEntityType?: string;
  actionUrl?: string;
  isRead: boolean;
  readAt?: string;
  createdDate?: string;
}
