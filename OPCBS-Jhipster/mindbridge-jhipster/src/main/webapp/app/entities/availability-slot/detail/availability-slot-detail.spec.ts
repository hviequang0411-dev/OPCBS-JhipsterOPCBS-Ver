import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { faArrowLeft, faPencilAlt } from '@fortawesome/free-solid-svg-icons';
import { of } from 'rxjs';

import { AvailabilitySlotDetail } from './availability-slot-detail';

describe('AvailabilitySlot Management Detail Component', () => {
  let comp: AvailabilitySlotDetail;
  let fixture: ComponentFixture<AvailabilitySlotDetail>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./availability-slot-detail').then(m => m.AvailabilitySlotDetail),
              resolve: { availabilitySlot: () => of({ id: 19542 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    });
    const library = TestBed.inject(FaIconLibrary);
    library.addIcons(faArrowLeft);
    library.addIcons(faPencilAlt);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AvailabilitySlotDetail);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load availabilitySlot on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', AvailabilitySlotDetail);

      // THEN
      expect(instance.availabilitySlot()).toEqual(expect.objectContaining({ id: 19542 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      vi.spyOn(globalThis.history, 'back');
      comp.previousState();
      expect(globalThis.history.back).toHaveBeenCalled();
    });
  });
});
