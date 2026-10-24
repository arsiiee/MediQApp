export type UserRole = 'patient' | 'doctor' | 'secretary' | 'admin';

export interface UserProfile {
  id: string;
  username: string;
  fullName: string;
  email?: string;
  mobileNumber: string;
  dateOfBirth: string;
  role: UserRole;
  status: string;
  assignedDoctorId?: string;
}

export interface AuthSession {
  accessToken: string;
  expiresAt: string;
  refreshToken: string;
  refreshExpiresAt: string;
  profile: UserProfile;
}

export interface Doctor {
  id: string;
  userId: string;
  fullName: string;
  specialty: string;
  specializationId: string;
  licenseNumber: string;
  yearsOfExperience: number;
  feeCentavos: number;
  bio: string;
  building: string;
  floor: string;
  room: string;
  languages: string;
  status: string;
}

export interface Paged<T> {
  items: T[];
  total: number;
  limit: number;
  offset: number;
}

export interface Appointment {
  id: string;
  patientId: string;
  doctorId: string;
  startsAt: string;
  durationMinutes: number;
  status: string;
  reason: string;
  createdAt: string;
  updatedAt: string;
}

export interface MessageThread {
  id: string;
  patientId: string;
  doctorId: string;
  subject: string;
  lastMessageAt: string;
  unreadCount: number;
  awaitingAction: boolean;
}
