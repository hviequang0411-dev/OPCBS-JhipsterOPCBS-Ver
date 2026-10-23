export interface IDoctorVerification {
  id?: number;
  doctorProfileId?: number;
  doctorId?: number;
  doctorName?: string;
  avatarUrl?: string;
  specialization?: string;
  licenseNumber?: string;
  experienceYears?: number;
  education?: string;
  submittedAt?: string;
  status?: string; // 'DRAFT' | 'PENDING' | 'SUBMITTED' | 'REQUIRES_ADDITIONAL_INFO' | 'APPROVED' | 'REJECTED'
  notes?: string;
  certificateUrl?: string;
  certificatePublicId?: string;
  certificateFileName?: string;
  certificateContentType?: string;
  certificateUploadedAt?: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewedByName?: string;
  rejectionReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface IStoredFileMetadata {
  fileName: string;
  originalFileName: string;
  fileDownloadUri: string;
  contentType: string;
  sizeBytes: number;
  documentType: string;
  doctorId: number;
  uploadedAt: string;
}

export interface IReviewVerification {
  action?: 'APPROVE' | 'REJECT' | 'REQUEST_INFO';
  approved?: boolean;
  rejectionReason?: string;
}

export interface IVerificationSummary {
  total: number;
  submitted: number;
  requiresAdditionalInfo: number;
  approved: number;
  rejected: number;
}
