export interface CompanySummary {
  id: string;
  name: string;
  slug: string;
  industryName: string | null;
  websiteUrl: string | null;
  logoUrl: string | null;
  fundingStage: string | null;
  employeeCountMin: number | null;
  employeeCountMax: number | null;
  verificationStatus: string | null;
  areas: string[];
  activeJobCount: number;
  fresherJobCount: number;
}

export interface Industry {
  id: string;
  name: string;
  slug: string;
}

export interface CompanyLocation {
  id: string;
  locationName: string | null;
  addressLine: string | null;
  area: string | null;
  city: string;
  state: string;
  countryCode: string;
  postalCode: string | null;
  locationType: string;
  geocodeStatus: string;
  headquarters: boolean;
  latitude: number | null;
  longitude: number | null;
}

export interface CompanySource {
  sourceName: string;
  sourceUrl: string | null;
  externalId: string | null;
  lastVerifiedAt: string | null;
}

export interface CompanyDetail {
  id: string;
  name: string;
  slug: string;
  websiteUrl: string | null;
  logoUrl: string | null;
  description: string | null;
  industryName: string | null;
  industrySlug: string | null;
  foundedYear: number | null;
  employeeCountMin: number | null;
  employeeCountMax: number | null;
  fundingStage: string | null;
  companyType: string | null;
  verificationStatus: string | null;
  locations: CompanyLocation[];
  sources: CompanySource[];
  activeJobCount: number;
  fresherJobCount: number;
  createdAt: string;
  updatedAt: string;
}
