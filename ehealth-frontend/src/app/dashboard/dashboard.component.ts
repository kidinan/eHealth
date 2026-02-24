import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { DataService } from '../services/data.service';
import { AuthService } from '../services/auth.service';
import { ChatbotComponent } from '../chatbot/chatbot.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatbotComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  patients: any[] = [];
  total = 0;
  page = 1;
  limit = 10;
  searchTerm = '';
  showForm = false;
  isEditing = false;
  editingId: number | null = null;
  newP = { name: '', dob: '', email: '', department: 'General' };
  
  private searchSubject = new Subject<string>();

  constructor(private dataService: DataService, public auth: AuthService) {}

  ngOnInit() {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(term => {
        this.searchTerm = term;
        this.page = 1; // Resets pagination when searching
        return this.dataService.getPatients(this.page, this.limit, term);
      })
    ).subscribe(res => {
      this.patients = res.data;
      this.total = res.total;
    });
    this.load();
  }

  onSearch(event: any) {
    this.searchSubject.next((event.target as HTMLInputElement).value);
  }

  onLimitChange() {
    this.page = 1; // Resets pagination when changing limit
    this.load();
  }

  load() {
    this.dataService.getPatients(this.page, this.limit, this.searchTerm).subscribe(res => {
      this.patients = res.data;
      this.total = res.total;
    });
  }

  openEdit(patient: any) {
    this.isEditing = true;
    this.editingId = patient.id;
    this.newP = { ...patient };
    this.showForm = true;
  }

deletePatient(id: number, event: any) {
  event.stopPropagation();
  if (window.confirm('Delete this patient?')) {
    this.dataService.deletePatient(id).subscribe({
      next: () => {
        // Instant UI Update: Filter the list locally
        this.patients = this.patients.filter(p => p.id !== id);
        this.total--;
      },
      error: (err) => window.alert("Delete failed on server.")
    });
  }
}

  save() {
    if (this.isEditing && this.editingId) {
      this.dataService.updatePatient(this.editingId, this.newP).subscribe(() => this.resetAndLoad());
    } else {
      this.dataService.addPatient(this.newP).subscribe(() => this.resetAndLoad());
    }
  }

  resetAndLoad() {
    this.showForm = false;
    this.isEditing = false;
    this.editingId = null;
    this.newP = { name: '', dob: '', email: '', department: 'General' };
    this.load();
  }

  move(step: number) {
    this.page += step;
    this.load();
  }
}