// Dev-server proxy for the Selenium run: the e2e build uses the production
// environment (apiBaseUrl ''), so the browser calls same-origin /api/** exactly
// like it does behind the k3s Ingress, and no CORS entry is needed.
export default {
  '/api': {
    target: process.env.CRM_E2E_API_TARGET ?? 'http://localhost:8080',
    secure: false,
    // Keep the browser's Host header (like Traefik does). With changeOrigin the
    // backend would see Origin :4300 vs Host :8080, treat the call as
    // cross-origin, and reject it with 403 "Invalid CORS request".
    changeOrigin: false,
  },
};
