import { HttpErrorResponse } from '@angular/common/http';

interface ProblemDetail {
  detail?: unknown;
  errors?: unknown;
}

/** Converts Spring Problem Details and transport failures into one readable UI message. */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return `${fallback}: cannot reach the API. Check that the backend is running.`;
  }
  if (error.status === 401) {
    return 'Your session is missing or expired. Please sign in again.';
  }
  const problem = error.error as ProblemDetail | null;
  const detail = typeof problem?.detail === 'string' ? problem.detail : null;
  const fieldErrors = Array.isArray(problem?.errors)
    ? problem.errors.filter((value): value is string => typeof value === 'string')
    : [];
  if (detail && fieldErrors.length > 0) {
    return `${detail}: ${fieldErrors.join('; ')}`;
  }
  if (detail) {
    return detail;
  }
  return `${fallback} (${error.status}).`;
}
