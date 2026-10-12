import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AccountService } from 'app/core/auth';
import { LoginService } from 'app/login/login.service';

@Component({
  selector: 'jhi-sidebar',
  standalone: true,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  imports: [CommonModule, RouterLink, RouterLinkActive],
})
export default class Sidebar {
  readonly account = inject(AccountService).account;

  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);

  isDoctor(): boolean {
    return this.account()?.authorities.includes('ROLE_DOCTOR') ?? false;
  }

  isPatient(): boolean {
    return this.account()?.authorities.includes('ROLE_PATIENT') ?? false;
  }

  isAdmin(): boolean {
    return this.account()?.authorities.includes('ROLE_ADMIN') ?? false;
  }

  getPortalTitle(): string {
    if (this.isAdmin()) return 'Cổng Quản Trị';
    if (this.isDoctor()) return 'Cổng Bác Sĩ';
    if (this.isPatient()) return 'Cổng Bệnh Nhân';
    return 'Cổng MindBridge';
  }

  getPortalIcon(): string {
    if (this.isAdmin()) return 'bi-shield-lock';
    if (this.isDoctor()) return 'bi-hospital';
    if (this.isPatient()) return 'bi-heart-pulse';
    return 'bi-grid';
  }

  getUserDisplayName(): string {
    const acc = this.account();
    if (!acc) return '';
    if (acc.firstName || acc.lastName) {
      return `${acc.firstName ?? ''} ${acc.lastName ?? ''}`.trim();
    }
    return acc.login;
  }

  getUserInitials(): string {
    const name = this.getUserDisplayName();
    if (!name) return 'U';
    const parts = name.split(' ').filter(p => p.length > 0);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  getRoleBadge(): { label: string; class: string } {
    if (this.isAdmin()) return { label: 'Quản Trị Viên', class: 'bg-dark' };
    if (this.isDoctor()) return { label: 'Bác Sĩ', class: 'bg-teal' };
    if (this.isPatient()) return { label: 'Bệnh Nhân', class: 'bg-success' };
    return { label: 'Thành Viên', class: 'bg-secondary' };
  }

  logout(): void {
    this.loginService.logout();
    this.router.navigate(['']);
  }
}
