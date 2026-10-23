import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { BlogService } from '../service/blog.service';
import { IBlogPost, IBlogStats } from '../blog.model';

@Component({
  selector: 'jhi-admin-blogs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgbModule],
  templateUrl: './admin-blogs.component.html',
  styleUrls: ['./admin-blogs.component.scss']
})
export class AdminBlogsComponent implements OnInit {
  private blogService = inject(BlogService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  blogs: IBlogPost[] = [];
  stats: IBlogStats | null = null;
  isLoading = false;
  selectedFilter = 'SUBMITTED';

  // Preview / Action Modal State
  selectedBlog: IBlogPost | null = null;
  rejectReason = '';
  isProcessing = false;

  ngOnInit(): void {
    this.loadStats();
    this.loadBlogs();
  }

  loadStats(): void {
    this.blogService.getBlogStats().subscribe({
      next: res => {
        this.stats = res;
        this.cdr.markForCheck();
      }
    });
  }

  loadBlogs(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.blogService.getAdminBlogs(this.selectedFilter).subscribe({
      next: res => {
        this.blogs = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.blogs = [];
        this.cdr.markForCheck();
      }
    });
  }

  setFilter(filter: string): void {
    this.selectedFilter = filter;
    this.loadBlogs();
  }

  openPreviewModal(content: any, blog: IBlogPost): void {
    this.selectedBlog = blog;
    this.rejectReason = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static', scrollable: true });
  }

  approveBlog(modal: any, id?: number): void {
    if (!id) return;
    this.isProcessing = true;
    this.blogService.approveBlog(id).subscribe({
      next: () => {
        this.isProcessing = false;
        modal.close();
        this.loadStats();
        this.loadBlogs();
      },
      error: () => {
        this.isProcessing = false;
        alert('Có lỗi xảy ra khi phê duyệt bài viết!');
      }
    });
  }

  openRejectModal(content: any, blog: IBlogPost): void {
    this.selectedBlog = blog;
    this.rejectReason = 'Nội dung chưa đảm bảo tính chính xác y khoa hoặc thiếu nguồn dẫn chứng.';
    this.modalService.open(content, { size: 'md', centered: true });
  }

  confirmReject(modal: any): void {
    if (!this.selectedBlog?.id || !this.rejectReason.trim()) {
      alert('Vui lòng nhập lý do từ chối!');
      return;
    }
    this.isProcessing = true;
    this.blogService.rejectBlog(this.selectedBlog.id, this.rejectReason).subscribe({
      next: () => {
        this.isProcessing = false;
        modal.close();
        this.loadStats();
        this.loadBlogs();
      },
      error: () => {
        this.isProcessing = false;
        alert('Có lỗi xảy ra khi từ chối bài viết!');
      }
    });
  }

  archiveBlog(id?: number): void {
    if (!id) return;
    if (confirm('Bạn có chắc muốn lưu trữ và ẩn bài viết này khỏi trang chủ?')) {
      this.blogService.archiveBlog(id).subscribe({
        next: () => {
          this.loadStats();
          this.loadBlogs();
        }
      });
    }
  }

  getStatusBadgeClass(status?: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'badge bg-success';
      case 'SUBMITTED':
        return 'badge bg-warning text-dark';
      case 'REJECTED':
        return 'badge bg-danger';
      case 'ARCHIVED':
        return 'badge bg-secondary';
      default:
        return 'badge bg-light text-dark border';
    }
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'Đã xuất bản';
      case 'SUBMITTED':
        return 'Chờ phê duyệt';
      case 'REJECTED':
        return 'Bị từ chối';
      case 'ARCHIVED':
        return 'Đã lưu trữ';
      default:
        return 'Bản nháp';
    }
  }
}
