import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbCollapse } from '@ng-bootstrap/ng-bootstrap/collapse';
import { NgbDropdown, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { environment } from 'environments/environment';

import { AccountService } from 'app/core/auth';
import { loadNavbarItems } from 'app/core/microfrontend';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { LoginService } from 'app/login/login.service';
import { HasAnyAuthorityDirective } from 'app/shared/auth';

import NavbarItem from './navbar-item.model';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    FontAwesomeModule,
    NgbCollapse,
    NgbDropdown,
    NgbDropdownMenu,
    NgbDropdownToggle,
    HasAnyAuthorityDirective,
  ],
})
export default class Navbar implements OnInit {
  readonly inProduction = signal(true);
  readonly isNavbarCollapsed = signal(true);
  readonly openAPIEnabled = signal(false);
  readonly version: string;
  readonly account = inject(AccountService).account;
  opcbsEntityNavbarItems = signal<NavbarItem[] | null>(null);

  private readonly loginService = inject(LoginService);
  private readonly accountService = inject(AccountService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    } else {
      this.version = '';
    }

    effect(() => {
      if (this.accountService.account()) {
        this.loadMicrofrontendsEntities();
      }
    });
  }

  ngOnInit(): void {
    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction.set(profileInfo.inProduction ?? true);
      this.openAPIEnabled.set(profileInfo.openAPIEnabled ?? false);
    });
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  isDoctor(): boolean {
    return this.account()?.authorities.includes('ROLE_DOCTOR') ?? false;
  }

  isPatient(): boolean {
    return this.account()?.authorities.includes('ROLE_PATIENT') ?? false;
  }

  isAdmin(): boolean {
    return this.account()?.authorities.includes('ROLE_ADMIN') ?? false;
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

  getPrimaryRoleBadge(): { label: string; class: string } {
    if (this.isAdmin()) return { label: 'Admin', class: 'bg-dark' };
    if (this.isDoctor()) return { label: 'Bác Sĩ', class: 'bg-teal' };
    if (this.isPatient()) return { label: 'Bệnh Nhân', class: 'bg-success' };
    return { label: 'Thành Viên', class: 'bg-secondary' };
  }

  loadMicrofrontendsEntities(): void {
    // Lazy load microfrontend entities.
    loadNavbarItems('opcbs').then(
      items => {
        this.opcbsEntityNavbarItems.set(items);
      },
      (error: unknown) => {
        // eslint-disable-next-line no-console
        console.log('Error loading opcbs entities', error);
      },
    );
  }
}
