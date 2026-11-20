import { Building2, ChevronLeft, ChevronRight, Search, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { CompanyCardSkeleton } from "@/features/companies/components/company-card-skeleton";
import { CompanyPreviewCard } from "@/features/companies/components/company-preview-card";
import { useCompanies } from "@/hooks/use-companies";
import { useCompanyAreas } from "@/hooks/use-company-areas";
import { useIndustries } from "@/hooks/use-industries";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 12;
const controlClassName =
  "h-11 w-full rounded-full border border-slate-300 bg-white px-4 text-sm text-slate-900 transition focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400";

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-900 py-1 pl-3 pr-1.5 text-xs font-medium text-white">
      {label}
      <button
        type="button"
        onClick={onRemove}
        aria-label={`Remove ${label} filter`}
        className="grid h-5 w-5 place-items-center rounded-full transition hover:bg-white/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
      >
        <X aria-hidden="true" className="h-3 w-3" />
      </button>
    </span>
  );
}

export function StartupsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const keyword = searchParams.get("q")?.trim() ?? "";
  const industry = searchParams.get("industry") ?? "";
  const area = searchParams.get("area") ?? "";
  const sort = searchParams.get("sort") === "updated" ? "updated" : "name";
  const parsedPage = Number.parseInt(searchParams.get("page") ?? "0", 10);
  const page = Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 0;

  const [keywordInput, setKeywordInput] = useState(keyword);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  const updateFilters = useCallback(
    (updates: Record<string, string | null>) => {
      setSearchParams(
        (current) => {
          const next = new URLSearchParams(current);

          for (const [name, value] of Object.entries(updates)) {
            if (value) {
              next.set(name, value);
            } else {
              next.delete(name);
            }
          }

          next.delete("page");
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  useEffect(() => {
    const nextKeyword = keywordInput.trim();

    if (nextKeyword === keyword) {
      return;
    }

    const timer = window.setTimeout(() => {
      updateFilters({ q: nextKeyword || null });
    }, 350);

    return () => window.clearTimeout(timer);
  }, [keywordInput, keyword, updateFilters]);

  const companiesQuery = useCompanies({
    keyword: keyword || undefined,
    industry: industry || undefined,
    area: area || undefined,
    sort,
    page,
    size: PAGE_SIZE,
  });
  const industriesQuery = useIndustries();
  const areasQuery = useCompanyAreas();

  const hasFilters = Boolean(keyword || industry || area);
  const activeIndustryName =
    industriesQuery.data?.find((option) => option.slug === industry)?.name ?? industry;
  const hasFilterOptionError = industriesQuery.isError || areasQuery.isError;

  function clearFilters() {
    setKeywordInput("");
    setSearchParams({}, { replace: true });
  }

  function setPage(nextPage: number) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);

      if (nextPage > 0) {
        next.set("page", String(nextPage));
      } else {
        next.delete("page");
      }

      return next;
    });
  }

  const data = companiesQuery.data;

  return (
    <section className="container-page py-12 sm:py-16">
      <div className="max-w-2xl">
        <Badge>Pune directory</Badge>
        <h1 className="mt-4 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">
          Find your next startup.
        </h1>
        <p className="mt-4 text-base leading-relaxed text-slate-600 sm:text-lg">
          Search Pune startups by name, industry, and area. Every listing keeps its source, and open
          roles stay tied to the company behind them.
        </p>
      </div>

      <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-900/5 sm:p-5">
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1.5fr)_repeat(3,minmax(0,1fr))]">
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Search
            </span>
            <span className="relative block">
              <Search
                aria-hidden="true"
                className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              />
              <input
                type="search"
                value={keywordInput}
                onChange={(event) => setKeywordInput(event.target.value)}
                placeholder="Startup name or keyword"
                className={cn(controlClassName, "pl-10 pr-10")}
              />
              {keywordInput ? (
                <button
                  type="button"
                  onClick={() => {
                    setKeywordInput("");
                    updateFilters({ q: null });
                  }}
                  aria-label="Clear search"
                  className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center rounded-full text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                >
                  <X aria-hidden="true" className="h-4 w-4" />
                </button>
              ) : null}
            </span>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Industry
            </span>
            <select
              value={industry}
              onChange={(event) => updateFilters({ industry: event.target.value || null })}
              disabled={industriesQuery.isPending}
              className={controlClassName}
            >
              <option value="">All industries</option>
              {industriesQuery.data?.map((option) => (
                <option key={option.id} value={option.slug}>
                  {option.name}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Area
            </span>
            <select
              value={area}
              onChange={(event) => updateFilters({ area: event.target.value || null })}
              disabled={areasQuery.isPending}
              className={controlClassName}
            >
              <option value="">All areas</option>
              {areasQuery.data?.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-slate-500">
              Sort
            </span>
            <select
              value={sort}
              onChange={(event) =>
                updateFilters({ sort: event.target.value === "updated" ? "updated" : null })
              }
              className={controlClassName}
            >
              <option value="name">A–Z</option>
              <option value="updated">Recently updated</option>
            </select>
          </label>
        </div>

        {hasFilterOptionError ? (
          <p className="mt-3 text-xs text-amber-700">
            Some filter options didn&apos;t load.{" "}
            <button
              type="button"
              className="font-semibold underline underline-offset-2"
              onClick={() => {
                if (industriesQuery.isError) {
                  void industriesQuery.refetch();
                }
                if (areasQuery.isError) {
                  void areasQuery.refetch();
                }
              }}
            >
              Retry
            </button>
          </p>
        ) : null}

        {hasFilters ? (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Active
            </span>
            {keyword ? (
              <FilterChip
                label={`“${keyword}”`}
                onRemove={() => {
                  setKeywordInput("");
                  updateFilters({ q: null });
                }}
              />
            ) : null}
            {industry ? (
              <FilterChip
                label={activeIndustryName}
                onRemove={() => updateFilters({ industry: null })}
              />
            ) : null}
            {area ? <FilterChip label={area} onRemove={() => updateFilters({ area: null })} /> : null}
            <button
              type="button"
              onClick={clearFilters}
              className="text-xs font-semibold text-brand-700 underline-offset-2 transition hover:text-brand-800 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              Clear all
            </button>
          </div>
        ) : null}
      </div>

      <div className="mt-8 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-600" role="status" aria-live="polite">
          {data
            ? `${data.totalElements} ${data.totalElements === 1 ? "startup" : "startups"} found`
            : "Loading startups…"}
        </p>
        {data && data.totalPages > 1 ? (
          <p className="text-sm text-slate-500">
            Page {page + 1} of {data.totalPages}
          </p>
        ) : null}
      </div>

      {companiesQuery.isPending ? (
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, index) => (
            <CompanyCardSkeleton key={index} />
          ))}
        </div>
      ) : null}

      {companiesQuery.isError ? (
        <ErrorState
          className="mt-6"
          title="Couldn't load the directory"
          description="The startup API didn't respond. Make sure the backend is running on port 8080, then try again."
          onRetry={() => void companiesQuery.refetch()}
        />
      ) : null}

      {data && data.content.length === 0 ? (
        <EmptyState
          className="mt-6"
          icon={<Building2 aria-hidden="true" className="h-8 w-8" />}
          title="No startups match those filters"
          description={
            hasFilters
              ? "Try a different keyword, industry, or area — or clear the filters to see everything."
              : "No startups are published yet. Check back once the directory has been seeded."
          }
          action={
            hasFilters ? (
              <Button variant="outline" size="sm" onClick={clearFilters}>
                Clear filters
              </Button>
            ) : undefined
          }
        />
      ) : null}

      {data && data.content.length > 0 ? (
        <div
          className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
          aria-busy={companiesQuery.isFetching}
        >
          {data.content.map((company) => (
            <CompanyPreviewCard key={company.id} company={company} />
          ))}
        </div>
      ) : null}

      {data && data.totalPages > 1 ? (
        <nav
          aria-label="Directory pagination"
          className="mt-10 flex items-center justify-between gap-4 border-t border-slate-200 pt-6"
        >
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page - 1)}
            disabled={page <= 0}
          >
            <ChevronLeft aria-hidden="true" className="h-4 w-4" />
            Previous
          </Button>
          <p className="text-sm font-medium text-slate-600">
            Page {page + 1} of {data.totalPages}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(page + 1)}
            disabled={!data.hasNext}
          >
            Next
            <ChevronRight aria-hidden="true" className="h-4 w-4" />
          </Button>
        </nav>
      ) : null}
    </section>
  );
}
