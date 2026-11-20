import { AlertTriangle, Inbox, Loader2 } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface BaseStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
  icon?: ReactNode;
  className?: string;
}

function StateFrame({ title, description, action, icon, className }: BaseStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-300 bg-white/70 px-6 py-12 text-center",
        className,
      )}
    >
      {icon ? <div className="mb-4 text-slate-400">{icon}</div> : null}
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {description ? (
        <p className="mt-2 max-w-md text-sm leading-relaxed text-slate-600">{description}</p>
      ) : null}
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  );
}

export function LoadingState({ label = "Loading…", className }: { label?: string; className?: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-3xl border border-dashed border-slate-300 bg-white/70 px-6 py-12 text-center",
        className,
      )}
    >
      <Loader2 aria-hidden="true" className="h-6 w-6 animate-spin text-brand-600" />
      <p className="text-sm font-medium text-slate-600">{label}</p>
    </div>
  );
}

export function EmptyState({ icon, ...props }: BaseStateProps) {
  return <StateFrame icon={icon ?? <Inbox aria-hidden="true" className="h-8 w-8" />} {...props} />;
}

interface ErrorStateProps extends Omit<BaseStateProps, "icon"> {
  onRetry?: () => void;
  retryLabel?: string;
}

export function ErrorState({
  title = "Something went wrong",
  description,
  action,
  onRetry,
  retryLabel = "Try again",
  className,
}: ErrorStateProps) {
  return (
    <StateFrame
      title={title}
      description={description}
      className={className}
      icon={<AlertTriangle aria-hidden="true" className="h-8 w-8 text-amber-500" />}
      action={
        action ??
        (onRetry ? (
          <Button variant="outline" size="sm" onClick={onRetry}>
            {retryLabel}
          </Button>
        ) : undefined)
      }
    />
  );
}
