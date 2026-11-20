export interface MapBounds {
  minLat: number;
  maxLat: number;
  minLng: number;
  maxLng: number;
}

export interface MapMarker {
  companyId: string;
  companyName: string;
  companySlug: string;
  locationId: string;
  area: string | null;
  city: string;
  latitude: number;
  longitude: number;
  activeJobCount: number;
  fresherJobCount: number;
}
