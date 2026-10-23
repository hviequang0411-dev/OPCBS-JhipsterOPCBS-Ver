import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'specialization',
    data: { pageTitle: 'Specializations' },
    loadChildren: () => import('./specialization/specialization.routes'),
  },
  {
    path: 'doctor-profile',
    data: { pageTitle: 'Doctor Profiles' },
    loadChildren: () => import('./doctor-profile/doctor-profile.routes'),
  },
  {
    path: 'patient-profile',
    data: { pageTitle: 'Patient Profiles' },
    loadChildren: () => import('./patient-profile/patient-profile.routes'),
  },
  {
    path: 'availability-slot',
    data: { pageTitle: 'Availability Slots' },
    loadChildren: () => import('./availability-slot/availability-slot.routes'),
  },
  {
    path: 'appointment',
    data: { pageTitle: 'Appointments' },
    loadChildren: () => import('./appointment/appointment.routes'),
  },
  {
    path: 'user-management',
    data: { pageTitle: 'User Management' },
    loadChildren: () => import('./admin/user-management/user-management.routes'),
  },
  {
    path: 'authority',
    data: { pageTitle: 'Authorities' },
    loadChildren: () => import('./admin/authority/authority.routes'),
  },
  {
    path: 'doctor/dashboard',
    title: 'Tổng Quan Bác Sĩ',
    loadComponent: () => import('./doctor-profile/dashboard/doctor-dashboard.component').then(m => m.DoctorDashboardComponent),
  },
  {
    path: 'doctor/verification',
    title: 'Thẩm Định Hồ Sơ & Chứng Chỉ',
    loadComponent: () => import('./doctor-profile/verification/doctor-verification.component').then(m => m.DoctorVerificationComponent),
  },
  {
    path: 'doctor/patients',
    title: 'Doctor Patients',
    loadComponent: () => import('./doctor-profile/patients/doctor-patients.component').then(m => m.DoctorPatientsComponent),
  },
  {
    path: 'doctor/patients/:id',
    title: 'Patient Clinical History',
    loadComponent: () => import('./doctor-profile/patients/detail/doctor-patient-detail.component').then(m => m.DoctorPatientDetailComponent),
  },
  {
    path: 'doctor/patients/:id/view',
    title: 'Patient Clinical History',
    loadComponent: () => import('./doctor-profile/patients/detail/doctor-patient-detail.component').then(m => m.DoctorPatientDetailComponent),
  },
  {
    path: 'doctor/appointments',
    title: 'Appointments',
    loadComponent: () => import('./appointment/list/appointment').then(m => m.Appointment),
  },
  {
    path: 'doctor/appointments/:id',
    title: 'Appointment Detail',
    loadComponent: () => import('./appointment/detail/appointment-detail').then(m => m.AppointmentDetail),
  },
  {
    path: 'doctor/appointments/:id/view',
    title: 'Appointment Detail',
    loadComponent: () => import('./appointment/detail/appointment-detail').then(m => m.AppointmentDetail),
  },
  {
    path: 'patient/appointments',
    title: 'Appointments',
    loadComponent: () => import('./appointment/list/appointment').then(m => m.Appointment),
  },
  {
    path: 'patient/appointments/:id',
    title: 'Appointment Detail',
    loadComponent: () => import('./appointment/detail/appointment-detail').then(m => m.AppointmentDetail),
  },
  {
    path: 'patient/appointments/:id/view',
    title: 'Appointment Detail',
    loadComponent: () => import('./appointment/detail/appointment-detail').then(m => m.AppointmentDetail),
  },
  {
    path: 'patient/clinical-records',
    title: 'My Clinical Records',
    loadComponent: () => import('./appointment/clinical-records/patient-clinical-records.component').then(m => m.PatientClinicalRecordsComponent),
  },
  {
    path: 'doctor/treatment-packages',
    title: 'Quản Lý Gói Trị Liệu',
    loadComponent: () => import('./treatment-package/doctor/doctor-treatment-packages.component').then(m => m.DoctorTreatmentPackagesComponent),
  },
  {
    path: 'patient/treatment-packages',
    title: 'Gói Trị Liệu Của Tôi',
    loadComponent: () => import('./treatment-package/patient/patient-treatment-packages.component').then(m => m.PatientTreatmentPackagesComponent),
  },
  {
    path: 'patient/journal',
    title: 'Nhật Ký Cảm Xúc',
    loadComponent: () => import('./emotion-journal/patient/patient-journal.component').then(m => m.PatientJournalComponent),
  },
  {
    path: 'patient/psychometrics',
    title: 'Trắc Nghiệm Tâm Lý',
    loadComponent: () => import('./psychometrics/patient/catalog/psychometrics-catalog.component').then(m => m.PsychometricsCatalogComponent),
  },
  {
    path: 'patient/psychometrics/take/:id',
    title: 'Làm Bài Trắc Nghiệm',
    loadComponent: () => import('./psychometrics/patient/take-test/take-test.component').then(m => m.TakeTestComponent),
  },
  {
    path: 'patient/psychometrics/result/:id',
    title: 'Kết Quả Trắc Nghiệm Tâm Lý',
    loadComponent: () => import('./psychometrics/patient/result/test-result.component').then(m => m.TestResultComponent),
  },
  {
    path: 'doctor/psychometrics',
    title: 'Trắc Nghiệm Tâm Lý Bệnh Nhân',
    loadComponent: () => import('./psychometrics/doctor/doctor-psychometrics.component').then(m => m.DoctorPsychometricsComponent),
  },
  {
    path: 'notifications',
    title: 'Trung Tâm Thông Báo',
    loadComponent: () => import('./notification/notification-center.component').then(m => m.NotificationCenterComponent),
  },
  {
    path: 'doctor/subscriptions',
    title: 'Gói Dịch Vụ Bác Sĩ & Cổng VNPay',
    loadComponent: () => import('./payment/doctor-subscription.component').then(m => m.DoctorSubscriptionComponent),
  },
  {
    path: 'doctor/subscription/callback',
    title: 'Kết Quả Thanh Toán VNPay',
    loadComponent: () => import('./payment/doctor-subscription.component').then(m => m.DoctorSubscriptionComponent),
  },
  {
    path: 'doctor/subscriptions/callback',
    title: 'Kết Quả Thanh Toán VNPay',
    loadComponent: () => import('./payment/doctor-subscription.component').then(m => m.DoctorSubscriptionComponent),
  },
  {
    path: 'pricing',
    title: 'Bảng Giá Gói Dịch Vụ Bác Sĩ',
    loadComponent: () => import('./payment/doctor-subscription.component').then(m => m.DoctorSubscriptionComponent),
  },
  {
    path: 'messages',
    title: 'Hộp Thư Tư Vấn Trực Tuyến',
    loadComponent: () => import('./message/chat-inbox.component').then(m => m.ChatInboxComponent),
  },
  {
    path: 'blogs',
    title: 'Góc Nhìn Chuyên Gia & Blog Y Khoa',
    loadComponent: () => import('./blog/public/blog-catalog.component').then(m => m.BlogCatalogComponent),
  },
  {
    path: 'blogs/:id',
    title: 'Chi Tiết Bài Viết Chuyên Môn',
    loadComponent: () => import('./blog/public/blog-detail.component').then(m => m.BlogDetailComponent),
  },
  {
    path: 'doctor/blogs',
    title: 'Quản Lý Bài Viết Bác Sĩ',
    loadComponent: () => import('./blog/doctor/doctor-blogs.component').then(m => m.DoctorBlogsComponent),
  },
  {
    path: 'admin/blogs',
    title: 'Kiểm Duyệt & Xuất Bản Blog Y Khoa',
    loadComponent: () => import('./blog/admin/admin-blogs.component').then(m => m.AdminBlogsComponent),
  },
  // jhipster-needle-add-entity-route - JHipster will add entity modules routes here
];

export default routes;
