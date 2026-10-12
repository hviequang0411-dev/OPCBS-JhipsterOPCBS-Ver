import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IConversation, IChatMessage } from './message.model';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = 'api/conversations';

  getMyConversations(): Observable<IConversation[]> {
    return this.http.get<IConversation[]>(this.resourceUrl);
  }

  getConversation(id: number): Observable<IConversation> {
    return this.http.get<IConversation>(`${this.resourceUrl}/${id}`);
  }

  getMessages(conversationId: number): Observable<IChatMessage[]> {
    return this.http.get<IChatMessage[]>(`${this.resourceUrl}/${conversationId}/messages`);
  }

  sendMessage(conversationId: number, message: Partial<IChatMessage>): Observable<IChatMessage> {
    return this.http.post<IChatMessage>(`${this.resourceUrl}/${conversationId}/messages`, message);
  }

  startConversation(req: {
    patientId: number;
    doctorId: number;
    patientName?: string;
    doctorName?: string;
    appointmentId?: number;
    treatmentPackageId?: number;
    initialMessage?: string;
  }): Observable<IConversation> {
    return this.http.post<IConversation>(`${this.resourceUrl}/start`, req);
  }

  markAsRead(conversationId: number): Observable<void> {
    return this.http.put<void>(`${this.resourceUrl}/${conversationId}/read`, {});
  }
}
