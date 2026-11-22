import {
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updatePassword,
  User,
} from 'firebase/auth';
import {
  Bytes,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  Unsubscribe,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';
import { auth, db, getProvisionerAuth, googleProvider } from './firebase';
import {
  ALL_SVT_DOMAINS,
  Announcement,
  ClassCodeLookup,
  CourseResource,
  Enrollment,
  ParsedRosterEntry,
  PrivateMessage,
  QuestionAnswer,
  Quiz,
  QuizAttempt,
  QuizQuestion,
  ResourceType,
  RosterStudent,
  StudentNotification,
  SvtClass,
  SvtDomain,
  TeacherStatus,
  UserProfile,
  UserRole,
  VideoStatus,
  VideoTracking,
} from '../types/svt';
import {
  buildStudentQrPayload,
  defaultStudentTaalimEmail,
  generateProvisionalPassword,
  generateQrToken,
  getOrCreateWebDeviceId,
  getWebDeviceLabel,
  massarToAuthEmail,
  normalizeMassar,
  OWNER_ADMIN_EMAIL,
  parseQrPayload,
  todayIsoDate,
} from './authHelpers';
import { evaluateBadgeUnlocks } from './badges';
import { CHUNK_SIZE_BYTES, CHUNK_URL_PREFIX, MAX_UPLOAD_BYTES, parseChunkResourceUrl, getMimeTypeForResource } from './fileUtils';

function tsToMillis(v: unknown): number {
  if (v instanceof Timestamp) return v.toMillis();
  if (v && typeof v === 'object' && 'toMillis' in v && typeof (v as any).toMillis === 'function') {
    return (v as any).toMillis();
  }
  if (typeof v === 'number') return v;
  return Date.now();
}

function requireFirebase(): { auth: NonNullable<typeof auth>; db: NonNullable<typeof db> } {
  if (!auth || !db) {
    throw new Error('Firebase Web n’est pas configuré (.env VITE_FIREBASE_API_KEY / VITE_FIREBASE_APP_ID).');
  }
  return { auth, db };
}

function mapUserProfile(uid: string, d: Record<string, any>): UserProfile {
  return {
    userId: d.userId || uid,
    fullName: d.fullName || '',
    phoneNumber: d.phoneNumber || '',
    role: d.role === 'teacher' ? 'teacher' : 'student',
    massarNumber: d.massarNumber || '',
    birthDate: d.birthDate || '',
    email: d.email || '',
    establishment: d.establishment || '',
    teacherStatus: (d.teacherStatus as TeacherStatus) || (d.role === 'teacher' ? 'pending' : 'approved'),
    mustChangePassword: Boolean(d.mustChangePassword),
    acceptedTerms: d.acceptedTerms === undefined ? true : Boolean(d.acceptedTerms),
    avatarUrl: d.avatarUrl || '',
    xp: typeof d.xp === 'number' ? d.xp : 0,
    loginStreak: typeof d.loginStreak === 'number' ? d.loginStreak : 1,
    lastLoginDate: d.lastLoginDate || todayIsoDate(),
    unlockedBadgeIds: Array.isArray(d.unlockedBadgeIds) ? d.unlockedBadgeIds : [],
    boundDeviceId: d.boundDeviceId || '',
    boundDeviceModel: d.boundDeviceModel || '',
    createdAtMillis: tsToMillis(d.createdAt),
    updatedAtMillis: tsToMillis(d.updatedAt),
  };
}

function mapClass(id: string, d: Record<string, any>): SvtClass {
  const domains: SvtDomain[] =
    Array.isArray(d.visibleLabDomains) && d.visibleLabDomains.length > 0
      ? d.visibleLabDomains.filter((x: string) => ALL_SVT_DOMAINS.includes(x as SvtDomain))
      : ALL_SVT_DOMAINS;
  return {
    id,
    teacherId: d.teacherId || '',
    teacherName: d.teacherName || '',
    name: d.name || '',
    academicYear: d.academicYear || '',
    level: d.level || '',
    qrToken: d.qrToken || '',
    visibleLabDomains: domains.length > 0 ? domains : ALL_SVT_DOMAINS,
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapEnrollment(id: string, d: Record<string, any>): Enrollment {
  return {
    id,
    studentId: d.studentId || id,
    classId: d.classId || '',
    className: d.className || '',
    teacherId: d.teacherId || '',
    teacherName: d.teacherName || '',
    studentName: d.studentName || '',
    studentPhone: d.studentPhone || '',
    massarNumber: d.massarNumber || '',
    qrToken: d.qrToken || '',
    status: d.status || 'pending',
    requestedAtMillis: tsToMillis(d.requestedAt),
    validatedAtMillis: d.validatedAt ? tsToMillis(d.validatedAt) : null,
  };
}

function mapRoster(id: string, d: Record<string, any>): RosterStudent {
  const studentUid = d.studentUid || '';
  return {
    id,
    classId: d.classId || '',
    teacherId: d.teacherId || '',
    massarNumber: d.massarNumber || '',
    fullName: d.fullName || '',
    phoneNumber: '',
    provisionalPassword: d.provisionalPassword || '',
    linkedStudentId: studentUid,
    accountProvisioned: Boolean(studentUid),
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapQuiz(id: string, d: Record<string, any>): Quiz {
  const questions: QuizQuestion[] = Array.isArray(d.questions)
    ? d.questions.map((q: any) => ({
        questionText: q.questionText || '',
        options: Array.isArray(q.options) ? q.options : [],
        correctIndex: typeof q.correctIndex === 'number' ? q.correctIndex : 0,
        explanation: q.explanation || '',
        imageUrl: q.imageUrl || undefined,
      }))
    : [];
  return {
    id,
    classId: d.classId || '',
    teacherId: d.teacherId || '',
    title: d.title || '',
    description: d.description || '',
    chapterSvt: d.chapterSvt || 'SVT',
    questions,
    isDownloadable: Boolean(d.isDownloadable),
    approvedStudentIds: Array.isArray(d.approvedStudentIds) ? d.approvedStudentIds : [],
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapAttempt(id: string, d: Record<string, any>): QuizAttempt {
  return {
    id,
    quizId: d.quizId || '',
    classId: d.classId || '',
    studentId: d.studentId || '',
    studentName: d.studentName || '',
    score: typeof d.score === 'number' ? d.score : 0,
    maxScore: typeof d.maxScore === 'number' ? d.maxScore : 1,
    totalDurationSeconds: typeof d.totalDurationSeconds === 'number' ? d.totalDurationSeconds : 0,
    answers: Array.isArray(d.answers) ? d.answers : [],
    submittedAtMillis: tsToMillis(d.completedAt || d.createdAt),
  };
}

function normalizeResourceTypeFromFirestore(raw: string): ResourceType {
  const up = (raw || 'pdf').toUpperCase();
  if (up === 'VIDEO' || up === 'PDF' || up === 'WORD' || up === 'IMAGE') return up;
  return 'PDF';
}

function mapResource(id: string, d: Record<string, any>): CourseResource {
  return {
    id,
    classId: d.classId || '',
    teacherId: d.teacherId || '',
    title: d.title || '',
    description: d.description || '',
    type: normalizeResourceTypeFromFirestore(d.type),
    fileUrl: d.fileUrl || '',
    durationSeconds: typeof d.durationSeconds === 'number' ? d.durationSeconds : 0,
    isDownloadable: Boolean(d.isDownloadable),
    approvedStudentIds: Array.isArray(d.approvedStudentIds) ? d.approvedStudentIds : [],
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapVideoTracking(id: string, d: Record<string, any>): VideoTracking {
  return {
    id,
    studentId: d.studentId || '',
    studentName: d.studentName || '',
    resourceId: d.resourceId || '',
    classId: d.classId || '',
    watchedSeconds: typeof d.maxPositionSeconds === 'number' ? d.maxPositionSeconds : 0,
    totalDurationSeconds: typeof d.durationSeconds === 'number' ? d.durationSeconds : 0,
    completionPercentage: typeof d.watchedPercentage === 'number' ? Math.round(d.watchedPercentage) : 0,
    status: (d.status as VideoStatus) || 'not_started',
    updatedAtMillis: tsToMillis(d.updatedAt),
  };
}

function mapAnnouncement(id: string, d: Record<string, any>): Announcement {
  return {
    id,
    classId: d.classId || '',
    teacherId: d.teacherId || '',
    teacherName: d.teacherName || 'Professeur SVT',
    title: d.title || '',
    content: d.content || '',
    approvedStudentIds: Array.isArray(d.approvedStudentIds) ? d.approvedStudentIds : [],
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapPrivateMessage(id: string, d: Record<string, any>): PrivateMessage {
  return {
    id,
    classId: d.classId || '',
    teacherId: d.teacherId || '',
    studentId: d.studentId || '',
    studentName: d.studentName || '',
    senderId: d.senderId || '',
    senderRole: d.senderRole === 'teacher' ? 'teacher' : 'student',
    text: d.content || '',
    isRead: Boolean(d.isRead),
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

function mapNotification(id: string, d: Record<string, any>): StudentNotification {
  return {
    id,
    recipientId: d.studentId || '',
    classId: d.classId || '',
    type: d.type || 'announcement',
    title: d.title || '',
    message: d.body || '',
    isRead: Boolean(d.isRead),
    createdAtMillis: tsToMillis(d.createdAt),
  };
}

// ---------- AUTHENTICATION & PROFILE ----------

export async function signInStudent(identifierRaw: string, password: string): Promise<User> {
  const { auth } = requireFirebase();
  const trimmed = identifierRaw.trim();
  const email = trimmed.includes('@') ? trimmed.toLowerCase() : massarToAuthEmail(trimmed);
  const cred = await signInWithEmailAndPassword(auth, email, password);
  return cred.user;
}

export async function signInTeacher(email: string, password: string): Promise<User> {
  const { auth } = requireFirebase();
  const cred = await signInWithEmailAndPassword(auth, email.trim().toLowerCase(), password);
  return cred.user;
}

export async function registerTeacher(params: {
  email: string;
  password: string;
  fullName: string;
  phoneNumber: string;
  establishment: string;
}): Promise<User> {
  const { auth, db } = requireFirebase();
  const cleanEmail = params.email.trim().toLowerCase();
  const cred = await createUserWithEmailAndPassword(auth, cleanEmail, params.password);
  const user = cred.user;
  const isOwner = cleanEmail === OWNER_ADMIN_EMAIL && user.emailVerified;

  await setDoc(doc(db, 'users', user.uid), {
    userId: user.uid,
    fullName: params.fullName.trim(),
    phoneNumber: params.phoneNumber.trim(),
    role: 'teacher',
    massarNumber: '',
    birthDate: '',
    email: cleanEmail,
    establishment: params.establishment.trim(),
    teacherStatus: isOwner ? 'approved' : 'pending',
    mustChangePassword: false,
    acceptedTerms: true,
    avatarUrl: null,
    xp: 0,
    loginStreak: 1,
    lastLoginDate: todayIsoDate(),
    unlockedBadgeIds: [],
    boundDeviceId: '',
    boundDeviceModel: '',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  return user;
}

export async function signInWithGoogleTeacher(fallbackProfile?: {
  fullName?: string;
  phoneNumber?: string;
  establishment?: string;
}): Promise<User> {
  const { auth, db } = requireFirebase();
  const cred = await signInWithPopup(auth, googleProvider);
  const user = cred.user;
  const snap = await getDoc(doc(db, 'users', user.uid));
  if (!snap.exists() && fallbackProfile && (fallbackProfile.phoneNumber || '').length >= 6) {
    const email = (user.email || '').toLowerCase();
    const isOwner = email === OWNER_ADMIN_EMAIL && user.emailVerified;
    await setDoc(doc(db, 'users', user.uid), {
      userId: user.uid,
      fullName: fallbackProfile.fullName?.trim() || user.displayName || 'Professeur SVT',
      phoneNumber: fallbackProfile.phoneNumber!.trim(),
      role: 'teacher',
      massarNumber: '',
      birthDate: '',
      email,
      establishment: fallbackProfile.establishment?.trim() || 'Établissement SVT',
      teacherStatus: isOwner ? 'approved' : 'pending',
      mustChangePassword: false,
      acceptedTerms: true,
      avatarUrl: null,
      xp: 0,
      loginStreak: 1,
      lastLoginDate: todayIsoDate(),
      unlockedBadgeIds: [],
      boundDeviceId: '',
      boundDeviceModel: '',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
  return user;
}

export async function completeProfileSetup(params: {
  uid: string;
  role: UserRole;
  fullName: string;
  phoneNumber: string;
  massarNumber?: string;
  birthDate?: string;
  email?: string;
  establishment?: string;
}): Promise<void> {
  const { auth, db } = requireFirebase();
  const currentUser = auth.currentUser;
  const cleanEmail = (params.email || currentUser?.email || '').trim().toLowerCase();
  const isOwner =
    params.role === 'teacher' && cleanEmail === OWNER_ADMIN_EMAIL && Boolean(currentUser?.emailVerified);
  const deviceId = params.role === 'student' ? getOrCreateWebDeviceId() : '';
  const deviceModel = params.role === 'student' ? getWebDeviceLabel() : '';
  const cleanMassar = params.massarNumber ? normalizeMassar(params.massarNumber) : '';
  const recoveryEmail =
    params.role === 'student' && !cleanEmail && cleanMassar
      ? defaultStudentTaalimEmail(cleanMassar)
      : cleanEmail;

  await setDoc(doc(db, 'users', params.uid), {
    userId: params.uid,
    fullName: params.fullName.trim(),
    phoneNumber: params.phoneNumber.trim(),
    role: params.role,
    massarNumber: cleanMassar,
    birthDate: (params.birthDate || '').trim(),
    email: recoveryEmail,
    establishment: (params.establishment || '').trim(),
    teacherStatus: params.role === 'teacher' ? (isOwner ? 'approved' : 'pending') : 'approved',
    mustChangePassword: false,
    acceptedTerms: params.role === 'teacher',
    avatarUrl: null,
    xp: 0,
    loginStreak: 1,
    lastLoginDate: todayIsoDate(),
    unlockedBadgeIds: [],
    boundDeviceId: deviceId,
    boundDeviceModel: deviceModel,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

export async function lookupClassByQrToken(rawPayload: string): Promise<{
  codeInfo: ClassCodeLookup;
  extractedMassar: string;
} | null> {
  const { db } = requireFirebase();
  const parsed = parseQrPayload(rawPayload);
  if (!parsed) return null;

  const snap = await getDoc(doc(db, 'class_codes', parsed.classToken));
  if (!snap.exists()) return null;
  const d = snap.data();
  return {
    codeInfo: {
      qrToken: parsed.classToken,
      classId: d.classId || '',
      className: d.className || '',
      academicYear: d.academicYear || '',
      teacherId: d.teacherId || '',
      teacherName: d.teacherName || '',
    },
    extractedMassar: parsed.massarNumber,
  };
}

export async function registerStudentWithQr(params: {
  qrInput: string;
  fullName: string;
  massarInput: string;
  password: string;
  recoveryEmail?: string;
  phoneNumber?: string;
}): Promise<void> {
  const { auth, db } = requireFirebase();
  const parsed = parseQrPayload(params.qrInput);
  if (!parsed) throw new Error('Code QR invalide ou introuvable.');

  const enteredMassar = normalizeMassar(params.massarInput || parsed.massarNumber);
  const effectiveMassar =
    enteredMassar.length >= 4
      ? enteredMassar
      : `QR${Math.random().toString(36).slice(2, 10).toUpperCase()}`;

  const authEmail = massarToAuthEmail(effectiveMassar);

  let user: User;
  try {
    const cred = await createUserWithEmailAndPassword(auth, authEmail, params.password);
    user = cred.user;
  } catch (err: any) {
    if (err?.code === 'auth/email-already-in-use' && enteredMassar.length >= 4) {
      const cred = await signInWithEmailAndPassword(auth, authEmail, params.password);
      user = cred.user;
    } else {
      throw err;
    }
  }

  // Verify class_codes after authentication (firestore.rules requires signedIn)
  const codeSnap = await getDoc(doc(db, 'class_codes', parsed.classToken));
  if (!codeSnap.exists()) {
    throw new Error('Code QR de classe introuvable.');
  }
  const codeData = codeSnap.data();

  const deviceId = getOrCreateWebDeviceId();
  const deviceLabel = getWebDeviceLabel();
  const cleanRecovery =
    (params.recoveryEmail || '').trim().toLowerCase() ||
    defaultStudentTaalimEmail(effectiveMassar);

  const userRef = doc(db, 'users', user.uid);
  const existingUserSnap = await getDoc(userRef);

  if (!existingUserSnap.exists()) {
    await setDoc(userRef, {
      userId: user.uid,
      fullName: params.fullName.trim(),
      phoneNumber: (params.phoneNumber || '').trim(),
      role: 'student',
      massarNumber: effectiveMassar,
      birthDate: '',
      email: cleanRecovery,
      establishment: '',
      teacherStatus: 'approved',
      mustChangePassword: false,
      acceptedTerms: false,
      avatarUrl: null,
      xp: 0,
      loginStreak: 1,
      lastLoginDate: todayIsoDate(),
      unlockedBadgeIds: [],
      boundDeviceId: deviceId,
      boundDeviceModel: deviceLabel,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } else {
    const existingData = existingUserSnap.data();
    const updates: Record<string, any> = {
      updatedAt: serverTimestamp(),
    };
    if (!existingData.boundDeviceId) {
      updates.boundDeviceId = deviceId;
      updates.boundDeviceModel = deviceLabel;
    }
    if (!existingData.email && cleanRecovery) {
      updates.email = cleanRecovery;
    }
    await updateDoc(userRef, updates);
  }

  // Create or confirm enrollment automatically approved via qrToken
  const enrRef = doc(db, 'enrollments', user.uid);
  const existingEnr = await getDoc(enrRef);
  if (!existingEnr.exists()) {
    await setDoc(enrRef, {
      studentId: user.uid,
      classId: codeData.classId,
      className: codeData.className,
      teacherId: codeData.teacherId,
      teacherName: codeData.teacherName,
      studentName: params.fullName.trim(),
      studentPhone: (params.phoneNumber || '').trim(),
      massarNumber: effectiveMassar,
      qrToken: parsed.classToken,
      status: 'approved',
      requestedAt: serverTimestamp(),
      validatedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}

export async function joinClassWithQrForSignedInStudent(params: {
  profile: UserProfile;
  rawQr: string;
}): Promise<Enrollment> {
  const { db } = requireFirebase();
  const lookup = await lookupClassByQrToken(params.rawQr);
  if (!lookup) {
    throw new Error('Code QR de classe introuvable.');
  }
  const { codeInfo } = lookup;
  const enrRef = doc(db, 'enrollments', params.profile.userId);
  await setDoc(enrRef, {
    studentId: params.profile.userId,
    classId: codeInfo.classId,
    className: codeInfo.className,
    teacherId: codeInfo.teacherId,
    teacherName: codeInfo.teacherName,
    studentName: params.profile.fullName,
    studentPhone: params.profile.phoneNumber,
    massarNumber: params.profile.massarNumber,
    qrToken: codeInfo.qrToken,
    status: 'approved',
    requestedAt: serverTimestamp(),
    validatedAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  return {
    id: params.profile.userId,
    studentId: params.profile.userId,
    classId: codeInfo.classId,
    className: codeInfo.className,
    teacherId: codeInfo.teacherId,
    teacherName: codeInfo.teacherName,
    studentName: params.profile.fullName,
    studentPhone: params.profile.phoneNumber,
    massarNumber: params.profile.massarNumber,
    qrToken: codeInfo.qrToken,
    status: 'approved',
    requestedAtMillis: Date.now(),
    validatedAtMillis: Date.now(),
  };
}

export async function bindCurrentWebDeviceIfEmpty(profile: UserProfile): Promise<void> {
  const { db } = requireFirebase();
  if (profile.role !== 'student' || profile.boundDeviceId) return;
  const deviceId = getOrCreateWebDeviceId();
  await updateDoc(doc(db, 'users', profile.userId), {
    boundDeviceId: deviceId,
    boundDeviceModel: getWebDeviceLabel(),
    updatedAt: serverTimestamp(),
  });
}

export async function acceptUserTerms(uid: string): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'users', uid), {
    acceptedTerms: true,
    updatedAt: serverTimestamp(),
  });
}

export async function updateStudentProfileData(params: {
  profile: UserProfile;
  isQrLocked: boolean;
  fullName: string;
  phoneNumber: string;
  massarNumber: string;
  birthDate: string;
  email: string;
  newPassword?: string;
}): Promise<void> {
  const { auth, db } = requireFirebase();
  const updates: Record<string, any> = {
    phoneNumber: params.phoneNumber.trim(),
    birthDate: params.birthDate.trim(),
    email: params.email.trim().toLowerCase(),
    updatedAt: serverTimestamp(),
  };

  if (!params.isQrLocked) {
    updates.fullName = params.fullName.trim();
    updates.massarNumber = normalizeMassar(params.massarNumber);
  }

  await updateDoc(doc(db, 'users', params.profile.userId), updates);

  if (params.newPassword && params.newPassword.trim().length >= 6 && auth.currentUser) {
    await updatePassword(auth.currentUser, params.newPassword.trim());
    await updateDoc(doc(db, 'users', params.profile.userId), {
      mustChangePassword: false,
      updatedAt: serverTimestamp(),
    });
  }
}

export async function updateAvatarDataUrl(uid: string, dataUrl: string): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'users', uid), {
    avatarUrl: dataUrl,
    updatedAt: serverTimestamp(),
  });
}

export async function sendPasswordReset(emailOrMassar: string): Promise<void> {
  const { auth } = requireFirebase();
  const trimmed = emailOrMassar.trim();
  const targetEmail = trimmed.includes('@') ? trimmed.toLowerCase() : massarToAuthEmail(trimmed);
  await sendPasswordResetEmail(auth, targetEmail);
}

export async function signOutAccount(): Promise<void> {
  if (!auth) return;
  await signOut(auth);
}

// ---------- REALTIME LISTENERS ----------

export function subscribeUserProfile(
  uid: string,
  onData: (profile: UserProfile | null) => void,
  onError?: (e: Error) => void
): Unsubscribe {
  const { db } = requireFirebase();
  return onSnapshot(
    doc(db, 'users', uid),
    (snap) => {
      onData(snap.exists() ? mapUserProfile(snap.id, snap.data()) : null);
    },
    (err) => onError?.(err)
  );
}

export function subscribeStudentEnrollment(
  studentId: string,
  onData: (enr: Enrollment | null) => void
): Unsubscribe {
  const { db } = requireFirebase();
  return onSnapshot(doc(db, 'enrollments', studentId), (snap) => {
    onData(snap.exists() ? mapEnrollment(snap.id, snap.data()) : null);
  });
}

export function subscribeStudentClass(
  classId: string,
  onData: (cls: SvtClass | null) => void
): Unsubscribe {
  const { db } = requireFirebase();
  return onSnapshot(doc(db, 'classes', classId), (snap) => {
    onData(snap.exists() ? mapClass(snap.id, snap.data()) : null);
  });
}

export function subscribeTeacherClasses(
  teacherId: string,
  onData: (classes: SvtClass[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q = query(collection(db, 'classes'), where('teacherId', '==', teacherId));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapClass(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

export function subscribeTeacherEnrollments(
  teacherId: string,
  onData: (items: Enrollment[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q = query(collection(db, 'enrollments'), where('teacherId', '==', teacherId));
  return onSnapshot(q, (snap) => {
    onData(snap.docs.map((d) => mapEnrollment(d.id, d.data())));
  });
}

export function subscribeTeacherRoster(
  teacherId: string,
  onData: (items: RosterStudent[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q = query(collection(db, 'class_rosters'), where('teacherId', '==', teacherId));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapRoster(d.id, d.data()));
    list.sort((a, b) => a.fullName.localeCompare(b.fullName));
    onData(list);
  });
}

export function subscribeClassQuizzes(
  params: { role: UserRole; userId: string; classId?: string },
  onData: (quizzes: Quiz[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q =
    params.role === 'teacher'
      ? query(collection(db, 'quizzes'), where('teacherId', '==', params.userId))
      : query(collection(db, 'quizzes'), where('classId', '==', params.classId || ''));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapQuiz(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

export function subscribeQuizAttempts(
  params: { role: UserRole; userId: string },
  onData: (attempts: QuizAttempt[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const field = params.role === 'teacher' ? 'teacherId' : 'studentId';
  const q = query(collection(db, 'quiz_attempts'), where(field, '==', params.userId));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapAttempt(d.id, d.data()));
    list.sort((a, b) => b.submittedAtMillis - a.submittedAtMillis);
    onData(list);
  });
}

export function subscribeResources(
  params: { role: UserRole; userId: string; classId?: string },
  onData: (items: CourseResource[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q =
    params.role === 'teacher'
      ? query(collection(db, 'resources'), where('teacherId', '==', params.userId))
      : query(collection(db, 'resources'), where('classId', '==', params.classId || ''));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapResource(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

export function subscribeVideoTracking(
  params: { role: UserRole; userId: string },
  onData: (items: VideoTracking[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const field = params.role === 'teacher' ? 'teacherId' : 'studentId';
  const q = query(collection(db, 'video_tracking'), where(field, '==', params.userId));
  return onSnapshot(q, (snap) => {
    onData(snap.docs.map((d) => mapVideoTracking(d.id, d.data())));
  });
}

export function subscribeAnnouncements(
  params: { role: UserRole; userId: string; classId?: string },
  onData: (items: Announcement[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q =
    params.role === 'teacher'
      ? query(collection(db, 'announcements'), where('teacherId', '==', params.userId))
      : query(collection(db, 'announcements'), where('classId', '==', params.classId || ''));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapAnnouncement(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

export function subscribePrivateMessages(
  params: { role: UserRole; userId: string },
  onData: (items: PrivateMessage[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const field = params.role === 'teacher' ? 'teacherId' : 'studentId';
  const q = query(collection(db, 'private_messages'), where(field, '==', params.userId));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapPrivateMessage(d.id, d.data()));
    list.sort((a, b) => a.createdAtMillis - b.createdAtMillis);
    onData(list);
  });
}

export function subscribeStudentNotifications(
  studentId: string,
  onData: (items: StudentNotification[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q = query(collection(db, 'student_notifications'), where('studentId', '==', studentId));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapNotification(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

export function subscribeAdminTeacherRequests(
  onData: (teachers: UserProfile[]) => void
): Unsubscribe {
  const { db } = requireFirebase();
  const q = query(collection(db, 'users'), where('role', '==', 'teacher'));
  return onSnapshot(q, (snap) => {
    const list = snap.docs.map((d) => mapUserProfile(d.id, d.data()));
    list.sort((a, b) => b.createdAtMillis - a.createdAtMillis);
    onData(list);
  });
}

// ---------- STUDENT MUTATIONS ----------

export async function submitQuizAttempt(params: {
  quiz: Quiz;
  profile: UserProfile;
  selectedAnswers: number[];
  perQuestionSeconds: number[];
  totalSeconds: number;
  previousAttemptsCount: number;
  completedVideosCount: number;
  openedResourcesCount: number;
  sentMessagesCount: number;
}): Promise<{ attempt: QuizAttempt; newBadges: string[] }> {
  const { db } = requireFirebase();
  const answers: QuestionAnswer[] = params.quiz.questions.map((q, idx) => {
    const sel = params.selectedAnswers[idx] ?? -1;
    return {
      questionIndex: idx,
      selectedIndex: sel,
      isCorrect: sel === q.correctIndex,
      timeSpentSeconds: Math.max(0, Math.round(params.perQuestionSeconds[idx] ?? 0)),
    };
  });
  const score = answers.filter((a) => a.isCorrect).length;
  const maxScore = Math.max(1, params.quiz.questions.length);
  const attemptId = `${params.profile.userId}_${params.quiz.id}`;
  const clampedTotal = Math.max(0, Math.min(86400, Math.round(params.totalSeconds)));

  await setDoc(doc(db, 'quiz_attempts', attemptId), {
    quizId: params.quiz.id,
    quizTitle: params.quiz.title,
    classId: params.quiz.classId,
    teacherId: params.quiz.teacherId,
    studentId: params.profile.userId,
    studentName: params.profile.fullName,
    score,
    maxScore,
    totalDurationSeconds: clampedTotal,
    answers,
    completedAt: serverTimestamp(),
    createdAt: serverTimestamp(),
  });

  const earnedXp = 25 + score * 10;
  const { newBadgeIds, bonusXp } = evaluateBadgeUnlocks({
    existingBadges: params.profile.unlockedBadgeIds,
    attemptsCount: params.previousAttemptsCount + 1,
    latestScore: score,
    latestMaxScore: maxScore,
    latestTotalSeconds: clampedTotal,
    latestQuestionsCount: params.quiz.questions.length,
    completedVideosCount: params.completedVideosCount,
    openedResourcesCount: params.openedResourcesCount,
    sentMessagesCount: params.sentMessagesCount,
    loginStreak: params.profile.loginStreak,
  });

  const mergedBadges = Array.from(new Set([...params.profile.unlockedBadgeIds, ...newBadgeIds])).slice(
    0,
    30
  );
  await updateDoc(doc(db, 'users', params.profile.userId), {
    xp: Math.min(1_000_000, params.profile.xp + earnedXp + bonusXp),
    unlockedBadgeIds: mergedBadges,
    updatedAt: serverTimestamp(),
  });

  for (const badgeId of newBadgeIds) {
    const nRef = doc(collection(db, 'student_notifications'));
    await setDoc(nRef, {
      studentId: params.profile.userId,
      teacherId: params.quiz.teacherId,
      type: 'badge',
      title: 'Nouveau badge SVT débloqué !',
      body: `Félicitations ! Vous avez débloqué le badge « ${badgeId} ».`,
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }

  return {
    attempt: {
      id: attemptId,
      quizId: params.quiz.id,
      classId: params.quiz.classId,
      studentId: params.profile.userId,
      studentName: params.profile.fullName,
      score,
      maxScore,
      totalDurationSeconds: clampedTotal,
      answers,
      submittedAtMillis: Date.now(),
    },
    newBadges: newBadgeIds,
  };
}

export async function updateVideoTrackingProgress(params: {
  resource: CourseResource;
  profile: UserProfile;
  currentSeconds: number;
  durationSeconds: number;
  existing?: VideoTracking;
}): Promise<void> {
  const { db } = requireFirebase();
  const safeDuration = Math.max(
    1,
    Math.round(params.durationSeconds || params.resource.durationSeconds || 1)
  );
  const pos = Math.max(0, Math.round(params.currentSeconds));
  const bestPos = Math.max(pos, params.existing?.watchedSeconds || 0);
  const pct = Math.min(100, Math.round((bestPos * 100) / safeDuration));
  const finalPct = Math.max(pct, params.existing?.completionPercentage || 0);

  let status: VideoStatus = 'not_started';
  if (finalPct >= 90 || params.existing?.status === 'completed') {
    status = 'completed';
  } else if (finalPct > 0) {
    status = 'partial';
  }

  const trackingId = `${params.profile.userId}_${params.resource.id}`;
  const ref = doc(db, 'video_tracking', trackingId);

  if (!params.existing) {
    await setDoc(ref, {
      resourceId: params.resource.id,
      classId: params.resource.classId,
      teacherId: params.resource.teacherId,
      studentId: params.profile.userId,
      studentName: params.profile.fullName,
      maxPositionSeconds: Math.min(86400, bestPos),
      durationSeconds: Math.min(86400, safeDuration),
      watchedPercentage: status === 'completed' ? Math.max(90, finalPct) : finalPct,
      status,
      lastWatchedAt: serverTimestamp(),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } else {
    await updateDoc(ref, {
      studentName: params.profile.fullName,
      maxPositionSeconds: Math.min(86400, bestPos),
      durationSeconds: Math.min(86400, safeDuration),
      watchedPercentage: status === 'completed' ? Math.max(90, finalPct) : finalPct,
      status,
      lastWatchedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}

export async function markResourceOpened(
  resource: CourseResource,
  profile: UserProfile,
  existing?: VideoTracking
): Promise<void> {
  const { db } = requireFirebase();
  const trackingId = `${profile.userId}_${resource.id}`;
  const ref = doc(db, 'video_tracking', trackingId);
  if (!existing) {
    await setDoc(ref, {
      resourceId: resource.id,
      classId: resource.classId,
      teacherId: resource.teacherId,
      studentId: profile.userId,
      studentName: profile.fullName,
      maxPositionSeconds: 1,
      durationSeconds: 1,
      watchedPercentage: 100,
      status: 'completed',
      lastWatchedAt: serverTimestamp(),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } else if (existing.status !== 'completed') {
    await updateDoc(ref, {
      studentName: profile.fullName,
      maxPositionSeconds: 1,
      durationSeconds: 1,
      watchedPercentage: 100,
      status: 'completed',
      lastWatchedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}

export async function sendPrivateMessage(params: {
  classId: string;
  teacherId: string;
  studentId: string;
  studentName: string;
  senderId: string;
  senderRole: UserRole;
  text: string;
}): Promise<void> {
  const { db } = requireFirebase();
  const ref = doc(collection(db, 'private_messages'));
  await setDoc(ref, {
    classId: params.classId,
    teacherId: params.teacherId,
    studentId: params.studentId,
    studentName: params.studentName,
    senderId: params.senderId,
    senderRole: params.senderRole,
    content: params.text.trim().slice(0, 2000),
    isRead: false,
    createdAt: serverTimestamp(),
  });

  if (params.senderRole === 'teacher') {
    const notifRef = doc(collection(db, 'student_notifications'));
    await setDoc(notifRef, {
      studentId: params.studentId,
      teacherId: params.teacherId,
      type: 'message',
      title: 'Nouveau message du professeur',
      body: params.text.trim().slice(0, 180),
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }
}

export async function markMessagesRead(messageIds: string[]): Promise<void> {
  const { db } = requireFirebase();
  if (messageIds.length === 0) return;
  const batch = writeBatch(db);
  for (const id of messageIds.slice(0, 400)) {
    batch.update(doc(db, 'private_messages', id), { isRead: true });
  }
  await batch.commit();
}

export async function markAllNotificationsRead(notifications: StudentNotification[]): Promise<void> {
  const { db } = requireFirebase();
  const unread = notifications.filter((n) => !n.isRead);
  if (unread.length === 0) return;
  const batch = writeBatch(db);
  for (const n of unread.slice(0, 400)) {
    batch.update(doc(db, 'student_notifications', n.id), { isRead: true });
  }
  await batch.commit();
}

// ---------- CHUNKED RESOURCE DOWNLOAD / RECONSTRUCTION ----------

export async function resolveResourceBlobUrl(resource: CourseResource): Promise<{
  url: string;
  mimeType: string;
  isObjectUrl: boolean;
  buffer?: ArrayBuffer;
}> {
  const chunkMeta = parseChunkResourceUrl(resource.fileUrl);
  if (!chunkMeta) {
    return {
      url: resource.fileUrl,
      mimeType: getMimeTypeForResource(resource.type, ''),
      isObjectUrl: false,
    };
  }
  const { db } = requireFirebase();
  const q = query(
    collection(db, 'resource_chunks'),
    where('resourceId', '==', chunkMeta.resourceId)
  );
  const snap = await getDocs(q);
  if (snap.empty) {
    throw new Error('Fichier introuvable ou supprimé par le professeur.');
  }

  const chunks = snap.docs
    .map((d) => ({
      index: d.data().chunkIndex as number,
      data: d.data().data,
    }))
    .sort((a, b) => a.index - b.index);

  const arrays: Uint8Array[] = chunks.map((c) => {
    if (c.data instanceof Bytes) return c.data.toUint8Array();
    if (c.data && typeof c.data.toUint8Array === 'function') return c.data.toUint8Array();
    if (c.data instanceof Uint8Array) return c.data;
    throw new Error('Format de chunk invalide');
  });

  const totalLen = arrays.reduce((acc, a) => acc + a.byteLength, 0);
  const merged = new Uint8Array(totalLen);
  let offset = 0;
  for (const part of arrays) {
    merged.set(part, offset);
    offset += part.byteLength;
  }

  const mimeType = getMimeTypeForResource(resource.type, chunkMeta.ext);
  const blob = new Blob([merged], { type: mimeType });
  const objectUrl = URL.createObjectURL(blob);
  return {
    url: objectUrl,
    mimeType,
    isObjectUrl: true,
    buffer: merged.buffer,
  };
}

// ---------- TEACHER MUTATIONS ----------

export async function createSvtClass(params: {
  teacher: UserProfile;
  name: string;
  level: string;
  academicYear: string;
}): Promise<SvtClass> {
  const { db } = requireFirebase();
  const classRef = doc(collection(db, 'classes'));
  const qrToken = generateQrToken(params.name);

  const batch = writeBatch(db);
  batch.set(classRef, {
    teacherId: params.teacher.userId,
    teacherName: params.teacher.fullName,
    name: params.name.trim(),
    academicYear: params.academicYear.trim(),
    level: params.level.trim(),
    qrToken,
    visibleLabDomains: ALL_SVT_DOMAINS,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  batch.set(doc(db, 'class_codes', qrToken), {
    classId: classRef.id,
    className: params.name.trim(),
    academicYear: params.academicYear.trim(),
    teacherId: params.teacher.userId,
    teacherName: params.teacher.fullName,
    createdAt: serverTimestamp(),
  });
  await batch.commit();

  return {
    id: classRef.id,
    teacherId: params.teacher.userId,
    teacherName: params.teacher.fullName,
    name: params.name.trim(),
    academicYear: params.academicYear.trim(),
    level: params.level.trim(),
    qrToken,
    visibleLabDomains: ALL_SVT_DOMAINS,
    createdAtMillis: Date.now(),
  };
}

export async function updateClassLabDomains(
  classId: string,
  domains: SvtDomain[]
): Promise<void> {
  const { db } = requireFirebase();
  const safe = domains.length > 0 ? domains : ALL_SVT_DOMAINS;
  await updateDoc(doc(db, 'classes', classId), {
    visibleLabDomains: safe,
    updatedAt: serverTimestamp(),
  });
}

export async function approveOrRejectEnrollment(
  studentId: string,
  status: 'approved' | 'rejected',
  teacherId: string,
  className: string
): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'enrollments', studentId), {
    status,
    validatedAt: status === 'approved' ? serverTimestamp() : null,
    updatedAt: serverTimestamp(),
  });

  if (status === 'approved') {
    const nRef = doc(collection(db, 'student_notifications'));
    await setDoc(nRef, {
      studentId,
      teacherId,
      type: 'enrollment',
      title: 'Inscription validée ✓',
      body: `Bienvenue dans la classe « ${className} » !`,
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }
}

export async function approveAllPendingEnrollments(
  pending: Enrollment[],
  teacherId: string
): Promise<void> {
  const { db } = requireFirebase();
  if (pending.length === 0) return;
  const batch = writeBatch(db);
  for (const enr of pending.slice(0, 300)) {
    batch.update(doc(db, 'enrollments', enr.studentId), {
      status: 'approved',
      validatedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
  await batch.commit();
  for (const enr of pending) {
    await setDoc(doc(collection(db, 'student_notifications')), {
      studentId: enr.studentId,
      teacherId,
      type: 'enrollment',
      title: 'Inscription validée ✓',
      body: `Bienvenue dans la classe « ${enr.className} » !`,
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }
}

export async function unlinkStudentDevice(studentId: string): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'users', studentId), {
    boundDeviceId: '',
    boundDeviceModel: '',
    updatedAt: serverTimestamp(),
  });
}

export async function removeEnrollmentAndData(enrollment: Enrollment): Promise<void> {
  const { db } = requireFirebase();
  await deleteDoc(doc(db, 'enrollments', enrollment.studentId));
}

export async function provisionRosterEntries(params: {
  svtClass: SvtClass;
  entries: ParsedRosterEntry[];
}): Promise<number> {
  const { db } = requireFirebase();
  const provAuth = getProvisionerAuth();
  let createdCount = 0;

  for (const entry of params.entries) {
    const cleanMassar = normalizeMassar(entry.massarNumber);
    if (cleanMassar.length < 4) continue;
    const provisionalPassword = generateProvisionalPassword(cleanMassar);
    const qrPayload = buildStudentQrPayload(params.svtClass.qrToken, cleanMassar);
    let studentUid = '';

    if (provAuth) {
      const authEmail = massarToAuthEmail(cleanMassar);
      try {
        const cred = await createUserWithEmailAndPassword(provAuth, authEmail, provisionalPassword);
        studentUid = cred.user.uid;
        await signOut(provAuth);
      } catch {
        // Already exists in Auth; student can log in or join via QR
      }
    }

    const rosterId = `${params.svtClass.id}_${cleanMassar}`;
    await setDoc(doc(db, 'class_rosters', rosterId), {
      classId: params.svtClass.id,
      className: params.svtClass.name,
      teacherId: params.svtClass.teacherId,
      teacherName: params.svtClass.teacherName,
      qrToken: params.svtClass.qrToken,
      fullName: entry.fullName.trim() || cleanMassar,
      massarNumber: cleanMassar,
      provisionalPassword,
      studentUid,
      qrPayload,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    createdCount += 1;
  }

  return createdCount;
}

export async function deleteRosterStudent(rosterId: string): Promise<void> {
  const { db } = requireFirebase();
  await deleteDoc(doc(db, 'class_rosters', rosterId));
}

export async function publishQuizToClasses(params: {
  teacherId: string;
  classIds: string[];
  enrollments: Enrollment[];
  title: string;
  description: string;
  chapterSvt: string;
  questions: QuizQuestion[];
  isDownloadable: boolean;
}): Promise<void> {
  const { db } = requireFirebase();
  const cleanQuestions = params.questions.slice(0, 20).map((q) => {
    const base: Record<string, any> = {
      questionText: q.questionText.trim(),
      options: q.options.map((o) => o.trim()).filter(Boolean),
      correctIndex: q.correctIndex,
      explanation: q.explanation.trim(),
    };
    if (q.imageUrl && q.imageUrl.trim()) {
      base.imageUrl = q.imageUrl.trim();
    }
    return base;
  });

  for (const classId of params.classIds) {
    const approvedIds = params.enrollments
      .filter((e) => e.classId === classId && e.status === 'approved')
      .map((e) => e.studentId)
      .slice(0, 100);

    const quizRef = doc(collection(db, 'quizzes'));
    await setDoc(quizRef, {
      classId,
      teacherId: params.teacherId,
      title: params.title.trim(),
      description: params.description.trim(),
      chapterSvt: params.chapterSvt.trim() || 'SVT',
      questions: cleanQuestions,
      approvedStudentIds: approvedIds,
      isPublished: true,
      isDownloadable: params.isDownloadable,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });

    for (const sid of approvedIds) {
      await setDoc(doc(collection(db, 'student_notifications')), {
        studentId: sid,
        teacherId: params.teacherId,
        type: 'quiz',
        title: 'Nouveau QCM SVT',
        body: params.title.trim(),
        isRead: false,
        createdAt: serverTimestamp(),
      });
    }
  }
}

export async function toggleQuizDownloadable(quiz: Quiz): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'quizzes', quiz.id), {
    isDownloadable: !quiz.isDownloadable,
    updatedAt: serverTimestamp(),
  });
}

export async function deleteQuiz(quizId: string): Promise<void> {
  const { db } = requireFirebase();
  await deleteDoc(doc(db, 'quizzes', quizId));
}

export async function publishResource(params: {
  teacherId: string;
  classId: string;
  enrollments: Enrollment[];
  title: string;
  description: string;
  type: ResourceType;
  externalUrl?: string;
  file?: File | null;
  durationSeconds?: number;
  isDownloadable: boolean;
  onProgress?: (pct: number) => void;
}): Promise<void> {
  const { db } = requireFirebase();
  const resRef = doc(collection(db, 'resources'));
  let finalUrl = (params.externalUrl || '').trim();

  if (params.file) {
    if (params.file.size > MAX_UPLOAD_BYTES) {
      throw new Error('Le fichier dépasse la limite de 20 Mo. Utilisez un lien HTTPS ou compressez-le.');
    }
    const buf = await params.file.arrayBuffer();
    const bytes = new Uint8Array(buf);
    if (bytes.byteLength === 0) {
      throw new Error('Le fichier sélectionné est vide.');
    }
    const totalChunks = Math.ceil(bytes.byteLength / CHUNK_SIZE_BYTES);
    if (totalChunks > 200) {
      throw new Error('Fichier trop volumineux pour le découpage Firestore.');
    }
    const ext = (params.file.name.split('.').pop() || 'bin').toLowerCase();

    for (let i = 0; i < totalChunks; i++) {
      const start = i * CHUNK_SIZE_BYTES;
      const end = Math.min(bytes.byteLength, start + CHUNK_SIZE_BYTES);
      const slice = bytes.slice(start, end);
      const chunkId = `${resRef.id}_${i}`;
      await setDoc(doc(db, 'resource_chunks', chunkId), {
        resourceId: resRef.id,
        classId: params.classId,
        teacherId: params.teacherId,
        chunkIndex: i,
        totalChunks,
        data: Bytes.fromUint8Array(slice),
        createdAt: serverTimestamp(),
      });
      params.onProgress?.(Math.round(((i + 1) * 100) / totalChunks));
    }

    finalUrl = `${CHUNK_URL_PREFIX}${resRef.id}?ext=${encodeURIComponent(ext)}`;
  }

  if (!finalUrl) {
    throw new Error('Veuillez fournir un lien URL ou choisir un fichier.');
  }

  const approvedIds = params.enrollments
    .filter((e) => e.classId === params.classId && e.status === 'approved')
    .map((e) => e.studentId)
    .slice(0, 100);

  await setDoc(resRef, {
    classId: params.classId,
    teacherId: params.teacherId,
    title: params.title.trim(),
    description: params.description.trim(),
    type: params.type.toLowerCase(),
    fileUrl: finalUrl,
    durationSeconds: Math.max(0, Math.min(86400, Math.round(params.durationSeconds || 0))),
    approvedStudentIds: approvedIds,
    isDownloadable: params.isDownloadable,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  for (const sid of approvedIds) {
    await setDoc(doc(collection(db, 'student_notifications')), {
      studentId: sid,
      teacherId: params.teacherId,
      type: 'resource',
      title: 'Nouveau support de cours SVT',
      body: params.title.trim(),
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }
}

export async function deleteResource(resourceId: string): Promise<void> {
  const { db } = requireFirebase();
  await deleteDoc(doc(db, 'resources', resourceId));
  // Cleanup chunks if any
  try {
    const q = query(collection(db, 'resource_chunks'), where('resourceId', '==', resourceId));
    const snap = await getDocs(q);
    for (const d of snap.docs) {
      await deleteDoc(d.ref);
    }
  } catch {}
}

export async function publishAnnouncement(params: {
  teacherId: string;
  classId: string;
  enrollments: Enrollment[];
  title: string;
  content: string;
}): Promise<void> {
  const { db } = requireFirebase();
  const approvedIds = params.enrollments
    .filter((e) => e.classId === params.classId && e.status === 'approved')
    .map((e) => e.studentId)
    .slice(0, 100);

  await setDoc(doc(collection(db, 'announcements')), {
    classId: params.classId,
    teacherId: params.teacherId,
    title: params.title.trim(),
    content: params.content.trim(),
    approvedStudentIds: approvedIds,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  for (const sid of approvedIds) {
    await setDoc(doc(collection(db, 'student_notifications')), {
      studentId: sid,
      teacherId: params.teacherId,
      type: 'announcement',
      title: `Annonce : ${params.title.trim()}`,
      body: params.content.trim().slice(0, 200),
      isRead: false,
      createdAt: serverTimestamp(),
    });
  }
}

export async function deleteAnnouncement(announcementId: string): Promise<void> {
  const { db } = requireFirebase();
  await deleteDoc(doc(db, 'announcements', announcementId));
}

export async function adminSetTeacherStatus(
  teacherUid: string,
  status: 'approved' | 'rejected'
): Promise<void> {
  const { db } = requireFirebase();
  await updateDoc(doc(db, 'users', teacherUid), {
    teacherStatus: status,
    updatedAt: serverTimestamp(),
  });
}
