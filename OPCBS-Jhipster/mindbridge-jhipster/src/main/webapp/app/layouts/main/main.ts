import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AccountService } from 'app/core/auth';
import Footer from '../footer/footer';
import PageRibbon from '../profiles/page-ribbon';
import Sidebar from '../sidebar/sidebar';

@Component({
  selector: 'jhi-main',
  templateUrl: './main.html',
  styleUrl: './main.scss',
  imports: [CommonModule, RouterOutlet, Footer, PageRibbon, Sidebar],
})
export default class Main implements OnInit {
  readonly currentUrl = signal<string>('');
  readonly account = inject(AccountService).account;

  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);

  ngOnInit(): void {
    // try to log in automatically
    this.accountService.identity().subscribe();

    this.currentUrl.set(this.router.url);
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: any) => {
      this.currentUrl.set(event.urlAfterRedirects || event.url || '');
    });
  }

  isAuthPage(): boolean {
    const url = (this.currentUrl() || '').split('?')[0].toLowerCase();
    return (
      url === '/login' ||
      url.startsWith('/login') ||
      url.startsWith('/account/register') ||
      url.startsWith('/account/register-doctor') ||
      url.startsWith('/account/verify-otp') ||
      url.startsWith('/account/reset') ||
      url.startsWith('/account/activate')
    );
  }

  isDashboardPage(): boolean {
    if (this.isAuthPage()) return false;
    const url = (this.currentUrl() || '').split('?')[0].toLowerCase();

    // Check if user is logged in
    const isAuth = !!this.account();
    if (!isAuth) return false;

    // Public marketing and browsing pages keep the public header & footer
    if (url === '/' || url === '') return false;
    if (url === '/doctor-profile' || (url.startsWith('/doctor-profile/') && url.endsWith('/view'))) return false;
    if (url === '/specialization' || (url.startsWith('/specialization/') && url.endsWith('/view'))) return false;
    if (url.startsWith('/appointment/book')) return false;

    // Functional SaaS dashboard pages use sidebar without public header
    const dashboardPrefixes = [
      '/doctor',
      '/patient',
      '/availability-slot',
      '/appointment',
      '/patient-profile',
      '/doctor-profile/new',
      '/doctor-profile/',
      '/specialization/new',
      '/specialization/',
      '/user-management',
      '/authority',
      '/admin',
      '/notifications',
      '/messages',
      '/pricing',
      '/account/settings',
      '/account/password',
    ];

    return dashboardPrefixes.some(prefix => url.startsWith(prefix));
  }
}
