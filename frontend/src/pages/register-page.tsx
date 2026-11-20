import { ArrowRight, Sparkles } from "lucide-react";
import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";

function safeNext(value: string | null): string {
  if (value && value.startsWith("/") && !value.startsWith("//")) {
    return value;
  }
  return "/dashboard";
}

export function RegisterPage() {
  const { candidate, register } = useAuth();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const next = safeNext(searchParams.get("next"));
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (candidate) {
    return <Navigate to={next} replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await register({ fullName, email, password });
      navigate(next, { replace: true });
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Couldn't create your account. Try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="container-page py-12 sm:py-16">
      <div className="mx-auto grid max-w-4xl gap-8 lg:grid-cols-[minmax(0,1fr)_380px] lg:items-center">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-brand-700">
            <Sparkles aria-hidden="true" className="h-3.5 w-3.5" />
            Free candidate account
          </span>
          <h1 className="mt-4 text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
            Track the roles you actually want.
          </h1>
          <p className="mt-3 max-w-md text-sm leading-relaxed text-slate-600">
            Save interesting jobs, keep an eye on startups, and build a simple application history.
          </p>
        </div>

        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <h2 className="text-xl font-bold text-slate-900">Create account</h2>
          <p className="mt-1 text-sm text-slate-500">No resume upload required.</p>

          {error ? (
            <p role="alert" className="mt-4 rounded-2xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
              {error}
            </p>
          ) : null}

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            <label className="block text-sm font-medium text-slate-700">
              Full name
              <input
                type="text"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
                required
                maxLength={200}
                autoComplete="name"
                className="mt-1.5 h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Email
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
                className="mt-1.5 h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                className="mt-1.5 h-11 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200"
              />
              <span className="mt-1 block text-xs font-normal text-slate-500">
                At least 8 characters.
              </span>
            </label>
            <Button type="submit" variant="brand" className="w-full" disabled={submitting}>
              {submitting ? "Creating account…" : "Create account"}
              <ArrowRight aria-hidden="true" className="h-4 w-4" />
            </Button>
          </form>

          <p className="mt-5 text-center text-sm text-slate-600">
            Already registered?{" "}
            <Link
              to={`/login${next === "/dashboard" ? "" : `?next=${encodeURIComponent(next)}`}`}
              className="font-semibold text-brand-700 hover:text-brand-800"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </section>
  );
}
