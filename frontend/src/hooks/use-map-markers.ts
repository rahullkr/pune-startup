import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { getMapMarkers } from "@/features/map/api";
import type { MapBounds } from "@/features/map/types";

export function useMapMarkers(bounds: MapBounds) {
  return useQuery({
    queryKey: ["companies", "map", bounds],
    queryFn: ({ signal }) => getMapMarkers(bounds, signal),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
  });
}
