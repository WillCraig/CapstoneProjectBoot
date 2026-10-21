import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage } from './api-error';

describe('apiErrorMessage', () => {
  it('shows Spring Problem Details with field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        detail: 'Validation failed',
        errors: ['summary: must not be blank'],
      },
    });

    expect(apiErrorMessage(error, 'Could not save')).toBe(
      'Validation failed: summary: must not be blank',
    );
  });

  it('explains network and authentication failures without raw HTTP details', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }), 'Could not load')).toContain(
      'cannot reach the API',
    );
    expect(apiErrorMessage(new HttpErrorResponse({ status: 401 }), 'Could not load')).toContain(
      'sign in again',
    );
  });
});
