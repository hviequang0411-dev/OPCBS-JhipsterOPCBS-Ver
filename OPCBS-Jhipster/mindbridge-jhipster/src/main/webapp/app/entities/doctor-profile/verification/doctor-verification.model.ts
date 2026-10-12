export interface IDoctorVerification {
  doctorId?: number;
  doctorName?: string;
  specialization?: string;
  licenseNumber?: string;
  experienceYears?: number;
  education?: string;
  submittedAt?: string;
  status?: string; // 'DRAFT' | 'PENDING' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
  notes?: string;
  certificateUrl?: string;
  certificateFileName?: string;
  certificateUploadedAt?: string;
  reviewedAt?: string;
  reviewedByName?: string;
  rejectionReason?: string;
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
