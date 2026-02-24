import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../services/data.service'; // Import this

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent {
  isOpen = false;
  userInput = '';
  isTyping = false; // Add this to show a "loading" state
  messages: { text: string, sender: 'user' | 'bot' }[] = [
    { text: 'Hello! I am your AI Health Assistant. How can I help you today?', sender: 'bot' }
  ];

  constructor(private dataService: DataService) {} // Inject service

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    if (!this.userInput.trim() || this.isTyping) return;

    const userMsg = this.userInput;
    this.messages.push({ text: userMsg, sender: 'user' });
    this.userInput = '';
    this.isTyping = true;

    // Call the Backend
    this.dataService.chatWithAI(userMsg).subscribe({
      next: (res) => {
        this.messages.push({ text: res.reply, sender: 'bot' });
        this.isTyping = false;
      },
      error: (err) => {
        this.messages.push({ text: "Sorry, I'm having trouble connecting to my brain right now.", sender: 'bot' });
        this.isTyping = false;
        console.error(err);
      }
    });
  }
}