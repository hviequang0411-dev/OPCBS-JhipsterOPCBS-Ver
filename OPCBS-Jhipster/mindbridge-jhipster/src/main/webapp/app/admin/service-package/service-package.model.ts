export interface ServicePackage {
  id?: number;
  name?: string;
  description?: string;
  price?: number;
  durationDays?: number;
  maxDailySlotsCapacity?: number | null;
  featuresList?: string;
  isActive?: boolean;
  displayOrder?: number;
  createdDate?: string;
  lastModifiedDate?: string;
}

export interface ServicePackageStats {
  totalPackages: number;
  activePackages: number;
  inactivePackages: number;
  averagePrice: number;
}
