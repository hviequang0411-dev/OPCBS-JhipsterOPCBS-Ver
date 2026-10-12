import { HttpHeaders } from '@angular/common/http';
import { Component, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { combineLatest, filter, map, tap } from 'rxjs';

import { DEFAULT_SORT_DATA, ITEMS_PER_PAGE, ITEM_DELETED_EVENT, PAGE_HEADER, SORT, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { AccountService } from 'app/core/auth';
import { DataUtils } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { Filter, FilterOptions, IFilterOption, IFilterOptions } from 'app/shared/filter';
import { type BlobType } from 'app/shared/jhipster/data-utils';
import { ItemCount } from 'app/shared/pagination';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { ISpecialization } from 'app/entities/specialization/specialization.model';
import { SpecializationService } from 'app/entities/specialization/service/specialization.service';
import { DoctorProfileDeleteDialog } from '../delete/doctor-profile-delete-dialog';
import { IDoctorProfile } from '../doctor-profile.model';
import { DoctorProfileService } from '../service/doctor-profile.service';

@Component({
  selector: 'jhi-doctor-profile',
  templateUrl: './doctor-profile.html',
  styleUrl: './doctor-profile.scss',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    FontAwesomeModule,
    AlertError,
    Alert,
    NgbPagination,
    ItemCount,
  ],
})
export class DoctorProfile {
  readonly doctorProfiles = signal<IDoctorProfile[]>([]);
  readonly specializations = signal<ISpecialization[]>([]);

  searchKeyword = '';
  selectedSpecializationId: number | null = null;

  sortState = sortStateSignal({});
  filters: IFilterOptions = new FilterOptions();

  readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  readonly totalItems = signal(0);
  readonly page = signal(1);

  readonly account = inject(AccountService).account;
  readonly router = inject(Router);
  protected readonly doctorProfileService = inject(DoctorProfileService);
  protected readonly specializationService = inject(SpecializationService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.doctorProfileService.doctorProfilesResource.isLoading;
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly activatedRouteState = toSignal(
    combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      map(([queryParamMap, data]) => ({ queryParamMap, data })),
    ),
    { initialValue: { queryParamMap: this.activatedRoute.snapshot.queryParamMap, data: this.activatedRoute.snapshot.data } },
  );
  protected readonly sortService = inject(SortService);
  protected readonly filterOptions = toSignal(this.filters.filterChanges);
  protected dataUtils = inject(DataUtils);
  protected modalService = inject(NgbModal);

  constructor() {
    // Load specializations for the filter dropdown
    this.specializationService.query({ size: 50, sort: ['name,asc'] }).subscribe({
      next: res => {
        if (res.body) {
          this.specializations.set(res.body);
        }
      },
    });

    effect(() => {
      const headers = this.doctorProfileService.doctorProfilesResource.headers();
      if (headers) {
        this.fillComponentAttributesFromResponseHeader(headers);
      }
    });
    effect(() => {
      this.doctorProfiles.set(this.fillComponentAttributesFromResponseBody([...this.doctorProfileService.doctorProfiles()]));
    });
    effect(() => {
      const activatedRouteState = this.activatedRouteState();
      untracked(() => {
        // Only watch for route changes. Other signals should be ignored.
        this.fillComponentAttributeFromRoute(activatedRouteState.queryParamMap, activatedRouteState.data);
        this.load();
      });
    });

    effect(() => {
      const filterOptions = this.filterOptions();
      if (filterOptions) {
        untracked(() => {
          // Only watch for filter changes. Other signals should be ignored.
          this.handleNavigation(1, this.sortState(), filterOptions);
        });
      }
    });
  }

  getInitials(name?: string | null): string {
    if (!name) return 'DR';
    const parts = name.trim().split(' ').filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  onFilterSubmit(): void {
    const queryParams: Record<string, any> = { page: 1 };
    if (this.searchKeyword.trim()) {
      queryParams['fullName.contains'] = this.searchKeyword.trim();
    }
    if (this.selectedSpecializationId) {
      queryParams['specializationsId.equals'] = this.selectedSpecializationId;
    }
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }

  resetFilters(): void {
    this.searchKeyword = '';
    this.selectedSpecializationId = null;
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: {},
    });
  }

  trackId = (item: IDoctorProfile): number => this.doctorProfileService.getDoctorProfileIdentifier(item);

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined, blobType?: BlobType): void {
    return this.dataUtils.openFile(base64String, contentType, blobType);
  }

  delete(doctorProfile: IDoctorProfile): void {
    const modalRef = this.modalService.open(DoctorProfileDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.doctorProfile = doctorProfile;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load(): void {
    this.queryBackend();
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page(), event, this.filters.filterOptions);
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState(), this.filters.filterOptions);
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page.set(+(page ?? 1));
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
    this.filters.initializeFromParams(params);
  }

  protected fillComponentAttributesFromResponseBody(data: IDoctorProfile[]): IDoctorProfile[] {
    return data;
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER)));
  }

  protected queryBackend(): void {
    const pageToLoad: number = this.page();
    const queryObject: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage(),
      eagerload: true,
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    for (const filterOption of this.filters.filterOptions) {
      queryObject[filterOption.name] = filterOption.values;
    }
    this.doctorProfileService.doctorProfilesParams.set(queryObject);
  }

  protected handleNavigation(page: number, sortState: SortState, filterOptions?: IFilterOption[]): void {
    const queryParamsObj: any = {
      page,
      size: this.itemsPerPage(),
      sort: this.sortService.buildSortParam(sortState),
    };

    if (filterOptions) {
      for (const filterOption of filterOptions) {
        queryParamsObj[filterOption.nameAsQueryParam()] = filterOption.values;
      }
    }

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }
}
