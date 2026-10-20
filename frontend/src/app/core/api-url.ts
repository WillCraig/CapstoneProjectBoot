import { environment } from '../../environments/environment';

/**
 * Identifies first-party API requests in both configurations: an absolute
 * development base URL, or the production build's same-origin `/api` paths.
 */
export function isApiUrl(url: string, apiBaseUrl: string = environment.apiBaseUrl): boolean {
  return apiBaseUrl ? url.startsWith(apiBaseUrl) : url.startsWith('/api/');
}
