import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  credentials = { username: '', password: '' };
  errorMessage = '';

  constructor(private auth: AuthService) {}

  onSignIn() {
    // success is now a boolean, so truthiness testing works
    const success = this.auth.login(this.credentials.username, this.credentials.password);
    
    if (!success) {
      this.errorMessage = 'Invalid username or password. Try: admin / password123';
    } else {
      this.errorMessage = '';
    }
  }
}