import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BlogService } from '../service/blog.service';
import { IBlogPost } from '../blog.model';

@Component({
  selector: 'jhi-blog-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './blog-catalog.component.html',
  styleUrls: ['./blog-catalog.component.scss']
})
export class BlogCatalogComponent implements OnInit {
  private blogService = inject(BlogService);
  private cdr = inject(ChangeDetectorRef);

  blogs: IBlogPost[] = [];
  isLoading = false;
  searchKeyword = '';
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;

  ngOnInit(): void {
    this.loadBlogs();
  }

  loadBlogs(page = 0): void {
    this.isLoading = true;
    this.currentPage = page;
    this.cdr.markForCheck();

    this.blogService.getPublishedBlogs(this.searchKeyword, page, 9).subscribe({
      next: res => {
        this.blogs = res.content || [];
        this.totalPages = res.totalPages || 0;
        this.totalElements = res.totalElements || 0;
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

  onSearch(): void {
    this.loadBlogs(0);
  }

  clearSearch(): void {
    this.searchKeyword = '';
    this.loadBlogs(0);
  }
}
