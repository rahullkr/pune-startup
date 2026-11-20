const dateFormatter = new Intl.DateTimeFormat("en-IN", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

export function formatDate(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return dateFormatter.format(date);
}

export function formatRelativeDate(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return null;
  }

  const days = Math.floor((Date.now() - date.getTime()) / 86_400_000);

  if (days <= 0) {
    return "today";
  }
  if (days === 1) {
    return "yesterday";
  }
  if (days < 7) {
    return `${days} days ago`;
  }
  if (days < 30) {
    const weeks = Math.floor(days / 7);
    return weeks === 1 ? "1 week ago" : `${weeks} weeks ago`;
  }
  if (days < 365) {
    const months = Math.floor(days / 30);
    return months === 1 ? "1 month ago" : `${months} months ago`;
  }

  return formatDate(value);
}

const enumLabels: Record<string, string> = {
  ONSITE: "On-site",
  HYBRID: "Hybrid",
  REMOTE: "Remote",
  FULL_TIME: "Full-time",
  PART_TIME: "Part-time",
  CONTRACT: "Contract",
  INTERNSHIP: "Internship",
  TEMPORARY: "Temporary",
  HEADQUARTERS: "Headquarters",
  OFFICE: "Office",
  COWORKING: "Coworking",
  OTHER: "Other",
  VERIFIED: "Verified",
  APPROXIMATE: "Approximate",
  PENDING: "Pending",
  FAILED: "Failed",
};

export function formatEnum(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  return (
    enumLabels[value] ??
    value
      .toLowerCase()
      .split("_")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ")
  );
}

function formatYears(value: number): string {
  return Number.isInteger(value) ? String(value) : String(Math.round(value * 10) / 10);
}

export function formatExperience(
  minYears: number | null,
  maxYears: number | null,
): string | null {
  if (minYears === null && maxYears === null) {
    return null;
  }

  if (minYears !== null && maxYears !== null) {
    if (minYears === maxYears) {
      return `${formatYears(minYears)} yrs`;
    }

    return `${formatYears(minYears)}–${formatYears(maxYears)} yrs`;
  }

  if (minYears !== null) {
    return `${formatYears(minYears)}+ yrs`;
  }

  return `Up to ${formatYears(maxYears as number)} yrs`;
}

const periodLabels: Record<string, string> = {
  YEARLY: "/ year",
  ANNUAL: "/ year",
  PER_ANNUM: "/ year",
  MONTHLY: "/ month",
  PER_MONTH: "/ month",
  WEEKLY: "/ week",
  DAILY: "/ day",
  HOURLY: "/ hour",
  PER_HOUR: "/ hour",
};

export function formatSalary(
  min: number | null,
  max: number | null,
  currency: string | null,
  period: string | null,
): string | null {
  if (min === null && max === null) {
    return null;
  }

  const formatter = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: currency ?? "INR",
    maximumFractionDigits: 0,
  });

  let range: string;

  if (min !== null && max !== null) {
    range = `${formatter.format(min)} – ${formatter.format(max)}`;
  } else if (min !== null) {
    range = `From ${formatter.format(min)}`;
  } else {
    range = `Up to ${formatter.format(max as number)}`;
  }

  const suffix = period ? (periodLabels[period] ?? ` / ${period.toLowerCase()}`) : "";

  return `${range}${suffix}`;
}

export function formatEmployeeCount(
  min: number | null,
  max: number | null,
): string | null {
  if (min === null && max === null) {
    return null;
  }

  if (min !== null && max !== null) {
    return `${min}–${max}`;
  }

  if (min !== null) {
    return `${min}+`;
  }

  return `Up to ${max}`;
}
