import { ArrowUpRight, MapPin, Sparkles, X } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/states";
import { CompanyMap, type CompanyMapHandle } from "@/features/map/components/company-map";
import type { MapBounds } from "@/features/map/types";
import { useMapMarkers } from "@/hooks/use-map-markers";
import { cn } from "@/lib/utils";

const INITIAL_BOUNDS: MapBounds = {
  minLat: 18.35,
  maxLat: 18.75,
  minLng: 73.6,
  maxLng: 74.1,
};

export function MapPage() {
  const [bounds, setBounds] = useState<MapBounds>(INITIAL_BOUNDS);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string | null>(null);
  const mapRef = useRef<CompanyMapHandle>(null);

  const markersQuery = useMapMarkers(bounds);
  const markers = useMemo(
    () => [...(markersQuery.data ?? [])].sort((a, b) => a.companyName.localeCompare(b.companyName)),
    [markersQuery.data],
  );
  const selectedMarker =
    markers.find((marker) => marker.companyId === selectedCompanyId) ?? null;

  useEffect(() => {
    if (!selectedCompanyId || markersQuery.isPending) {
      return;
    }

    if (!markers.some((marker) => marker.companyId === selectedCompanyId)) {
      setSelectedCompanyId(null);
    }
  }, [markers, markersQuery.isPending, selectedCompanyId]);

  function selectCompany(companyId: string) {
    setSelectedCompanyId(companyId);
  }

  function selectFromList(companyId: string, latitude: number, longitude: number) {
    setSelectedCompanyId(companyId);
    mapRef.current?.focusOn(latitude, longitude);
  }

  return (
    <section className="container-page py-12 sm:py-16">
      <div className="max-w-2xl">
        <Badge>Startup map</Badge>
        <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">
          See where Pune builds.
        </h1>
        <p className="mt-4 text-base leading-relaxed text-slate-600 sm:text-lg">
          Explore verified startup offices across the city. Move the map to refresh pins, then open a
          startup&apos;s profile and its open roles.
        </p>
      </div>

      <div className="mt-8 grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start">
        <div className="relative h-[420px] overflow-hidden rounded-3xl border border-slate-200 bg-slate-100 shadow-sm shadow-slate-900/5 sm:h-[600px]">
          <CompanyMap
            ref={mapRef}
            markers={markers}
            selectedCompanyId={selectedCompanyId}
            onSelectCompany={selectCompany}
            onBoundsChange={setBounds}
          />

          <div className="pointer-events-none absolute left-3 top-3 z-[500] flex flex-wrap gap-2">
            <span className="rounded-full bg-white/95 px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm">
              {markersQuery.isPending
                ? "Loading locations…"
                : `${markers.length} ${markers.length === 1 ? "startup" : "startups"} in view`}
            </span>
            {markersQuery.isFetching && !markersQuery.isPending ? (
              <span className="rounded-full bg-slate-900/90 px-3 py-1.5 text-xs font-semibold text-white shadow-sm">
                Updating…
              </span>
            ) : null}
          </div>

          <p className="pointer-events-none absolute bottom-3 left-3 z-[500] max-w-[70%] rounded-full bg-white/95 px-3 py-1.5 text-[11px] font-medium text-slate-600 shadow-sm">
            Verified office locations only. Approximate locations stay on company profiles.
          </p>
        </div>

        <aside className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-900/5">
          <div className="flex items-center justify-between gap-3 px-1">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Startups in view
            </h2>
            <span className="text-xs font-medium text-slate-500">{markers.length}</span>
          </div>

          {selectedMarker ? (
            <div className="mt-3 rounded-2xl border border-brand-200 bg-brand-50/60 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">
                    Selected
                  </p>
                  <p className="mt-1 truncate text-sm font-bold text-slate-900">
                    {selectedMarker.companyName}
                  </p>
                  <p className="mt-0.5 flex items-center gap-1 text-xs text-slate-600">
                    <MapPin aria-hidden="true" className="h-3 w-3" />
                    {[selectedMarker.area, selectedMarker.city].filter(Boolean).join(", ")}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedCompanyId(null)}
                  aria-label="Clear selected startup"
                  className="grid h-7 w-7 shrink-0 place-items-center rounded-full text-slate-500 transition hover:bg-white hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                >
                  <X aria-hidden="true" className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-3 flex flex-wrap gap-1.5 text-[11px] font-medium">
                <span className="rounded-full bg-white px-2.5 py-1 text-slate-700 ring-1 ring-brand-200">
                  {selectedMarker.activeJobCount} active{" "}
                  {selectedMarker.activeJobCount === 1 ? "role" : "roles"}
                </span>
                {selectedMarker.fresherJobCount > 0 ? (
                  <span className="inline-flex items-center gap-1 rounded-full bg-white px-2.5 py-1 text-emerald-700 ring-1 ring-emerald-200">
                    <Sparkles aria-hidden="true" className="h-3 w-3" />
                    {selectedMarker.fresherJobCount} fresher-friendly
                  </span>
                ) : null}
              </div>

              <Link
                to={`/startups/${selectedMarker.companyId}`}
                className="mt-4 inline-flex items-center gap-1.5 rounded text-xs font-semibold text-brand-700 transition hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
              >
                Open startup profile
                <ArrowUpRight aria-hidden="true" className="h-3.5 w-3.5" />
              </Link>
            </div>
          ) : null}

          {markersQuery.isError ? (
            <ErrorState
              className="mt-3 border-solid px-4 py-6"
              title="Couldn't load map locations"
              description="The map API didn't respond. Make sure the backend is running on port 8080."
              onRetry={() => void markersQuery.refetch()}
            />
          ) : null}

          {!markersQuery.isError && !markersQuery.isPending && markers.length === 0 ? (
            <p className="mt-3 rounded-2xl border border-dashed border-slate-300 bg-slate-50/60 px-4 py-8 text-center text-sm text-slate-600">
              No verified startup offices in this view. Try zooming out or moving the map.
            </p>
          ) : null}

          {markers.length > 0 ? (
            <ul className="mt-3 max-h-[420px] space-y-2 overflow-y-auto pr-1">
              {markers.map((marker) => {
                const selected = marker.companyId === selectedCompanyId;

                return (
                  <li
                    key={`${marker.locationId}-${marker.companyId}`}
                    className={cn(
                      "rounded-2xl border p-3 transition",
                      selected
                        ? "border-brand-300 bg-brand-50/50"
                        : "border-slate-200 bg-slate-50/50 hover:border-slate-300 hover:bg-white",
                    )}
                  >
                    <button
                      type="button"
                      onClick={() =>
                        selectFromList(marker.companyId, marker.latitude, marker.longitude)
                      }
                      aria-pressed={selected}
                      className="w-full rounded text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                    >
                      <span className="block text-sm font-semibold text-slate-900">
                        {marker.companyName}
                      </span>
                      <span className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                        <span className="inline-flex items-center gap-1">
                          <MapPin aria-hidden="true" className="h-3 w-3" />
                          {[marker.area, marker.city].filter(Boolean).join(", ")}
                        </span>
                        <span>
                          {marker.activeJobCount} active{" "}
                          {marker.activeJobCount === 1 ? "role" : "roles"}
                        </span>
                        {marker.fresherJobCount > 0 ? (
                          <span className="text-emerald-700">
                            {marker.fresherJobCount} fresher-friendly
                          </span>
                        ) : null}
                      </span>
                    </button>
                    <div className="mt-2">
                      <Link
                        to={`/startups/${marker.companyId}`}
                        className="inline-flex items-center gap-1 rounded text-xs font-semibold text-brand-700 transition hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                      >
                        View profile
                        <ArrowUpRight aria-hidden="true" className="h-3 w-3" />
                      </Link>
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : null}

          {markersQuery.isPending ? (
            <div className="mt-3 space-y-2" aria-hidden="true">
              <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
              <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
              <div className="h-16 animate-pulse rounded-2xl bg-slate-100" />
            </div>
          ) : null}

          <div className="mt-4 border-t border-slate-100 pt-3">
            <Button
              variant="ghost"
              size="sm"
              className="w-full"
              onClick={() => {
                setSelectedCompanyId(null);
                mapRef.current?.resetView();
              }}
            >
              Reset map view
            </Button>
          </div>
        </aside>
      </div>
    </section>
  );
}
