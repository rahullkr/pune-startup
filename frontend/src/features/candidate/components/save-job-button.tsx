import { Bookmark, BookmarkCheck } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useLocation } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { getSavedJob, saveJob, unsaveJob } from "@/features/candidate/api";

export function SaveJobButton({ jobId }: { jobId: string }) {
  const { candidate } = useAuth();
  const location = useLocation();
  const queryClient = useQueryClient();
  const savedQuery = useQuery({
    queryKey: ["saved-job", jobId],
    queryFn: ({ signal }) => getSavedJob(jobId, signal),
    enabled: Boolean(candidate),
    retry: false,
  });
  const saved = Boolean(savedQuery.data);
  const toggle = useMutation({
    mutationFn: async (): Promise<void> => {
      if (saved) {
        await unsaveJob(jobId);
      } else {
        await saveJob(jobId);
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["saved-job", jobId] });
      void queryClient.invalidateQueries({ queryKey: ["saved-jobs"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  if (!candidate) {
    const next = `${location.pathname}${location.search}`;
    return (
      <Link
        to={`/login?next=${encodeURIComponent(next)}`}
        className={buttonVariants({ variant: "outline", size: "sm" })}
      >
        <Bookmark aria-hidden="true" className="h-4 w-4" />
        Sign in to save
      </Link>
    );
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant={saved ? "outline" : "brand"}
        size="sm"
        onClick={() => toggle.mutate()}
        disabled={toggle.isPending}
      >
        {saved ? (
          <BookmarkCheck aria-hidden="true" className="h-4 w-4" />
        ) : (
          <Bookmark aria-hidden="true" className="h-4 w-4" />
        )}
        {saved ? "Saved" : "Save job"}
      </Button>
      {toggle.isError ? (
        <span className="text-xs text-red-600">
          {toggle.error instanceof ApiError ? toggle.error.message : "Couldn't update saved job."}
        </span>
      ) : null}
    </div>
  );
}
