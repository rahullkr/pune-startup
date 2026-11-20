import { MapPin } from "lucide-react";
import { Link } from "react-router-dom";

const footerLinks = [
  { to: "/", label: "Home" },
  { to: "/startups", label: "Startups" },
  { to: "/jobs", label: "Jobs" },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="container-page flex flex-col gap-10 py-12 md:flex-row md:items-start md:justify-between">
        <div className="max-w-sm">
          <Link to="/" className="flex items-center gap-2.5">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-slate-900 text-brand-300">
              <MapPin aria-hidden="true" className="h-5 w-5" />
            </span>
            <span className="text-sm font-bold tracking-tight text-slate-900">Pune Startup Arena</span>
          </Link>
          <p className="mt-4 text-sm leading-relaxed text-slate-600">
            A Pune-focused directory of startups, offices, and open roles. Listings carry their source
            and verification details so you can judge freshness yourself.
          </p>
        </div>

        <nav aria-label="Footer" className="flex flex-col gap-3">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Explore</p>
          {footerLinks.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className="w-fit rounded text-sm font-medium text-slate-700 transition-colors hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="max-w-xs">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Built in Pune</p>
          <p className="mt-3 text-sm leading-relaxed text-slate-600">
            Independent directory. Not affiliated with the companies listed. Missing data stays
            missing — we do not invent details.
          </p>
        </div>
      </div>

      <div className="border-t border-slate-200">
        <div className="container-page flex flex-col gap-2 py-5 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} Pune Startup Arena</p>
          <p>Startups · Jobs · Pune</p>
        </div>
      </div>
    </footer>
  );
}
