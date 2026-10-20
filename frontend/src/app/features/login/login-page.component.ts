import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthSessionService } from '../../core/auth/auth-session.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
})
export class LoginPageComponent {
  private readonly session = inject(AuthSessionService);
  private readonly router = inject(Router);

  login(): void {
    this.session.loginMock();
    void this.router.navigate(['/home']);
  }
}
