import { Routes } from '@angular/router';
import { userRouteAccessService } from 'app/core/auth';

const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/admin-dashboard.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Tổng Quan Quản Trị Hệ Thống - MindBridge',
  },
  {
    path: 'doctor-verifications',
    loadComponent: () => import('./doctor-verification/admin-doctor-verification-list.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Duyệt Chứng Chỉ & Hồ Sơ Bác Sĩ - MindBridge Admin',
  },
  {
    path: 'doctor-verifications/:id',
    loadComponent: () => import('./doctor-verification/admin-doctor-verification-detail.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Chi Tiết Thẩm Định Bác Sĩ - MindBridge Admin',
  },
  {
    path: 'service-package',
    loadComponent: () => import('./service-package/service-package-list.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Gói Dịch Vụ Nền Tảng - MindBridge Admin',
  },
  {
    path: 'service-package/new',
    loadComponent: () => import('./service-package/service-package-edit.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Tạo Gói Dịch Vụ Mới - MindBridge Admin',
  },
  {
    path: 'service-package/:id/edit',
    loadComponent: () => import('./service-package/service-package-edit.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Chỉnh Sửa Gói Dịch Vụ - MindBridge Admin',
  },
  {
    path: 'service-package/:id/view',
    loadComponent: () => import('./service-package/service-package-detail.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Chi Tiết Gói Dịch Vụ - MindBridge Admin',
  },
  {
    path: 'psychometrics',
    loadComponent: () => import('./psychometrics/admin-psychometrics-list.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Đề Trắc Nghiệm Tâm Lý - MindBridge Admin',
  },
  {
    path: 'psychometrics/new',
    loadComponent: () => import('./psychometrics/admin-psychometrics-edit.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Tạo / Nhập Đề Trắc Nghiệm - MindBridge Admin',
  },
  {
    path: 'psychometrics/:id/edit',
    loadComponent: () => import('./psychometrics/admin-psychometrics-edit.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Chỉnh Sửa Đề Trắc Nghiệm - MindBridge Admin',
  },
  {
    path: 'psychometrics/:id/view',
    loadComponent: () => import('./psychometrics/admin-psychometrics-detail.component'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Chi Tiết Đề Trắc Nghiệm - MindBridge Admin',
  },
  {
    path: 'docs',
    loadComponent: () => import('./docs/docs'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'API',
  },
  {
    path: 'configuration',
    loadComponent: () => import('./configuration/configuration'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Configuration',
  },
  {
    path: 'health',
    loadComponent: () => import('./health/health'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Health Checks',
  },
  {
    path: 'logs',
    loadComponent: () => import('./logs/logs'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Logs',
  },
  {
    path: 'metrics',
    loadComponent: () => import('./metrics/metrics'),
    data: {
      authorities: ['ROLE_ADMIN'],
    },
    canActivate: [userRouteAccessService],
    title: 'Application Metrics',
  },
];

export default routes;
