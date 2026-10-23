import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { serverApiUrl } from 'app/config';
import { IBlogComment, IBlogPost, IBlogStats } from '../blog.model';

@Injectable({ providedIn: 'root' })
export class BlogService {
  private http = inject(HttpClient);

  private publicUrl = `${serverApiUrl}api/blogs`;
  private doctorUrl = `${serverApiUrl}api/doctor/blogs`;
  private adminUrl = `${serverApiUrl}api/admin/blogs`;

  // === Public Endpoints ===
  getPublishedBlogs(keyword?: string, page = 0, size = 9): Observable<any> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<any>(this.publicUrl, { params });
  }

  getBlogDetails(id: number): Observable<IBlogPost> {
    return this.http.get<IBlogPost>(`${this.publicUrl}/${id}`);
  }

  getComments(blogId: number): Observable<IBlogComment[]> {
    return this.http.get<IBlogComment[]>(`${this.publicUrl}/${blogId}/comments`);
  }

  addComment(blogId: number, comment: IBlogComment): Observable<IBlogComment> {
    return this.http.post<IBlogComment>(`${this.publicUrl}/${blogId}/comments`, comment);
  }

  // === Doctor Endpoints ===
  getDoctorBlogs(): Observable<IBlogPost[]> {
    return this.http.get<IBlogPost[]>(this.doctorUrl);
  }

  getDoctorBlogById(id: number): Observable<IBlogPost> {
    return this.http.get<IBlogPost>(`${this.doctorUrl}/${id}`);
  }

  createDoctorBlog(post: IBlogPost): Observable<IBlogPost> {
    return this.http.post<IBlogPost>(this.doctorUrl, post);
  }

  updateDoctorBlog(id: number, post: IBlogPost): Observable<IBlogPost> {
    return this.http.put<IBlogPost>(`${this.doctorUrl}/${id}`, post);
  }

  submitDoctorBlog(id: number): Observable<IBlogPost> {
    return this.http.post<IBlogPost>(`${this.doctorUrl}/${id}/submit`, {});
  }

  deleteDoctorBlog(id: number): Observable<void> {
    return this.http.delete<void>(`${this.doctorUrl}/${id}`);
  }

  // === Admin / CS Moderation Endpoints ===
  getAdminBlogs(status?: string): Observable<IBlogPost[]> {
    let params = new HttpParams();
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    return this.http.get<IBlogPost[]>(this.adminUrl, { params });
  }

  approveBlog(id: number): Observable<IBlogPost> {
    return this.http.post<IBlogPost>(`${this.adminUrl}/${id}/approve`, {});
  }

  rejectBlog(id: number, reason: string): Observable<IBlogPost> {
    return this.http.post<IBlogPost>(`${this.adminUrl}/${id}/reject`, { reason });
  }

  archiveBlog(id: number): Observable<IBlogPost> {
    return this.http.post<IBlogPost>(`${this.adminUrl}/${id}/archive`, {});
  }

  getBlogStats(): Observable<IBlogStats> {
    return this.http.get<IBlogStats>(`${this.adminUrl}/stats`);
  }
}
