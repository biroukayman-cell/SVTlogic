import { initializeApp, getApps, FirebaseApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, Auth } from 'firebase/auth';
import { getFirestore, Firestore } from 'firebase/firestore';

export const NAMED_FIRESTORE_DATABASE_ID =
  'ai-studio-android-bioterre-eb3decdd-52a7-4d8e-8dc6-bad24a12b2c5';

export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain:
    import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'gen-lang-client-0300039938.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'gen-lang-client-0300039938',
  storageBucket:
    import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'gen-lang-client-0300039938.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '339782413729',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '',
};

export const isFirebaseConfigured = Boolean(
  firebaseConfig.apiKey.trim() && firebaseConfig.appId.trim()
);

let appInstance: FirebaseApp | null = null;
let authInstance: Auth | null = null;
let dbInstance: Firestore | null = null;
let provisionerAuthInstance: Auth | null = null;

if (isFirebaseConfigured) {
  appInstance = getApps().find((a) => a.name === '[DEFAULT]') || initializeApp(firebaseConfig);
  authInstance = getAuth(appInstance);
  dbInstance = getFirestore(appInstance, NAMED_FIRESTORE_DATABASE_ID);
}

export const firebaseApp = appInstance;
export const auth = authInstance;
export const db = dbInstance;
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export function getProvisionerAuth(): Auth | null {
  if (!isFirebaseConfigured) return null;
  if (provisionerAuthInstance) return provisionerAuthInstance;
  const provName = 'bioterre_student_provisioner';
  const existing = getApps().find((a) => a.name === provName);
  const provApp = existing || initializeApp(firebaseConfig, provName);
  provisionerAuthInstance = getAuth(provApp);
  return provisionerAuthInstance;
}
