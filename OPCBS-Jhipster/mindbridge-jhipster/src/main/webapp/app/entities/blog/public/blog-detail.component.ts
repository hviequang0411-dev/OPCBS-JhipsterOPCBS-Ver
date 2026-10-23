import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { BlogService } from '../service/blog.service';
import { IBlogComment, IBlogPost } from '../blog.model';

@Component({
  selector: 'jhi-blog-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './blog-detail.component.html',
  styleUrls: ['./blog-detail.component.scss']
})
export class BlogDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private blogService = inject(BlogService);
  private cdr = inject(ChangeDetectorRef);

  blog: IBlogPost | null = null;
  comments: IBlogComment[] = [];
  isLoading = true;

  // New Comment Form
  newComment: IBlogComment = { content: '', authorName: '', authorEmail: '' };
  isSubmittingComment = false;
  commentSuccessMessage = '';
  commentErrorMessage = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadBlogDetail(Number(id));
    }
  }

  loadBlogDetail(id: number): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    this.blogService.getBlogDetails(id).subscribe({
      next: res => {
        this.blog = res;
        this.isLoading = false;
        this.loadComments(id);
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadComments(blogId: number): void {
    this.blogService.getComments(blogId).subscribe({
      next: res => {
        this.comments = res || [];
        this.cdr.markForCheck();
      }
    });
  }

  submitComment(): void {
    if (!this.blog?.id || !this.newComment.content.trim()) {
      this.commentErrorMessage = 'Vui lòng nhập nội dung bình luận!';
      return;
    }

    this.isSubmittingComment = true;
    this.commentErrorMessage = '';
    this.commentSuccessMessage = '';

    this.blogService.addComment(this.blog.id, this.newComment).subscribe({
      next: created => {
        this.comments.unshift(created);
        this.newComment = { content: '', authorName: '', authorEmail: '' };
        this.isSubmittingComment = false;
        this.commentSuccessMessage = 'Bình luận của bạn đã được gửi thành công!';
        this.cdr.markForCheck();
        setTimeout(() => (this.commentSuccessMessage = ''), 5000);
      },
      error: () => {
        this.isSubmittingComment = false;
        this.commentErrorMessage = 'Có lỗi xảy ra khi gửi bình luận!';
        this.cdr.markForCheck();
      }
    });
  }
}
