import { ArrowRight, Compass } from "lucide-react";
import { Link } from "react-router-dom";
import { buttonVariants } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/states";

export function NotFoundPage() {
  return (
    <section className="container-page py-20 sm:py-28">
      <EmptyState
        className="mx-auto max-w-2xl"
        icon={<Compass aria-hidden="true" className="h-8 w-8" />}
        title="That page isn't on the map"
        description="The link may be out of date, or the page may not exist yet. Head back to the landing page and start from there."
        action={
          <Link to="/" className={buttonVariants({ variant: "brand" })}>
            Back to home
            <ArrowRight aria-hidden="true" className="h-4 w-4" />
          </Link>
        }
      />
    </section>
  );
}
