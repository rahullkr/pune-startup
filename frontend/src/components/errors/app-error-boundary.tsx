import { Component, type ErrorInfo, type ReactNode } from "react";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/states";

interface Props {
  children: ReactNode;
}

interface State {
  reference: string | null;
}

function referenceFor(error: unknown): string {
  if (error instanceof ApiError && error.requestId) {
    return error.requestId;
  }
  return `ui-${crypto.randomUUID()}`;
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { reference: null };

  static getDerivedStateFromError(error: unknown): State {
    return { reference: referenceFor(error) };
  }

  componentDidCatch(error: unknown, info: ErrorInfo) {
    console.error("Unhandled UI error", {
      reference: this.state.reference,
      error,
      componentStack: info.componentStack,
    });
  }

  render() {
    if (!this.state.reference) {
      return this.props.children;
    }

    return (
      <main className="flex min-h-screen items-center justify-center bg-stone-50 px-6 py-12">
        <div className="w-full max-w-lg">
          <ErrorState
            title="Something went wrong"
            description="Reload the page to try again. If it keeps happening, share the reference below so we can trace it."
            action={
              <Button variant="brand" onClick={() => window.location.reload()}>
                Reload page
              </Button>
            }
          />
          <p className="mt-4 text-center text-xs text-slate-500">
            Reference: <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono">{this.state.reference}</code>
          </p>
        </div>
      </main>
    );
  }
}
