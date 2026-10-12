export interface IConversation {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  appointmentId?: number;
  treatmentPackageId?: number;
  status: 'OPEN' | 'CLOSED' | 'ARCHIVED';
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount?: number;
  createdDate?: string;
}

export interface IChatMessage {
  id?: number;
  conversationId: number;
  senderId?: number;
  senderName?: string;
  senderRole?: string;
  content: string;
  attachmentUrl?: string;
  attachmentType?: string;
  isRead?: boolean;
  createdDate?: string;
}
