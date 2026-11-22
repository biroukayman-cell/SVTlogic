export type AppLanguage = 'fr' | 'ar' | 'en';
export type ThemeMode = 'system' | 'light' | 'dark';
export type AppPalette = 'emerald' | 'ocean' | 'earth' | 'botanical';

export type UserRole = 'student' | 'teacher';
export type TeacherStatus = 'pending' | 'approved' | 'rejected';
export type EnrollmentStatus = 'pending' | 'approved' | 'rejected';

export type SvtDomain =
  | 'CELL_GENETICS'
  | 'IMMUNITY'
  | 'REPRODUCTION'
  | 'PHYSIOLOGY_IMMUNITY'
  | 'NERVOUS_MUSCULAR'
  | 'GEOLOGY'
  | 'ECOLOGY';

export const ALL_SVT_DOMAINS: SvtDomain[] = [
  'CELL_GENETICS',
  'IMMUNITY',
  'REPRODUCTION',
  'PHYSIOLOGY_IMMUNITY',
  'NERVOUS_MUSCULAR',
  'GEOLOGY',
  'ECOLOGY',
];

export type ResourceType = 'VIDEO' | 'PDF' | 'WORD' | 'IMAGE';
export type VideoStatus = 'not_started' | 'partial' | 'completed';

export type StudentTab =
  | 'exercises'
  | 'svt_lab'
  | 'resources'
  | 'announcements'
  | 'messaging'
  | 'profile';

export type TeacherTab =
  | 'dashboard'
  | 'exercises'
  | 'resources'
  | 'announcements'
  | 'messaging'
  | 'classes'
  | 'profile';

export interface UserProfile {
  userId: string;
  fullName: string;
  phoneNumber: string;
  role: UserRole;
  massarNumber: string;
  birthDate: string;
  email: string;
  establishment: string;
  teacherStatus: TeacherStatus;
  mustChangePassword: boolean;
  acceptedTerms: boolean;
  avatarUrl: string;
  xp: number;
  loginStreak: number;
  lastLoginDate: string;
  unlockedBadgeIds: string[];
  boundDeviceId: string;
  boundDeviceModel: string;
  createdAtMillis: number;
  updatedAtMillis: number;
}

export interface SvtClass {
  id: string;
  teacherId: string;
  teacherName: string;
  name: string;
  academicYear: string;
  level: string;
  qrToken: string;
  visibleLabDomains: SvtDomain[];
  createdAtMillis: number;
}

export interface ClassCodeLookup {
  qrToken: string;
  classId: string;
  className: string;
  academicYear: string;
  teacherId: string;
  teacherName: string;
}

export interface Enrollment {
  id: string;
  studentId: string;
  classId: string;
  className: string;
  teacherId: string;
  teacherName: string;
  studentName: string;
  studentPhone: string;
  massarNumber: string;
  qrToken: string;
  status: EnrollmentStatus;
  requestedAtMillis: number;
  validatedAtMillis: number | null;
}

export interface RosterStudent {
  id: string;
  classId: string;
  teacherId: string;
  massarNumber: string;
  fullName: string;
  phoneNumber: string;
  provisionalPassword: string;
  linkedStudentId: string;
  accountProvisioned: boolean;
  createdAtMillis: number;
}

export interface QuizQuestion {
  questionText: string;
  options: string[];
  correctIndex: number;
  explanation: string;
  imageUrl?: string;
}

export interface Quiz {
  id: string;
  classId: string;
  teacherId: string;
  title: string;
  description: string;
  chapterSvt: string;
  questions: QuizQuestion[];
  isDownloadable: boolean;
  approvedStudentIds: string[];
  createdAtMillis: number;
}

export interface QuestionAnswer {
  questionIndex: number;
  selectedIndex: number;
  isCorrect: boolean;
  timeSpentSeconds: number;
}

export interface QuizAttempt {
  id: string;
  quizId: string;
  classId: string;
  studentId: string;
  studentName: string;
  score: number;
  maxScore: number;
  totalDurationSeconds: number;
  answers: QuestionAnswer[];
  submittedAtMillis: number;
}

export interface CourseResource {
  id: string;
  classId: string;
  teacherId: string;
  title: string;
  description: string;
  type: ResourceType;
  fileUrl: string;
  durationSeconds: number;
  isDownloadable: boolean;
  approvedStudentIds: string[];
  createdAtMillis: number;
}

export interface VideoTracking {
  id: string;
  studentId: string;
  studentName: string;
  resourceId: string;
  classId: string;
  watchedSeconds: number;
  totalDurationSeconds: number;
  completionPercentage: number;
  status: VideoStatus;
  updatedAtMillis: number;
}

export interface Announcement {
  id: string;
  classId: string;
  teacherId: string;
  teacherName: string;
  title: string;
  content: string;
  approvedStudentIds: string[];
  createdAtMillis: number;
}

export interface PrivateMessage {
  id: string;
  classId: string;
  teacherId: string;
  studentId: string;
  studentName: string;
  senderId: string;
  senderRole: UserRole;
  text: string;
  isRead: boolean;
  createdAtMillis: number;
}

export interface StudentNotification {
  id: string;
  recipientId: string;
  classId: string;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAtMillis: number;
}

export interface ParsedQrPayload {
  classToken: string;
  massarNumber: string;
}

export interface ParsedRosterEntry {
  massarNumber: string;
  fullName: string;
  phoneNumber: string;
}
