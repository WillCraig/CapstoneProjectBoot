import { isApiUrl } from './api-url';

describe('isApiUrl', () => {
  it('matches the absolute development API origin only', () => {
    expect(isApiUrl('http://localhost:8080/api/v1/interactions', 'http://localhost:8080')).toBeTrue();
    expect(isApiUrl('https://example.invalid/api/v1/interactions', 'http://localhost:8080')).toBeFalse();
  });

  it('matches same-origin production API paths when the base is empty', () => {
    expect(isApiUrl('/api/v1/interactions', '')).toBeTrue();
    expect(isApiUrl('/assets/logo.svg', '')).toBeFalse();
  });
});
