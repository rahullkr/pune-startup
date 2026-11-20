import { Skeleton } from "@/components/ui/skeleton";

export function JobCardSkeleton() {
  return (
    <div className="flex h-full flex-col rounded-3xl border border-slate-200 bg-white p-5">
      <div className="flex items-start gap-3">
        <Skeleton className="h-11 w-11 shrink-0 rounded-xl" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-5 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      </div>
      <Skeleton className="mt-4 h-3.5 w-1/3" />
      <div className="mt-4 flex gap-2">
        <Skeleton className="h-6 w-16 rounded-full" />
        <Skeleton className="h-6 w-20 rounded-full" />
        <Skeleton className="h-6 w-14 rounded-full" />
      </div>
      <div className="mt-5 border-t border-slate-100 pt-4">
        <Skeleton className="h-4 w-2/5" />
      </div>
    </div>
  );
}
