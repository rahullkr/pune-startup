import { Check, ClipboardList } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { createApplication } from "@/features/candidate/api";

export function TrackApplicationButton({
  jobId,
  size = "sm",
  variant = "outline",
}: {
  jobId: string;
  size?: "sm" | "md" | "lg";
  variant?: "outline" | "ghost" | "primary";
}) {
  const { candidate } = useAuth();
  const location = useLocation();
  const queryClient = useQueryClient();
  const [tracked, setTracked] = useState(false);
  const create = useMutation({
    mutationFn: () =>
      createApplication({
        jobId,
        status: "APPLIED",
        statusNote: "Tracked from the job page",
      }),
    onSuccess: () => {
      setTracked(true);
      void queryClient.invalidateQueries({ queryKey: ["applications"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  if (!candidate) {
    const next = `${location.pathname}${location.search}`;
    return (
      <Link
        to={`/login?next=${encodeURIComponent(next)}`}
        className={buttonVariants({ variant, size })}
      >
        <ClipboardList aria-hidden="true" className="h-4 w-4" />
        Sign in to track
      </Link>
    );
  }

  const alreadyTracked =
    tracked || (create.isError && create.error instanceof ApiError && create.error.status === 409);

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant={variant}
        size={size}
        onClick={() => create.mutate()}
        disabled={create.isPending || alreadyTracked}
      >
        {alreadyTracked ? (
          <Check aria-hidden="true" className="h-4 w-4" />
        ) : (
          <ClipboardList aria-hidden="true" className="h-4 w-4" />
        )}
        {alreadyTracked ? "Tracked" : "Track application"}
      </Button>
      {alreadyTracked ? (
        <Link
          to="/dashboard"
          className="text-xs font-semibold text-brand-700 hover:text-brand-800"
        >
          Open dashboard
        </Link>
      ) : null}
      {create.isError && !alreadyTracked ? (
        <span className="max-w-48 text-xs text-red-600">
          {create.error instanceof ApiError ? create.error.message : "Couldn't track this role."}
        </span>
      ) : null}
    </div>
  );
}
