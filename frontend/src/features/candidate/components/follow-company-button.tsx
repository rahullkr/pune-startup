import { Building2, Check } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useLocation } from "react-router-dom";
import { ApiError } from "@/api/client";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { followCompany, getFollowedCompany, unfollowCompany } from "@/features/candidate/api";

export function FollowCompanyButton({ companyId }: { companyId: string }) {
  const { candidate } = useAuth();
  const location = useLocation();
  const queryClient = useQueryClient();
  const followedQuery = useQuery({
    queryKey: ["followed-company", companyId],
    queryFn: ({ signal }) => getFollowedCompany(companyId, signal),
    enabled: Boolean(candidate),
    retry: false,
  });
  const followed = Boolean(followedQuery.data);
  const toggle = useMutation({
    mutationFn: async (): Promise<void> => {
      if (followed) {
        await unfollowCompany(companyId);
      } else {
        await followCompany(companyId);
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["followed-company", companyId] });
      void queryClient.invalidateQueries({ queryKey: ["followed-companies"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  if (!candidate) {
    const next = `${location.pathname}${location.search}`;
    return (
      <Link
        to={`/login?next=${encodeURIComponent(next)}`}
        className={buttonVariants({ variant: "outline" })}
      >
        <Building2 aria-hidden="true" className="h-4 w-4" />
        Sign in to follow
      </Link>
    );
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <Button
        variant={followed ? "outline" : "primary"}
        onClick={() => toggle.mutate()}
        disabled={toggle.isPending}
      >
        {followed ? (
          <Check aria-hidden="true" className="h-4 w-4" />
        ) : (
          <Building2 aria-hidden="true" className="h-4 w-4" />
        )}
        {followed ? "Following" : "Follow"}
      </Button>
      {toggle.isError ? (
        <span className="text-xs text-red-600">
          {toggle.error instanceof ApiError ? toggle.error.message : "Couldn't update follow."}
        </span>
      ) : null}
    </div>
  );
}
