import { Menu, MapPin, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

const navItems = [
  { to: "/", label: "Home", end: true },
  { to: "/startups", label: "Startups" },
  { to: "/jobs", label: "Jobs" },
  { to: "/map", label: "Map" },
];

export function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { candidate, logout } = useAuth();

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-stone-50/85 backdrop-blur">
      <div className="container-page flex h-16 items-center justify-between gap-4">
        <Link
          to="/"
          className="group flex items-center gap-2.5 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
        >
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-slate-900 text-brand-300 shadow-sm transition-transform group-hover:-translate-y-0.5">
            <MapPin aria-hidden="true" className="h-5 w-5" />
          </span>
          <span className="leading-tight">
            <span className="block text-sm font-bold tracking-tight text-slate-900">
              Pune Startup Arena
            </span>
            <span className="block text-[11px] font-medium uppercase tracking-[0.18em] text-slate-500">
              Startups · Jobs · Pune
            </span>
          </span>
        </Link>

        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  "rounded-full px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500",
                  isActive
                    ? "bg-slate-900 text-white"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
          <Link
            to="/startups"
            className={cn(buttonVariants({ variant: "brand", size: "sm" }), "ml-2")}
          >
            Browse startups
          </Link>
          {candidate ? (
            <>
              <Link to="/dashboard" className={buttonVariants({ variant: "primary", size: "sm" })}>
                Dashboard
              </Link>
              <button
                type="button"
                onClick={() => void handleLogout()}
                className={buttonVariants({ variant: "ghost", size: "sm" })}
              >
                Sign out
              </button>
            </>
          ) : (
            <Link to="/login" className={buttonVariants({ variant: "outline", size: "sm" })}>
              Sign in
            </Link>
          )}
        </nav>

        <button
          type="button"
          className={cn(buttonVariants({ variant: "outline", size: "icon" }), "md:hidden")}
          aria-expanded={menuOpen}
          aria-controls="mobile-nav"
          aria-label={menuOpen ? "Close menu" : "Open menu"}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? (
            <X aria-hidden="true" className="h-5 w-5" />
          ) : (
            <Menu aria-hidden="true" className="h-5 w-5" />
          )}
        </button>
      </div>

      {menuOpen ? (
        <div id="mobile-nav" className="border-t border-slate-200 bg-stone-50 md:hidden">
          <nav aria-label="Mobile" className="container-page flex flex-col gap-1 py-3">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    "rounded-xl px-4 py-3 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500",
                    isActive
                      ? "bg-slate-900 text-white"
                      : "text-slate-700 hover:bg-slate-100 hover:text-slate-900",
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
            <Link
              to="/startups"
              className={cn(buttonVariants({ variant: "brand" }), "mt-2")}
            >
              Browse startups
            </Link>
            {candidate ? (
              <>
                <Link to="/dashboard" className={cn(buttonVariants({ variant: "primary" }), "mt-1")}>
                  Dashboard
                </Link>
                <button
                  type="button"
                  onClick={() => void handleLogout()}
                  className={cn(buttonVariants({ variant: "ghost" }), "mt-1")}
                >
                  Sign out
                </button>
              </>
            ) : (
              <Link to="/login" className={cn(buttonVariants({ variant: "outline" }), "mt-1")}>
                Sign in
              </Link>
            )}
          </nav>
        </div>
      ) : null}
    </header>
  );
}
