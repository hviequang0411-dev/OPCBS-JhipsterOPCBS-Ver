import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DoctorVerificationService } from './doctor-verification.service';
import { IDoctorVerification } from './doctor-verification.model';

@Component({
  selector: 'jhi-doctor-verification',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './doctor-verification.component.html',
  styleUrls: ['./doctor-verification.component.scss'],
})
export class DoctorVerificationComponent implements OnInit {
  private readonly verificationService = inject(DoctorVerificationService);

  verification = signal<IDoctorVerification | null>(null);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  isUploading = signal<boolean>(false);

  // Alerts
  alertMessage = signal<string | null>(null);
  alertType = signal<'success' | 'danger' | 'warning' | 'info'>('info');

  // Modal & File upload
  isModalOpen = signal<boolean>(false);
  selectedFile: File | null = null;
  selectedFileName = signal<string>('Chưa chọn tệp tin');
  filePreviewUrl = signal<string | null>(null);
  filePreviewType = signal<'image' | 'pdf' | null>(null);
  filePreviewSize = signal<string>('');
  fileError = signal<string | null>(null);

  // Initial Form Data
  formData = {
    doctorName: '',
    specialization: '',
    licenseNumber: '',
    experienceYears: 5,
    education: '',
    notes: '',
    certifyCheck: false,
  };

  ngOnInit(): void {
    this.loadVerification();
  }

  loadVerification(): void {
    this.isLoading.set(true);
    this.verificationService.getVerification().subscribe({
      next: data => {
        this.verification.set(data);
        if (data) {
          this.formData.doctorName = data.doctorName ?? '';
          this.formData.specialization = data.specialization ?? '';
          this.formData.licenseNumber = data.licenseNumber ?? '';
          this.formData.experienceYears = data.experienceYears ?? 5;
          this.formData.education = data.education ?? '';
          this.formData.notes = data.notes ?? '';
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading doctor verification:', err);
        this.showAlert('Không thể tải thông tin thẩm định. Vui lòng thử lại.', 'danger');
        this.isLoading.set(false);
      },
    });
  }

  showAlert(message: string, type: 'success' | 'danger' | 'warning' | 'info' = 'info'): void {
    this.alertMessage.set(message);
    this.alertType.set(type);
    setTimeout(() => {
      this.alertMessage.set(null);
    }, 6000);
  }

  getStatusClass(status?: string): { bg: string; text: string; border: string; label: string } {
    const s = (status || 'DRAFT').toUpperCase();
    switch (s) {
      case 'APPROVED':
        return { bg: '#ecfdf5', text: '#065f46', border: '#a7f3d0', label: 'Đã Thẩm Định Hợp Lệ (Approved)' };
      case 'PENDING':
      case 'SUBMITTED':
        return { bg: '#fffbeb', text: '#92400e', border: '#fde68a', label: 'Đang Chờ Phê Duyệt (Pending Review)' };
      case 'REJECTED':
        return { bg: '#fef2f2', text: '#991b1b', border: '#fecaca', label: 'Bị Từ Chối (Rejected)' };
      default:
        return { bg: '#f8fafc', text: '#475569', border: '#e2e8f0', label: 'Chưa Nộp Hồ Sơ (Draft)' };
    }
  }

  isFileTypeImage(url?: string): boolean {
    if (!url) return false;
    const lower = url.toLowerCase();
    return lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png') || lower.endsWith('.webp');
  }

  isFileTypePdf(url?: string): boolean {
    if (!url) return false;
    const lower = url.toLowerCase();
    return lower.endsWith('.pdf');
  }

  openModal(): void {
    this.resetModalFile();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.resetModalFile();
  }

  resetModalFile(): void {
    this.selectedFile = null;
    this.selectedFileName.set('Chưa chọn tệp tin');
    this.filePreviewUrl.set(null);
    this.filePreviewType.set(null);
    this.filePreviewSize.set('');
    this.fileError.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  onFileDropped(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  handleFile(file: File): void {
    this.fileError.set(null);
    const allowedExtensions = ['.pdf', '.jpg', '.jpeg', '.png', '.webp'];
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

    if (!allowedExtensions.includes(ext)) {
      this.fileError.set('Định dạng tệp không hợp lệ. Chỉ chấp nhận: PDF, JPG, JPEG, PNG, WEBP.');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.fileError.set('Dung lượng tệp vượt quá giới hạn tối đa cho phép (10MB).');
      return;
    }

    this.selectedFile = file;
    this.selectedFileName.set(file.name);
    this.filePreviewSize.set((file.size / (1024 * 1024)).toFixed(2) + ' MB');

    if (ext === '.pdf') {
      this.filePreviewType.set('pdf');
      this.filePreviewUrl.set(null);
    } else {
      this.filePreviewType.set('image');
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.filePreviewUrl.set(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  }

  uploadAndSaveCertificate(): void {
    if (!this.selectedFile) {
      this.fileError.set('Vui lòng chọn hoặc kéo thả tệp chứng chỉ trước khi tiếp tục.');
      return;
    }

    this.isUploading.set(true);
    this.verificationService.uploadCertificate(this.selectedFile).subscribe({
      next: meta => {
        this.isUploading.set(false);
        this.closeModal();
        this.showAlert('🎉 Tải lên chứng chỉ hành nghề thành công! Hồ sơ đang được chuyển đến ban thẩm định.', 'success');
        this.loadVerification();
      },
      error: err => {
        console.error('Upload certificate error:', err);
        this.isUploading.set(false);
        this.fileError.set('Tải lên thất bại. Vui lòng kiểm tra kết nối mạng hoặc thử lại với định dạng khác.');
      },
    });
  }

  submitInitialVerification(): void {
    if (!this.formData.certifyCheck) {
      this.showAlert('Vui lòng tích xác nhận cam kết tính trung thực của hồ sơ.', 'warning');
      return;
    }

    this.isSubmitting.set(true);
    this.verificationService.submitVerification({
      doctorName: this.formData.doctorName,
      specialization: this.formData.specialization,
      licenseNumber: this.formData.licenseNumber,
      experienceYears: this.formData.experienceYears,
      education: this.formData.education,
      notes: this.formData.notes,
    }).subscribe({
      next: () => {
        if (this.selectedFile) {
          // If a file was selected in initial form, upload it as well
          this.verificationService.uploadCertificate(this.selectedFile).subscribe({
            next: () => {
              this.isSubmitting.set(false);
              this.showAlert('🎉 Đã nộp hồ sơ thẩm định và chứng chỉ thành công!', 'success');
              this.loadVerification();
            },
            error: () => {
              this.isSubmitting.set(false);
              this.showAlert('Đã lưu thông tin hồ sơ nhưng tải tệp gặp sự cố. Bạn có thể cập nhật lại tệp chứng chỉ.', 'warning');
              this.loadVerification();
            },
          });
        } else {
          this.isSubmitting.set(false);
          this.showAlert('🎉 Đã cập nhật thông tin hồ sơ hành nghề thành công!', 'success');
          this.loadVerification();
        }
      },
      error: err => {
        console.error('Submit verification error:', err);
        this.isSubmitting.set(false);
        this.showAlert('Gặp lỗi khi lưu hồ sơ. Vui lòng thử lại.', 'danger');
      },
    });
  }
}
