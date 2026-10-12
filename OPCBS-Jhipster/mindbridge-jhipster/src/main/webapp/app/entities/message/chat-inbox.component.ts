import { Component, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MessageService } from './message.service';
import { IConversation, IChatMessage } from './message.model';
import { AccountService } from 'app/core/auth';
import { FormatMediumDatetimePipe } from 'app/shared/date';

@Component({
  selector: 'mb-chat-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FormatMediumDatetimePipe],
  templateUrl: './chat-inbox.component.html',
  styleUrls: ['./chat-inbox.component.scss'],
})
export class ChatInboxComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesScrollContainer') private messagesScrollContainer!: ElementRef;

  private readonly messageService = inject(MessageService);
  private readonly accountService = inject(AccountService);
  private readonly route = inject(ActivatedRoute);

  conversations = signal<IConversation[]>([]);
  activeConversation = signal<IConversation | null>(null);
  messages = signal<IChatMessage[]>([]);
  searchQuery = signal<string>('');
  activeFilter = signal<'all' | 'unread'>('all');
  newMessageText = '';
  isLoading = signal<boolean>(false);
  isSending = signal<boolean>(false);
  currentUser = signal<any>(null);

  private pollTimer: any = null;
  private shouldScrollBottom = false;

  ngOnInit(): void {
    this.accountService.identity().subscribe(acc => {
      this.currentUser.set(acc);
      this.loadConversations();
    });

    // Auto-poll new messages every 3 seconds
    this.pollTimer = setInterval(() => {
      if (this.activeConversation()) {
        this.refreshActiveMessages(false);
      }
    }, 3000);
  }

  ngOnDestroy(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollBottom) {
      this.scrollToBottom();
      this.shouldScrollBottom = false;
    }
  }

  loadConversations(): void {
    this.isLoading.set(true);
    this.messageService.getMyConversations().subscribe({
      next: data => {
        const convList = data || [];
        this.conversations.set(convList);
        this.isLoading.set(false);

        // Check query parameters
        const queryParams = this.route.snapshot.queryParamMap;
        const routeConvId = queryParams.get('convId');
        const targetDoctorId = queryParams.get('doctorId');
        const targetPatientId = queryParams.get('patientId');
        const targetDoctorName = queryParams.get('doctorName');
        const targetPatientName = queryParams.get('patientName');

        if (routeConvId) {
          const target = convList.find(c => c.id === Number(routeConvId));
          if (target) {
            this.selectConversation(target);
            return;
          }
        }

        if (targetDoctorId || targetPatientId) {
          const docId = targetDoctorId ? Number(targetDoctorId) : (this.isDoctor() ? this.currentUser()?.id : 2);
          const patId = targetPatientId ? Number(targetPatientId) : (!this.isDoctor() ? this.currentUser()?.id : 1);

          const existing = convList.find(c => c.doctorId === docId && c.patientId === patId);
          if (existing) {
            this.selectConversation(existing);
            return;
          } else {
            // Auto start conversation
            this.messageService.startConversation({
              doctorId: docId,
              patientId: patId,
              doctorName: targetDoctorName ?? undefined,
              patientName: targetPatientName ?? undefined,
              initialMessage: 'Xin chào, tôi muốn được tư vấn y tế cùng chuyên gia.'
            }).subscribe({
              next: createdConv => {
                this.conversations.update(list => [createdConv, ...list]);
                this.selectConversation(createdConv);
              }
            });
            return;
          }
        }

        if (convList.length > 0 && !this.activeConversation()) {
          this.selectConversation(convList[0]);
        }
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  selectConversation(conv: IConversation): void {
    this.activeConversation.set(conv);
    this.messageService.getMessages(conv.id).subscribe({
      next: msgs => {
        this.messages.set(msgs || []);
        this.shouldScrollBottom = true;
        conv.unreadCount = 0;
      },
    });
  }

  refreshActiveMessages(shouldScroll = false): void {
    const conv = this.activeConversation();
    if (!conv) return;

    this.messageService.getMessages(conv.id).subscribe({
      next: msgs => {
        if (msgs.length !== this.messages().length) {
          this.messages.set(msgs);
          if (shouldScroll) {
            this.shouldScrollBottom = true;
          }
        }
      },
    });
  }

  sendMessage(): void {
    if (!this.newMessageText.trim() || this.isSending()) return;
    const conv = this.activeConversation();
    if (!conv) return;

    const content = this.newMessageText.trim();
    this.newMessageText = '';
    this.isSending.set(true);

    this.messageService.sendMessage(conv.id, { content }).subscribe({
      next: newMsg => {
        this.messages.update(list => [...list, newMsg]);
        conv.lastMessage = content;
        conv.lastMessageTime = new Date().toISOString();
        this.shouldScrollBottom = true;
        this.isSending.set(false);
      },
      error: () => {
        this.isSending.set(false);
      },
    });
  }

  getFilteredConversations(): IConversation[] {
    let list = this.conversations();
    if (this.activeFilter() === 'unread') {
      list = list.filter(c => c.unreadCount && c.unreadCount > 0);
    }

    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return list;

    return list.filter(c =>
      (c.doctorName && c.doctorName.toLowerCase().includes(q)) ||
      (c.patientName && c.patientName.toLowerCase().includes(q)) ||
      (c.lastMessage && c.lastMessage.toLowerCase().includes(q))
    );
  }

  getInterlocutorName(conv: IConversation): string {
    const isDoc = this.isDoctor();
    return isDoc ? (conv.patientName || 'Bệnh nhân') : (conv.doctorName || 'Bác sĩ chuyên khoa');
  }

  getInterlocutorRole(conv: IConversation): string {
    const isDoc = this.isDoctor();
    return isDoc ? 'Thân chủ / Bệnh nhân' : 'Bác sĩ / Chuyên gia Tâm lý';
  }

  isDoctor(): boolean {
    return this.accountService.hasAnyAuthority('ROLE_DOCTOR');
  }

  isMyMessage(msg: IChatMessage): boolean {
    const acc = this.currentUser();
    if (!acc) return false;

    if (msg.senderId && acc.id && msg.senderId === acc.id) {
      return true;
    }

    const isDoc = this.isDoctor();
    if (msg.senderRole) {
      if (isDoc && msg.senderRole === 'ROLE_DOCTOR') return true;
      if (!isDoc && msg.senderRole === 'ROLE_PATIENT') return true;
    }

    if (msg.senderName) {
      const userDisplay = (acc.firstName ? `${acc.firstName} ${acc.lastName || ''}` : acc.login).trim();
      if (msg.senderName.toLowerCase().includes(acc.login.toLowerCase()) || msg.senderName.toLowerCase().includes(userDisplay.toLowerCase())) {
        return true;
      }
    }

    return false;
  }

  getInitials(name?: string): string {
    if (!name) return 'U';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return (parts[0]?.[0] ?? 'U').toUpperCase();
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesScrollContainer) {
        this.messagesScrollContainer.nativeElement.scrollTop =
          this.messagesScrollContainer.nativeElement.scrollHeight;
      }
    } catch {}
  }
}

