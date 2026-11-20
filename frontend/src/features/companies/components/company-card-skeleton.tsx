import { Skeleton } from "@/components/ui/skeleton";

export function CompanyCardSkeleton() {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6">
      <div className="flex items-center gap-3">
        <Skeleton className="h-11 w-11 rounded-xl" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      </div>
      <Skeleton className="mt-5 h-3 w-1/2" />
      <Skeleton className="mt-6 h-7 w-40 rounded-full" />
    </div>
  );
}
