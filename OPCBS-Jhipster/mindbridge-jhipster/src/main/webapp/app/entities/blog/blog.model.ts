import { IDoctorProfile } from 'app/entities/doctor-profile/doctor-profile.model';

export type BlogStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'PUBLISHED' | 'ARCHIVED';

export interface IBlogComment {
  id?: number;
  authorName?: string | null;
  authorEmail?: string | null;
  content: string;
  isApproved?: boolean;
  createdDate?: string | null;
  patient?: {
    id?: number;
    fullName?: string | null;
    user?: {
      login?: string;
      imageUrl?: string;
    };
  } | null;
}

export interface IBlogPost {
  id?: number;
  title: string;
  content: string;
  thumbnailUrl?: string | null;
  excerpt?: string | null;
  status?: BlogStatus;
  viewCount?: number;
  rejectionReason?: string | null;
  submittedAt?: string | null;
  approvedAt?: string | null;
  publishedAt?: string | null;
  archivedAt?: string | null;
  createdDate?: string | null;
  doctor?: IDoctorProfile | null;
  comments?: IBlogComment[];
}

export interface IBlogStats {
  totalBlogs: number;
  publishedBlogs: number;
  pendingBlogs: number;
  rejectedBlogs: number;
  draftBlogs: number;
}
