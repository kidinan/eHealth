import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DataService {
  private apiUrl = 'http://localhost:8080/api/v1';

  constructor(private http: HttpClient) {}

  getPatients(page: number, limit: number, search: string = ''): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString())
      .set('search', search); 
      
    return this.http.get(`${this.apiUrl}/patients`, { params });
  }

  chatWithAI(message: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/chat`, { message });
  }

  addPatient(patient: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/patients`, patient);
  }

  // NEW: Update Patient
  updatePatient(id: number, patient: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/patients/${id}`, patient);
  }

  // NEW: Delete Patient
deletePatient(id: number) {
  return this.http.delete(`${this.apiUrl}/patients/${id}`);
}
}