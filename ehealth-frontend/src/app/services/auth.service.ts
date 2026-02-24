import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Logic is now stored in RAM, not on the hard drive
  private isUserAuthenticated = false;

  constructor(private router: Router) {}

  login(username: string, password: string): boolean {
    if (username === 'admin' && password === 'password123') {
      this.isUserAuthenticated = true;
      this.router.navigate(['/dashboard']);
      return true;
    }
    return false;
  }

  isLoggedIn(): boolean {
    return this.isUserAuthenticated;
  }

  logout() {
    this.isUserAuthenticated = false;
    this.router.navigate(['/login']);
  }
}