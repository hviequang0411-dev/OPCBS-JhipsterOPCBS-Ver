import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { of, throwError } from 'rxjs';

import { Account, AccountService } from 'app/core/auth';

import Settings from './settings';

describe('Settings', () => {
  let comp: Settings;
  let fixture: ComponentFixture<Settings>;
  let mockAccountService: AccountService;
  const profile = {
    userId: 1,
    firstName: 'John',
    lastName: 'Doe',
    activated: true,
    email: 'john.doe@mail.com',
    langKey: 'en',
    login: 'john',
    authorities: ['ROLE_USER', 'ROLE_PATIENT'],
    primaryRole: 'PATIENT',
    imageUrl: '',
    patientProfile: {
      id: 1,
      fullName: 'John Doe',
      phone: '0912345678',
      gender: 'MALE',
      dateOfBirth: '1990-01-01',
      address: '123 Street',
      medicalHistory: 'None',
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AccountService,
          useValue: {
            authenticate: vi.fn(),
            getProfile: vi.fn(() => of(profile)),
            updateProfile: vi.fn(() => of(profile)),
            identity: vi.fn(() => of(null)),
          },
        },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(Settings);
    comp = fixture.componentInstance;
    mockAccountService = TestBed.inject(AccountService);
  });

  it('should send the updated profile upon save', () => {
    // GIVEN
    mockAccountService.getProfile = vi.fn(() => of(profile));
    mockAccountService.updateProfile = vi.fn(() => of(profile));

    // WHEN
    comp.ngOnInit();
    comp.save();

    // THEN
    expect(mockAccountService.getProfile).toHaveBeenCalled();
    expect(mockAccountService.updateProfile).toHaveBeenCalled();
    expect(mockAccountService.authenticate).toHaveBeenCalled();
    expect(comp.settingsForm.get('email')?.value).toBe('john.doe@mail.com');
  });

  it('should notify of success upon successful save', () => {
    // GIVEN
    mockAccountService.updateProfile = vi.fn(() => of(profile));

    // WHEN
    comp.ngOnInit();
    comp.save();

    // THEN
    expect(comp.success()).toBe(true);
  });

  it('should notify of error upon failed save', () => {
    // GIVEN
    mockAccountService.updateProfile = vi.fn(() => throwError(Error));

    // WHEN
    comp.ngOnInit();
    comp.save();

    // THEN
    expect(comp.success()).toBe(false);
  });
});
