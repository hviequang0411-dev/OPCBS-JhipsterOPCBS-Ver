import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { BlogService } from '../service/blog.service';
import { IBlogPost } from '../blog.model';

@Component({
  selector: 'jhi-doctor-blogs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgbModule],
  templateUrl: './doctor-blogs.component.html',
  styleUrls: ['./doctor-blogs.component.scss']
})
export class DoctorBlogsComponent implements OnInit {
  private blogService = inject(BlogService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  blogs: IBlogPost[] = [];
  isLoading = false;
  selectedFilter = 'ALL';
  searchKeyword = '';

  // Form State
  editingBlog: IBlogPost = this.getEmptyBlog();
  isEditMode = false;
  isSaving = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loadBlogs();
  }

  loadBlogs(): void {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.blogService.getDoctorBlogs().subscribe({
      next: res => {
        this.blogs = res || [];
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get totalCount(): number {
    return this.blogs.length;
  }

  get publishedCount(): number {
    return this.blogs.filter(b => b.status === 'PUBLISHED').length;
  }

  get pendingCount(): number {
    return this.blogs.filter(b => b.status === 'SUBMITTED').length;
  }

  get draftCount(): number {
    return this.blogs.filter(b => b.status === 'DRAFT').length;
  }

  get filteredBlogs(): IBlogPost[] {
    let list = this.blogs;
    if (this.selectedFilter !== 'ALL') {
      list = list.filter(b => b.status === this.selectedFilter);
    }
    if (this.searchKeyword && this.searchKeyword.trim()) {
      const q = this.searchKeyword.trim().toLowerCase();
      list = list.filter(b => 
        (b.title && b.title.toLowerCase().includes(q)) ||
        (b.excerpt && b.excerpt.toLowerCase().includes(q))
      );
    }
    return list;
  }

  openCreateModal(content: any): void {
    this.isEditMode = false;
    this.editingBlog = this.getEmptyBlog();
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static', scrollable: true });
  }

  openEditModal(content: any, blog: IBlogPost): void {
    this.isEditMode = true;
    this.editingBlog = { ...blog };
    this.errorMessage = '';
    this.modalService.open(content, { size: 'lg', backdrop: 'static', scrollable: true });
  }

  saveBlog(modal: any, submitImmediately = false): void {
    if (!this.editingBlog.title?.trim() || !this.editingBlog.content?.trim()) {
      this.errorMessage = 'Vui lòng nhập đầy đủ tiêu đề và nội dung bài viết!';
      return;
    }

    if (submitImmediately) {
      this.editingBlog.status = 'SUBMITTED';
    }

    this.isSaving = true;
    this.errorMessage = '';

    if (this.isEditMode && this.editingBlog.id) {
      this.blogService.updateDoctorBlog(this.editingBlog.id, this.editingBlog).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadBlogs();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi cập nhật bài viết!';
        }
      });
    } else {
      this.blogService.createDoctorBlog(this.editingBlog).subscribe({
        next: () => {
          this.isSaving = false;
          modal.close();
          this.loadBlogs();
        },
        error: err => {
          this.isSaving = false;
          this.errorMessage = err.error?.message || 'Có lỗi xảy ra khi đăng bài!';
        }
      });
    }
  }

  submitBlog(id?: number): void {
    if (!id) return;
    if (confirm('Bác sĩ có chắc chắn muốn gửi bài viết này để Ban kiểm duyệt phê duyệt không?')) {
      this.blogService.submitDoctorBlog(id).subscribe({
        next: () => {
          this.loadBlogs();
        },
        error: err => {
          alert(err.error?.message || 'Không thể gửi duyệt bài viết!');
        }
      });
    }
  }

  deleteBlog(id?: number): void {
    if (!id) return;
    if (confirm('Bác sĩ có chắc chắn muốn xóa bài viết này không?')) {
      this.blogService.deleteDoctorBlog(id).subscribe({
        next: () => {
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

  private getEmptyBlog(): IBlogPost {
    return {
      title: '',
      content: '',
      excerpt: '',
      thumbnailUrl: '',
      status: 'DRAFT'
    };
  }
}
