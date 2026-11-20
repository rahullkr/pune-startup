import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import type { MapBounds, MapMarker } from "@/features/map/types";

const CLUSTER_SIZE_PX = 72;
const INITIAL_CENTER: L.LatLngExpression = [18.55, 73.84];
const INITIAL_ZOOM = 11;

const singleMarkerIcon = (selected: boolean) =>
  L.divIcon({
    className: "company-map-pin",
    html: `<span style="display:block;width:18px;height:18px;border-radius:9999px;background:${
      selected ? "#ea580c" : "#0f172a"
    };border:3px solid #ffffff;box-shadow:0 2px 10px rgba(15,23,42,0.35);${
      selected ? "outline:3px solid rgba(234,88,12,0.35);outline-offset:2px;" : ""
    }"></span>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });

const clusterIcon = (count: number) =>
  L.divIcon({
    className: "company-map-pin",
    html: `<span style="display:flex;align-items:center;justify-content:center;width:42px;height:42px;border-radius:9999px;background:#0f172a;color:#fdba74;border:3px solid #ffffff;box-shadow:0 6px 16px rgba(15,23,42,0.3);font-size:13px;font-weight:700;line-height:1;">${count}</span>`,
    iconSize: [48, 48],
    iconAnchor: [24, 24],
  });

export interface CompanyMapHandle {
  focusOn: (latitude: number, longitude: number) => void;
  resetView: () => void;
}

interface CompanyMapProps {
  markers: MapMarker[];
  selectedCompanyId: string | null;
  onSelectCompany: (companyId: string) => void;
  onBoundsChange: (bounds: MapBounds) => void;
}

export const CompanyMap = forwardRef<CompanyMapHandle, CompanyMapProps>(function CompanyMap(
  { markers, selectedCompanyId, onSelectCompany, onBoundsChange },
  ref,
) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const layerRef = useRef<L.LayerGroup | null>(null);
  const renderRef = useRef<() => void>(() => undefined);
  const markersRef = useRef(markers);
  const selectedCompanyIdRef = useRef(selectedCompanyId);
  const onSelectCompanyRef = useRef(onSelectCompany);
  const onBoundsChangeRef = useRef(onBoundsChange);

  markersRef.current = markers;
  selectedCompanyIdRef.current = selectedCompanyId;
  onSelectCompanyRef.current = onSelectCompany;
  onBoundsChangeRef.current = onBoundsChange;

  useImperativeHandle(ref, () => ({
    focusOn(latitude, longitude) {
      const map = mapRef.current;

      if (!map) {
        return;
      }

      map.flyTo([latitude, longitude], Math.max(map.getZoom(), 14), { duration: 0.4 });
    },
    resetView() {
      mapRef.current?.flyTo(INITIAL_CENTER, INITIAL_ZOOM, { duration: 0.4 });
    },
  }));

  useEffect(() => {
    const container = containerRef.current;

    if (!container || mapRef.current) {
      return;
    }

    const map = L.map(container, {
      center: INITIAL_CENTER,
      zoom: INITIAL_ZOOM,
      zoomControl: true,
      scrollWheelZoom: false,
    });

    L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png", {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: "abcd",
      maxZoom: 20,
    }).addTo(map);

    const layer = L.layerGroup().addTo(map);
    mapRef.current = map;
    layerRef.current = layer;

    const publishViewport = () => {
      const bounds = map.getBounds();
      onBoundsChangeRef.current({
        minLat: bounds.getSouth(),
        maxLat: bounds.getNorth(),
        minLng: bounds.getWest(),
        maxLng: bounds.getEast(),
      });
    };

    const renderMarkers = () => {
      const activeMap = mapRef.current;
      const activeLayer = layerRef.current;

      if (!activeMap || !activeLayer) {
        return;
      }

      activeLayer.clearLayers();

      const cells = new Map<string, MapMarker[]>();

      for (const marker of markersRef.current) {
        if (!Number.isFinite(marker.latitude) || !Number.isFinite(marker.longitude)) {
          continue;
        }

        const point = activeMap.latLngToContainerPoint([marker.latitude, marker.longitude]);
        const key = `${Math.floor(point.x / CLUSTER_SIZE_PX)}:${Math.floor(
          point.y / CLUSTER_SIZE_PX,
        )}`;
        const entries = cells.get(key) ?? [];
        entries.push(marker);
        cells.set(key, entries);
      }

      for (const entries of cells.values()) {
        if (entries.length === 1) {
          const entry = entries[0];
          const selected = entry.companyId === selectedCompanyIdRef.current;
          const marker = L.marker([entry.latitude, entry.longitude], {
            icon: singleMarkerIcon(selected),
            title: entry.companyName,
            alt: `${entry.companyName} location`,
            zIndexOffset: selected ? 1000 : 0,
          });

          marker.on("click", () => onSelectCompanyRef.current(entry.companyId));
          marker.addTo(activeLayer);
          continue;
        }

        const latitude =
          entries.reduce((sum, entry) => sum + entry.latitude, 0) / entries.length;
        const longitude =
          entries.reduce((sum, entry) => sum + entry.longitude, 0) / entries.length;
        const cluster = L.marker([latitude, longitude], {
          icon: clusterIcon(entries.length),
          title: `${entries.length} startups in this area`,
          alt: `${entries.length} startups in this area`,
        });

        cluster.on("click", () => {
          const bounds = L.latLngBounds(
            entries.map((entry) => [entry.latitude, entry.longitude] as [number, number]),
          );
          map.flyToBounds(bounds, { padding: [48, 48], maxZoom: 15, duration: 0.4 });
        });
        cluster.addTo(activeLayer);
      }
    };

    renderRef.current = renderMarkers;
    renderMarkers();

    map.on("moveend", () => {
      renderMarkers();
      publishViewport();
    });

    publishViewport();

    return () => {
      renderRef.current = () => undefined;
      map.off();
      map.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  useEffect(() => {
    renderRef.current();
  }, [markers, selectedCompanyId]);

  return (
    <div
      ref={containerRef}
      role="region"
      aria-label="Map of verified startup office locations in Pune"
      className="z-0 h-full w-full bg-slate-100"
    />
  );
});
