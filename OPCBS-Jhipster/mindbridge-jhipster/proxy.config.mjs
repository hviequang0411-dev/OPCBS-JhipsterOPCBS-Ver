const backendHost = '127.0.0.1';

/**
 * Multi-Microservices Proxy Configuration for OPCBS
 * Routing table connecting Vite frontend (4200/9000) to the 8 domain microservices.
 *
 * @type {import('vite').CommonServerOptions['proxy']}
 */
export default {
  // 1. Auth Service (8081) - Authentication, OTP, Registration, User Management
  '^/api/(authenticate|register|register-doctor|account|users|authorities|admin/users)': {
    target: `http://${backendHost}:8081`,
    xfwd: true,
  },

  // 2. Specific nested domain routes (Journals & Psychometrics under patient ID)
  '^/api/doctor/patients/[0-9]+/shared-journals': {
    target: `http://${backendHost}:8084`,
    xfwd: true,
  },
  '^/api/doctor/patients/[0-9]+/psychometrics': {
    target: `http://${backendHost}:8085`,
    xfwd: true,
  },

  // 3. Clinical Service (8083) - Patient Records, Doctor Patients, Clinical Consultation Notes
  '^/api/(patient-profiles|doctor/my-patients|doctor/patients|patient/my-clinical-records)': {
    target: `http://${backendHost}:8083`,
    xfwd: true,
  },

  // 4. Treatment Service (8084) - Treatment Packages, Mood Emotion Journals
  '^/api/(doctor/treatment-packages|patient/my-treatment-packages|treatment-packages|patient/journals)': {
    target: `http://${backendHost}:8084`,
    xfwd: true,
  },

  // 5. Psychometric Service (8085) - Psychometric Tests (DASS-21, PHQ-9, GAD-7) & Submissions
  '^/api/(psychometrics|patient/psychometrics|doctor/psychometrics)': {
    target: `http://${backendHost}:8085`,
    xfwd: true,
  },

  // 6. Notification Service (8086) - In-app Notifications & Alerts
  '^/api/notifications': {
    target: `http://${backendHost}:8086`,
    xfwd: true,
  },

  // 7. Payment Service (8087) - Platform Subscriptions, VNPay Sandbox, Transactions
  '^/api/(payments|service-packages|doctor-subscriptions)': {
    target: `http://${backendHost}:8087`,
    xfwd: true,
  },

  // 8. Message Service (8088) - Doctor-Patient Secure Messaging & Conversations
  '^/api/conversations': {
    target: `http://${backendHost}:8088`,
    xfwd: true,
  },

  // 9. Booking Service (8082) - Doctor Profiles, Slots, Appointments, Specializations, Doctor Dashboard, Certificate Uploads, Cloud Storage, Verification, Avatar Upload
  '^/api/(doctor-profiles|specializations|availability-slots|appointments|doctor/my-slots|doctor/slots|doctor/appointments|doctor/dashboard|doctor/upload-avatar|doctor/upload-certificate|doctor/upload-certificate-cloud|doctor/upload-certificate-base64|doctor/certificates|doctor/verification|doctor/submit-verification|patient/appointments|patient/my-appointments)': {
    target: `http://${backendHost}:8082`,
    xfwd: true,
  },

  // Fallback management & docs to Auth Service (8081)
  '^/(management|v3/api-docs|services)': {
    target: `http://${backendHost}:8081`,
    xfwd: true,
  },
};
