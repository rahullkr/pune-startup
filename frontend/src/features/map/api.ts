import { apiGet } from "@/api/client";
import type { MapBounds, MapMarker } from "@/features/map/types";

export function getMapMarkers(bounds: MapBounds, signal?: AbortSignal): Promise<MapMarker[]> {
  const search = new URLSearchParams({
    minLat: String(bounds.minLat),
    maxLat: String(bounds.maxLat),
    minLng: String(bounds.minLng),
    maxLng: String(bounds.maxLng),
  });

  return apiGet<MapMarker[]>(`/companies/map?${search.toString()}`, signal);
}
