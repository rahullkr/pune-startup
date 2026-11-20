import {
  ArrowRight,
  ArrowUpRight,
  Briefcase,
  Building2,
  CheckCircle2,
  MapPin,
  ShieldCheck,
  Sparkles,
  type LucideIcon,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { EmptyState, ErrorState } from "@/components/ui/states";
import { CompanyCardSkeleton } from "@/features/companies/components/company-card-skeleton";
import { CompanyPreviewCard } from "@/features/companies/components/company-preview-card";
import { useCompanies } from "@/hooks/use-companies";
import { cn } from "@/lib/utils";

const trustPoints = [
  "Source and verification attached",
  "Expired roles stay hidden",
  "Built for Pune, not everywhere",
];

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
  to: string;
  cta: string;
}

const features: Feature[] = [
  {
    icon: Building2,
    title: "Startup directory",
    description:
      "Search Pune startups by name, industry, and area — with the details that actually help you shortlist.",
    to: "/startups",
    cta: "Browse startups",
  },
  {
    icon: Briefcase,
    title: "Job board",
    description:
      "Roles tied to real companies, with source links, freshness, and the original application destination.",
    to: "/jobs",
    cta: "Explore jobs",
  },
  {
    icon: ShieldCheck,
    title: "Source-backed data",
    description:
      "Every company and role keeps its provenance. Unknown details stay unknown instead of being invented.",
    to: "/startups",
    cta: "See how listings look",
  },
];

function HeroPanel() {
  return (
    <div className="relative">
      <div
        aria-hidden="true"
        className="absolute -inset-4 -z-10 rounded-[2rem] bg-gradient-to-br from-brand-200/70 via-white to-emerald-200/60 blur-2xl"
      />
      <div className="rounded-3xl border border-slate-200 bg-white/95 p-2 shadow-xl shadow-slate-900/5">
        <div className="rounded-[1.25rem] bg-slate-950 px-5 py-4 text-white">
          <div className="flex items-center justify-between gap-4 text-[11px] font-medium uppercase tracking-[0.2em] text-slate-400">
            <span>Pune</span>
            <span>18.52° N, 73.86° E</span>
          </div>
          <p className="mt-3 text-lg font-semibold">Your next move starts here.</p>
        </div>

        <div className="divide-y divide-slate-100 p-2">
          <Link
            to="/startups"
            className="group flex items-center gap-4 rounded-2xl px-3 py-4 transition-colors hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
              <Building2 aria-hidden="true" className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-slate-900">Browse startups</span>
              <span className="block text-sm text-slate-500">Company, industry, or area</span>
            </span>
            <ArrowUpRight
              aria-hidden="true"
              className="h-4 w-4 shrink-0 text-slate-400 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-brand-600"
            />
          </Link>

          <Link
            to="/jobs"
            className="group flex items-center gap-4 rounded-2xl px-3 py-4 transition-colors hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
              <Briefcase aria-hidden="true" className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-slate-900">Find a role</span>
              <span className="block text-sm text-slate-500">Mode, experience, and skills</span>
            </span>
            <ArrowUpRight
              aria-hidden="true"
              className="h-4 w-4 shrink-0 text-slate-400 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-brand-600"
            />
          </Link>

          <div className="flex items-center gap-4 rounded-2xl px-3 py-4">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700">
              <ShieldCheck aria-hidden="true" className="h-5 w-5" />
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm font-semibold text-slate-900">Source-backed listings</span>
              <span className="block text-sm text-slate-500">Every record carries its origin</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export function HomePage() {
  const { data, isPending, isError, refetch } = useCompanies({ size: 3, sort: "newest" });

  return (
    <>
      <section className="relative overflow-hidden border-b border-slate-200">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
          <div className="absolute -top-32 right-[-10%] h-80 w-80 rounded-full bg-brand-200/50 blur-3xl" />
          <div className="absolute bottom-[-12rem] left-[-6rem] h-96 w-96 rounded-full bg-emerald-200/40 blur-3xl" />
        </div>

        <div className="container-page grid gap-12 py-16 sm:py-20 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:py-24">
          <div>
            <Badge>
              <Sparkles aria-hidden="true" className="h-3.5 w-3.5" />
              Pune&apos;s startup discovery layer
            </Badge>
            <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl lg:text-6xl">
              Find your place in <span className="text-brand-600">Pune&apos;s startup scene.</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-slate-600">
              A source-backed directory of Pune startups, their offices, and the roles they are hiring
              for. Explore by company, by neighbourhood, or by what you want to build next.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/startups" className={buttonVariants({ variant: "brand", size: "lg" })}>
                Browse startups
                <ArrowRight aria-hidden="true" className="h-4 w-4" />
              </Link>
              <Link to="/jobs" className={buttonVariants({ variant: "outline", size: "lg" })}>
                Explore jobs
              </Link>
            </div>

            <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2">
              {trustPoints.map((point) => (
                <li key={point} className="flex items-center gap-2 text-sm font-medium text-slate-600">
                  <CheckCircle2 aria-hidden="true" className="h-4 w-4 text-emerald-600" />
                  {point}
                </li>
              ))}
            </ul>
          </div>

          <HeroPanel />
        </div>
      </section>

      <section className="container-page py-16 sm:py-20">
        <div className="max-w-2xl">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-700">
            Built for exploring
          </p>
          <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
            The city, without the guesswork.
          </h2>
          <p className="mt-3 text-base leading-relaxed text-slate-600">
            Start with the companies around you, then follow the trail to the teams and roles that fit.
          </p>
        </div>

        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {features.map((feature) => (
            <article
              key={feature.title}
              className="group flex flex-col rounded-3xl border border-slate-200 bg-white p-6 transition hover:-translate-y-0.5 hover:shadow-lg hover:shadow-slate-900/5"
            >
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-slate-900 text-brand-300">
                <feature.icon aria-hidden="true" className="h-6 w-6" />
              </span>
              <h3 className="mt-5 text-lg font-semibold text-slate-900">{feature.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{feature.description}</p>
              <Link
                to={feature.to}
                className="mt-5 inline-flex w-fit items-center gap-1.5 rounded text-sm font-semibold text-brand-700 transition-colors hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
              >
                {feature.cta}
                <ArrowRight
                  aria-hidden="true"
                  className="h-4 w-4 transition-transform group-hover:translate-x-0.5"
                />
              </Link>
            </article>
          ))}
        </div>
      </section>

      <section className="border-y border-slate-200 bg-white/70 py-16 sm:py-20">
        <div className="container-page">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-700">
                Fresh from the directory
              </p>
              <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-900">
                Newest startups
              </h2>
              <p className="mt-2 text-sm text-slate-600">
                Pulled live from the company API — the same data the directory will use.
              </p>
            </div>
            <Link
              to="/startups"
              className="inline-flex w-fit items-center gap-1.5 rounded text-sm font-semibold text-brand-700 transition-colors hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              View the directory
              <ArrowRight aria-hidden="true" className="h-4 w-4" />
            </Link>
          </div>

          {isPending ? (
            <div className="mt-8 grid gap-5 md:grid-cols-3">
              {[0, 1, 2].map((index) => (
                <CompanyCardSkeleton key={index} />
              ))}
            </div>
          ) : null}

          {isError ? (
            <ErrorState
              className="mt-8"
              title="Couldn't load startups"
              description="The startup API didn't respond. Make sure the backend is running on port 8080, then try again."
              onRetry={() => void refetch()}
            />
          ) : null}

          {data && data.content.length === 0 ? (
            <EmptyState
              className="mt-8"
              title="No startups published yet"
              description="Once companies are live in the directory, the newest ones will show up here."
            />
          ) : null}

          {data && data.content.length > 0 ? (
            <div className="mt-8 grid gap-5 md:grid-cols-3">
              {data.content.map((company) => (
                <CompanyPreviewCard key={company.id} company={company} />
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <section className="container-page py-16 sm:py-24">
        <div className="overflow-hidden rounded-3xl bg-slate-950 px-6 py-12 text-center sm:px-12">
          <p className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-brand-300">
            <MapPin aria-hidden="true" className="h-4 w-4" />
            Built in and for Pune
          </p>
          <h2 className="mx-auto mt-4 max-w-2xl text-3xl font-bold tracking-tight text-white sm:text-4xl">
            Start with the city you already know.
          </h2>
          <p className="mx-auto mt-4 max-w-2xl text-base leading-relaxed text-slate-300">
            Browse the startups around you, then find the role that fits what you want to build next.
          </p>
          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link to="/startups" className={buttonVariants({ variant: "brand", size: "lg" })}>
              Browse startups
              <ArrowRight aria-hidden="true" className="h-4 w-4" />
            </Link>
            <Link
              to="/jobs"
              className={cn(
                buttonVariants({ variant: "outline", size: "lg" }),
                "border-slate-700 bg-transparent text-white hover:border-slate-500 hover:bg-white/10 hover:text-white",
              )}
            >
              Explore jobs
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
