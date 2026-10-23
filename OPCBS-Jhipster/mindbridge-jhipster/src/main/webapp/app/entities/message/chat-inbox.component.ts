import { Component, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MessageService } from './message.service';
import { IConversation, IChatMessage } from './message.model';
import { AccountService, StateStorageService } from 'app/core/auth';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { Client, StompSubscription, IMessage } from '@stomp/stompjs';

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
  private readonly stateStorageService = inject(StateStorageService);
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

  // WebSocket Connection States
  wsConnected = signal<boolean>(false);
  wsStatus = signal<'connected' | 'connecting' | 'disconnected' | 'fallback'>('connecting');
  sendErrorAlert = signal<string | null>(null);

  private stompClient: Client | null = null;
  private currentSubscription: StompSubscription | null = null;
  private ackSubscription: StompSubscription | null = null;
  private errorSubscription: StompSubscription | null = null;
  private currentLoadingConvId: number | null = null;
  private pendingTrackers = new Map<string, { timer: any; convId: number }>();
  private shouldScrollBottom = false;

  ngOnInit(): void {
    this.accountService.identity().subscribe(acc => {
      this.currentUser.set(acc);
      this.loadConversations();
    });

    // Initialize real-time WebSocket connection
    this.initWebSocket();
  }

  ngOnDestroy(): void {
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }
    if (this.ackSubscription) {
      this.ackSubscription.unsubscribe();
      this.ackSubscription = null;
    }
    if (this.errorSubscription) {
      this.errorSubscription.unsubscribe();
      this.errorSubscription = null;
    }
    this.pendingTrackers.forEach(t => clearTimeout(t.timer));
    this.pendingTrackers.clear();
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollBottom) {
      this.scrollToBottom();
      this.shouldScrollBottom = false;
    }
  }

  /**
   * Initializes the STOMP Client over native WebSocket without sending JWT in query params.
   * Authentication is passed via STOMP CONNECT headers and HttpOnly cookies via reverse proxy.
   */
  initWebSocket(): void {
    const token = this.stateStorageService.getAuthenticationToken();
    const wsUrl = this.getWebSocketUrl();

    this.wsStatus.set('connecting');

    this.stompClient = new Client({
      brokerURL: wsUrl,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {
        // Safe logging: never output JWT tokens or sensitive messages
      },
      beforeConnect: () => {
        // Update token before every reconnection attempt to ensure freshness
        const currentToken = this.stateStorageService.getAuthenticationToken();
        if (this.stompClient) {
          this.stompClient.connectHeaders = currentToken ? { Authorization: `Bearer ${currentToken}` } : {};
        }
      },
      onConnect: () => {
        this.wsConnected.set(true);
        this.wsStatus.set('connected');

        // 1. Subscribe to personal queue for message ACKs (/user/queue/ack)
        if (this.stompClient) {
          if (this.ackSubscription) {
            this.ackSubscription.unsubscribe();
          }
          this.ackSubscription = this.stompClient.subscribe('/user/queue/ack', (msg: IMessage) => {
            try {
              const ack = JSON.parse(msg.body);
              if (ack && ack.clientMessageId) {
                const tracker = this.pendingTrackers.get(ack.clientMessageId);
                if (tracker) {
                  clearTimeout(tracker.timer);
                  this.pendingTrackers.delete(ack.clientMessageId);
                }
                const targetConvId = ack.conversationId || tracker?.convId;
                if (this.activeConversation()?.id === targetConvId) {
                  this.messages.update(list => {
                    const idx = list.findIndex(m => m.clientMessageId === ack.clientMessageId);
                    if (idx >= 0) {
                      list[idx].id = ack.messageId;
                      list[idx].status = 'sent';
                      if (ack.createdDate) {
                        list[idx].createdDate = ack.createdDate;
                      }
                      return [...list];
                    }
                    return list;
                  });
                  this.isSending.set(false);
                }
              }
            } catch {}
          });

          // 2. Subscribe to personal queue for errors (/user/queue/errors)
          if (this.errorSubscription) {
            this.errorSubscription.unsubscribe();
          }
          this.errorSubscription = this.stompClient.subscribe('/user/queue/errors', (msg: IMessage) => {
            try {
              const err = JSON.parse(msg.body);
              if (err && err.clientMessageId) {
                const tracker = this.pendingTrackers.get(err.clientMessageId);
                if (tracker) {
                  clearTimeout(tracker.timer);
                  this.pendingTrackers.delete(err.clientMessageId);
                }
                const targetConvId = tracker?.convId;
                if (this.activeConversation()?.id === targetConvId) {
                  this.messages.update(list => {
                    const idx = list.findIndex(m => m.clientMessageId === err.clientMessageId);
                    if (idx >= 0) {
                      list[idx].status = 'failed';
                      return [...list];
                    }
                    return list;
                  });
                }
              }
              if (err.code === 'SESSION_EXPIRED' || err.code === 'SESSION_REVOKED') {
                this.wsConnected.set(false);
                this.wsStatus.set('disconnected');
                this.sendErrorAlert.set(err.message || 'Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
              } else {
                this.sendErrorAlert.set(err.message || 'Lỗi gửi tin nhắn qua WebSocket.');
                setTimeout(() => this.sendErrorAlert.set(null), 5000);
              }
              this.isSending.set(false);
            } catch {}
          });
        }

        // 3. Automatically re-subscribe to current conversation and synchronize missed messages
        const currentConv = this.activeConversation();
        if (currentConv) {
          this.subscribeToConversation(currentConv.id);
          this.syncMissedMessages(currentConv.id);
        }
      },
      onDisconnect: () => {
        this.wsConnected.set(false);
        this.wsStatus.set('disconnected');
      },
      onStompError: () => {
        // Fallback gracefully to REST when STOMP connection fails or authentication is rejected
        this.wsConnected.set(false);
        this.wsStatus.set('fallback');
      },
      onWebSocketClose: (evt?: any) => {
        this.wsConnected.set(false);
        if (evt && (evt.code === 4001 || evt.code === 4002)) {
          this.wsStatus.set('disconnected');
          const msg = evt.code === 4001
            ? 'Phiên đăng nhập đã hết hạn. Vui lòng làm mới trang hoặc đăng nhập lại.'
            : 'Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.';
          this.sendErrorAlert.set(msg);
        } else if (this.wsStatus() !== 'fallback') {
          this.wsStatus.set('disconnected');
        }
      },
    });

    this.stompClient.activate();
  }

  private getWebSocketUrl(): string {
    const loc = window.location;
    const protocol = loc.protocol === 'https:' ? 'wss:' : 'ws:';
    // Connects via reverse proxy on same domain / port (/ws)
    return `${protocol}//${loc.host}/ws`;
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
          const docId = targetDoctorId ? Number(targetDoctorId) : (this.isDoctor() ? this.currentUser()?.id : 1);
          const patId = targetPatientId ? Number(targetPatientId) : (!this.isDoctor() ? this.currentUser()?.id : 1);

          const existing = convList.find(c => {
            if (targetDoctorId && c.doctorId === Number(targetDoctorId)) return true;
            if (targetPatientId && c.patientId === Number(targetPatientId)) return true;
            return c.doctorId === docId && c.patientId === patId;
          });

          if (existing) {
            this.selectConversation(existing);
            return;
          } else {
            // Auto start or resolve conversation
            this.messageService.startConversation({
              doctorId: docId,
              patientId: patId,
              doctorName: targetDoctorName ?? undefined,
              patientName: targetPatientName ?? undefined,
              initialMessage: 'Xin chào, tôi muốn được tư vấn y tế cùng chuyên gia.'
            }).subscribe({
              next: createdConv => {
                this.conversations.update(list => {
                  const idx = list.findIndex(c => c.id === createdConv.id);
                  if (idx >= 0) {
                    list[idx] = createdConv;
                    return [...list];
                  }
                  return [createdConv, ...list];
                });
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

  getMyDisplayName(): string {
    const acc = this.currentUser();
    if (!acc) return 'Tôi';
    if (acc.firstName || acc.lastName) {
      return `${acc.firstName || ''} ${acc.lastName || ''}`.trim();
    }
    return acc.login || 'Tôi';
  }

  /**
   * Selects a conversation:
   * 1. Unsubscribes from previous conversation topic.
   * 2. Subscribes to new conversation topic.
   * 3. Loads message history via REST API without race condition overwrite.
   * NOTE: Does NOT clear active pendingTrackers for other conversations to prevent losing pending status.
   */
  selectConversation(conv: IConversation): void {
    this.activeConversation.set(conv);
    this.currentLoadingConvId = conv.id;

    // 1. Unsubscribe previous conversation topic
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }

    this.isSending.set(false);

    // 2. Subscribe to current conversation topic over WebSocket first
    this.subscribeToConversation(conv.id);

    // 3. Fetch message history via REST API with race-condition guard
    this.messageService.getMessages(conv.id).subscribe({
      next: msgs => {
        // Discard late response if user already switched to another conversation
        if (this.currentLoadingConvId !== conv.id || this.activeConversation()?.id !== conv.id) {
          return;
        }

        // Merge REST history with any pending or real-time messages for this conversation
        this.messages.update(currentList => {
          const map = new Map<string, IChatMessage>();

          // Add all REST messages first
          for (const m of msgs || []) {
            if (m.id != null) {
              map.set(`id_${m.id}`, { ...m, status: 'sent' });
            }
          }

          // Merge current pending or in-flight messages belonging to this conversation
          for (const cur of currentList) {
            if (cur.conversationId !== conv.id) continue;
            if (cur.clientMessageId != null) {
              // Only keep pending message if not already acknowledged in REST history
              const hasMatchingInRest = Array.from(map.values()).some(
                m => m.clientMessageId === cur.clientMessageId
              );
              if (!hasMatchingInRest) {
                map.set(`cm_${cur.clientMessageId}`, cur);
              }
            } else if (cur.id != null && !map.has(`id_${cur.id}`)) {
              map.set(`id_${cur.id}`, cur);
            }
          }

          // Sort chronologically
          const merged = Array.from(map.values()).sort((a, b) => {
            const timeA = new Date(a.createdDate || 0).getTime();
            const timeB = new Date(b.createdDate || 0).getTime();
            return timeA - timeB;
          });

          return merged;
        });

        this.shouldScrollBottom = true;
        conv.unreadCount = 0;
      },
    });
  }

  /**
   * Synchronizes missed messages from REST API after WebSocket reconnection without duplicates.
   */
  private syncMissedMessages(conversationId: number): void {
    const currentList = this.messages();
    let maxId = 0;
    for (const m of currentList) {
      if (m.conversationId === conversationId && m.id && m.id > maxId) {
        maxId = m.id;
      }
    }

    this.messageService.getMessages(conversationId, maxId > 0 ? maxId : undefined).subscribe({
      next: newMsgs => {
        if (this.activeConversation()?.id !== conversationId) {
          return;
        }
        if (!newMsgs || newMsgs.length === 0) {
          return;
        }

        this.messages.update(list => {
          const map = new Map<string, IChatMessage>();
          for (const m of list) {
            if (m.id != null) {
              map.set(`id_${m.id}`, m);
            } else if (m.clientMessageId != null) {
              map.set(`cm_${m.clientMessageId}`, m);
            }
          }

          for (const m of newMsgs) {
            if (m.clientMessageId && map.has(`cm_${m.clientMessageId}`)) {
              map.delete(`cm_${m.clientMessageId}`);
            }
            if (m.id != null) {
              map.set(`id_${m.id}`, { ...m, status: 'sent' });
            }
          }

          return Array.from(map.values()).sort(
            (a, b) => new Date(a.createdDate || 0).getTime() - new Date(b.createdDate || 0).getTime()
          );
        });

        this.shouldScrollBottom = true;
      },
      error: () => {
        // Silent sync failure
      },
    });
  }

  private subscribeToConversation(conversationId: number): void {
    if (!this.stompClient || !this.stompClient.connected) {
      return;
    }

    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }

    const topic = `/topic/conversations/${conversationId}`;
    this.currentSubscription = this.stompClient.subscribe(topic, (message: IMessage) => {
      try {
        const incomingMsg: IChatMessage = JSON.parse(message.body);
        if (incomingMsg && incomingMsg.conversationId === conversationId) {
          this.messages.update(list => {
            // Check if there is a pending message sent by me with matching clientMessageId or identical content
            const pendingIdx = list.findIndex(m =>
              (incomingMsg.clientMessageId != null && m.clientMessageId === incomingMsg.clientMessageId) ||
              (incomingMsg.id != null && m.id === incomingMsg.id) ||
              (m.status === 'sending' && m.content === incomingMsg.content && this.isMyMessage(incomingMsg))
            );

            if (pendingIdx >= 0) {
              const pending = list[pendingIdx];
              if (pending.clientMessageId) {
                const tracker = this.pendingTrackers.get(pending.clientMessageId);
                if (tracker) {
                  clearTimeout(tracker.timer);
                  this.pendingTrackers.delete(pending.clientMessageId);
                }
              }
              list[pendingIdx] = { ...incomingMsg, status: 'sent' };
              return [...list];
            }

            if (list.some(m => m.id === incomingMsg.id)) {
              return list;
            }

            return [...list, { ...incomingMsg, status: 'sent' }];
          });

          this.isSending.set(false);

          // Update active conversation preview
          const active = this.activeConversation();
          if (active && active.id === conversationId) {
            active.lastMessage = incomingMsg.content;
            active.lastMessageTime = incomingMsg.createdDate;
          }

          // Update list preview
          this.conversations.update(list => {
            const idx = list.findIndex(c => c.id === conversationId);
            if (idx >= 0) {
              list[idx].lastMessage = incomingMsg.content;
              list[idx].lastMessageTime = incomingMsg.createdDate;
            }
            return [...list];
          });

          this.shouldScrollBottom = true;
        }
      } catch (err) {
        // ignore JSON parse error
      }
    });
  }

  /**
   * Sends a message:
   * Uses STOMP over WebSocket when connected with clientMessageId acknowledgement protocol.
   * Falls back gracefully to REST API when WebSocket is disconnected.
   * Displays message as pending 'sending', confirms on server ACK/broadcast, or marks 'failed' on error.
   */
  sendMessage(): void {
    if (!this.newMessageText.trim() || this.isSending()) return;
    const conv = this.activeConversation();
    if (!conv) return;

    const content = this.newMessageText.trim();
    const clientMessageId = 'cm_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);

    this.newMessageText = '';
    this.isSending.set(true);

    const pendingMsg: IChatMessage = {
      conversationId: conv.id,
      senderId: this.currentUser()?.id,
      senderName: this.getMyDisplayName(),
      senderRole: this.isDoctor() ? 'ROLE_DOCTOR' : 'ROLE_PATIENT',
      content,
      createdDate: new Date().toISOString(),
      clientMessageId,
      status: 'sending',
    };

    // Append pending message to list immediately
    this.messages.update(list => [...list, pendingMsg]);
    this.shouldScrollBottom = true;

    if (this.stompClient && this.stompClient.connected) {
      try {
        this.stompClient.publish({
          destination: `/app/conversations/${conv.id}/send`,
          body: JSON.stringify({ content, clientMessageId }),
        });

        // Set timeout 10s: if no ack and no broadcast received, mark as failed
        const timer = setTimeout(() => {
          this.markMessageFailed(clientMessageId, conv.id);
        }, 10000);
        this.pendingTrackers.set(clientMessageId, { timer, convId: conv.id });
      } catch (err) {
        this.sendViaRest(conv, content, clientMessageId);
      }
    } else {
      // Fallback via REST
      this.sendViaRest(conv, content, clientMessageId);
    }
  }

  private sendViaRest(conv: IConversation, content: string, clientMessageId: string): void {
    // Transmit clientMessageId via REST fallback for backend database idempotency deduplication
    this.messageService.sendMessage(conv.id, { content, clientMessageId }).subscribe({
      next: newMsg => {
        const tracker = this.pendingTrackers.get(clientMessageId);
        if (tracker) {
          clearTimeout(tracker.timer);
          this.pendingTrackers.delete(clientMessageId);
        }

        if (this.activeConversation()?.id === conv.id) {
          this.messages.update(list => {
            const idx = list.findIndex(m => m.clientMessageId === clientMessageId);
            if (idx >= 0) {
              list[idx].id = newMsg.id;
              list[idx].status = 'sent';
              list[idx].createdDate = newMsg.createdDate;
              return [...list];
            }
            if (list.some(m => m.id === newMsg.id)) return list;
            return [...list, { ...newMsg, status: 'sent' }];
          });
          this.shouldScrollBottom = true;
        }

        conv.lastMessage = content;
        conv.lastMessageTime = newMsg.createdDate || new Date().toISOString();
        this.isSending.set(false);
      },
      error: () => {
        this.markMessageFailed(clientMessageId, conv.id);
      },
    });
  }

  private markMessageFailed(clientMessageId: string, convId: number): void {
    const tracker = this.pendingTrackers.get(clientMessageId);
    if (tracker) {
      clearTimeout(tracker.timer);
      this.pendingTrackers.delete(clientMessageId);
    }

    if (this.activeConversation()?.id === convId) {
      this.messages.update(list => {
        const idx = list.findIndex(m => m.clientMessageId === clientMessageId);
        if (idx >= 0) {
          list[idx].status = 'failed';
          return [...list];
        }
        return list;
      });
    }
    this.isSending.set(false);
  }

  /**
   * Retries sending a failed message:
   * STRICT IDEMPOTENCY REQUIREMENT:
   * Preserves exact clientMessageId and conversationId without generating a new ID or deleting and re-creating pending record.
   */
  retryMessage(failedMsg: IChatMessage): void {
    if (!failedMsg || !failedMsg.content || !failedMsg.clientMessageId) return;
    const conv = this.activeConversation();
    if (!conv || conv.id !== failedMsg.conversationId) return;

    const clientMessageId = failedMsg.clientMessageId;
    const content = failedMsg.content;

    // 1. Keep same record, change status back to 'sending'
    this.messages.update(list => {
      const idx = list.findIndex(m => m.clientMessageId === clientMessageId);
      if (idx >= 0) {
        list[idx].status = 'sending';
        return [...list];
      }
      return list;
    });
    this.isSending.set(true);

    // 2. Retry over WebSocket if connected, otherwise via REST fallback
    if (this.stompClient && this.stompClient.connected) {
      try {
        this.stompClient.publish({
          destination: `/app/conversations/${conv.id}/send`,
          body: JSON.stringify({ content, clientMessageId }),
        });

        const timer = setTimeout(() => {
          this.markMessageFailed(clientMessageId, conv.id);
        }, 10000);
        this.pendingTrackers.set(clientMessageId, { timer, convId: conv.id });
      } catch (err) {
        this.sendViaRest(conv, content, clientMessageId);
      }
    } else {
      this.sendViaRest(conv, content, clientMessageId);
    }
  }

  reconnectWebSocket(): void {
    if (this.stompClient) {
      this.stompClient.deactivate().then(() => {
        this.initWebSocket();
      });
    } else {
      this.initWebSocket();
    }
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
