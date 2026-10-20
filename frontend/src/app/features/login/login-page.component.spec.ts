import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  it('sends a successful mock login to the customer choices page', async () => {
    const session = jasmine.createSpyObj<AuthSessionService>('AuthSessionService', ['loginMock']);
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        { provide: AuthSessionService, useValue: session },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click();

    expect(session.loginMock).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });
});
