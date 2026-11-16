const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const TEACHER_UID = "teacher_uid_1";
const STUDENT_A_UID = "student_a_1";
const STUDENT_B_UID = "student_b_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const serverNow = () => new Date();

test("Unauthenticated caller cannot read users, classes, quizzes, or private_messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(unauthDb.collection("classes").get());
  await assertFails(unauthDb.collection("quizzes").get());
  await assertFails(unauthDb.collection("private_messages").get());
});

test("User profile: owner can create/read, cross-user read & role escalation fail", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      fullName: "Amina El Amrani",
      phoneNumber: "+212600112233",
      role: "student",
      acceptedTerms: false,
      xp: 0,
      loginStreak: 1,
      lastLoginDate: "2026-11-12",
      unlockedBadgeIds: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student can accept terms of use on own profile
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A_UID).update({
      acceptedTerms: true,
      updatedAt: now,
    })
  );

  // Cross-user terms update fails
  await assertFails(
    bobDb.collection("users").doc(STUDENT_A_UID).update({
      acceptedTerms: false,
      updatedAt: now,
    })
  );

  // Cross-user PII read fails
  await assertFails(bobDb.collection("users").doc(STUDENT_A_UID).get());

  // Role escalation fails
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_A_UID).update({
      role: "teacher",
      updatedAt: now,
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_A_UID).update({
      isVerified: true,
      updatedAt: now,
    })
  );
});

test("Enrollment workflow: student cannot self-approve, teacher approves, student-to-student hidden", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  // Teacher creates class and class_code
  await assertSucceeds(
    teacherDb.collection("classes").doc("class_1").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "2eme Bac SVT - Groupe A",
      academicYear: "2026-2027",
      level: "2eme Bac",
      qrToken: "SVT_TOKEN_1234",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    teacherDb.collection("class_codes").doc("SVT_TOKEN_1234").set({
      classId: "class_1",
      className: "2eme Bac SVT - Groupe A",
      academicYear: "2026-2027",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      createdAt: now,
    })
  );

  // List scraping on class_codes fails
  await assertFails(studentADb.collection("class_codes").get());
  // Direct get with exact QR token succeeds
  await assertSucceeds(studentADb.collection("class_codes").doc("SVT_TOKEN_1234").get());

  // Student A cannot create an already-approved enrollment
  await assertFails(
    studentADb.collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_1",
      className: "2eme Bac SVT - Groupe A",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "+212600112233",
      status: "approved",
      requestedAt: now,
      updatedAt: now,
    })
  );

  // Student A creates pending enrollment
  await assertSucceeds(
    studentADb.collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_1",
      className: "2eme Bac SVT - Groupe A",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "+212600112233",
      status: "pending",
      requestedAt: now,
      updatedAt: now,
    })
  );

  // Student A cannot self-approve via update
  await assertFails(
    studentADb.collection("enrollments").doc(STUDENT_A_UID).update({
      status: "approved",
      updatedAt: now,
    })
  );

  // Student B cannot read Student A's enrollment
  await assertFails(studentBDb.collection("enrollments").doc(STUDENT_A_UID).get());

  // Teacher approves Student A
  await assertSucceeds(
    teacherDb.collection("enrollments").doc(STUDENT_A_UID).update({
      status: "approved",
      validatedAt: now,
      updatedAt: now,
    })
  );
});

test("Private messaging: strictly student <-> teacher, zero student-to-student access", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_1",
      className: "2eme Bac SVT",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "0600000000",
      status: "approved",
      requestedAt: now,
      updatedAt: now,
    });
  });

  // Student A sends message to teacher
  await assertSucceeds(
    studentADb.collection("private_messages").doc("msg_1").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Amina",
      senderId: STUDENT_A_UID,
      senderRole: "student",
      content: "Bonjour professeur, question sur la méiose.",
      isRead: false,
      createdAt: now,
    })
  );

  // Student B cannot read Student A's message or query Student A's messages
  await assertFails(studentBDb.collection("private_messages").doc("msg_1").get());
  await assertFails(
    studentBDb.collection("private_messages").where("studentId", "==", STUDENT_A_UID).get()
  );

  // Student B cannot impersonate Student A
  await assertFails(
    studentBDb.collection("private_messages").doc("msg_spoof").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Amina",
      senderId: STUDENT_B_UID,
      senderRole: "student",
      content: "Hello",
      isRead: false,
      createdAt: now,
    })
  );

  // Teacher can query and reply
  await assertSucceeds(
    teacherDb.collection("private_messages").where("teacherId", "==", TEACHER_UID).get()
  );
  await assertSucceeds(
    teacherDb.collection("private_messages").doc("msg_2").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Amina",
      senderId: TEACHER_UID,
      senderRole: "teacher",
      content: "Voici l'explication sur la prophase I.",
      isRead: false,
      createdAt: now,
    })
  );
});

test("Quizzes, attempts, resources, video tracking, and notifications enforce isolation", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("classes").doc("class_1").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "SVT 1",
      academicYear: "2026-2027",
      level: "2eme Bac",
      qrToken: "SVT_TOK",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("quizzes").doc("quiz_1").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      title: "Génétique",
      description: "QCM Mitose",
      chapterSvt: "Division cellulaire",
      questions: [{ questionText: "Q1", options: ["A", "B"], correctIndex: 0, explanation: "E1" }],
      approvedStudentIds: [STUDENT_A_UID],
      isPublished: true,
      isDownloadable: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("resources").doc("res_1").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      title: "Vidéo Mitose",
      description: "Cours vidéo",
      type: "video",
      fileUrl: "https://example.com/mitose.mp4",
      durationSeconds: 120,
      approvedStudentIds: [STUDENT_A_UID],
      isDownloadable: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Teacher can mark resource as downloadable; student cannot
  await assertSucceeds(
    teacherDb.collection("resources").doc("res_1").update({
      isDownloadable: true,
      updatedAt: now,
    })
  );
  await assertFails(
    studentADb.collection("resources").doc("res_1").update({
      isDownloadable: false,
      updatedAt: now,
    })
  );

  // Approved student A can list quizzes & resources with aligned query
  await assertSucceeds(
    studentADb.collection("quizzes").where("approvedStudentIds", "array-contains", STUDENT_A_UID).get()
  );
  // Unapproved student B cannot list all quizzes (unaligned), query Student A's quizzes, or get quiz_1
  await assertFails(studentBDb.collection("quizzes").get());
  await assertFails(
    studentBDb.collection("quizzes").where("approvedStudentIds", "array-contains", STUDENT_A_UID).get()
  );
  await assertFails(studentBDb.collection("quizzes").doc("quiz_1").get());

  // Student A submits attempt
  await assertSucceeds(
    studentADb.collection("quiz_attempts").doc("att_1").set({
      quizId: "quiz_1",
      quizTitle: "Génétique",
      classId: "class_1",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Amina",
      score: 1,
      maxScore: 1,
      totalDurationSeconds: 18,
      answers: [{ questionIndex: 0, selectedIndex: 0, isCorrect: true, timeSpentSeconds: 18 }],
      completedAt: now,
      createdAt: now,
    })
  );

  // Student A updates video tracking
  await assertSucceeds(
    studentADb.collection("video_tracking").doc("res_1_student_a_1").set({
      resourceId: "res_1",
      classId: "class_1",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Amina",
      maxPositionSeconds: 115,
      durationSeconds: 120,
      watchedPercentage: 95.8,
      status: "completed",
      lastWatchedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student A records badge notification
  await assertSucceeds(
    studentADb.collection("student_notifications").doc("notif_1").set({
      studentId: STUDENT_A_UID,
      teacherId: TEACHER_UID,
      type: "badge",
      title: "Nouveau badge débloqué !",
      body: "Vous avez obtenu le badge Sans Faute",
      isRead: false,
      createdAt: now,
    })
  );

  // Student B cannot read Student A's attempt, video tracking, or notification
  await assertFails(studentBDb.collection("quiz_attempts").doc("att_1").get());
  await assertFails(studentBDb.collection("video_tracking").doc("res_1_student_a_1").get());
  await assertFails(studentBDb.collection("student_notifications").doc("notif_1").get());
});

test("Class rosters: teacher creates/reads roster, student only sees own entry, cross-student denied", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await assertSucceeds(
    teacherDb.collection("classes").doc("class_1").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "2Bac SVT",
      academicYear: "2026-2027",
      level: "2Bac",
      qrToken: "SVT_ROSTER1",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    teacherDb.collection("class_rosters").doc("class_1_G134567890").set({
      classId: "class_1",
      className: "2Bac SVT",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      qrToken: "SVT_ROSTER1",
      fullName: "Amina El Amrani",
      massarNumber: "G134567890",
      provisionalPassword: "Svt8492K",
      studentUid: STUDENT_A_UID,
      qrPayload: "BIOTERRE:SVT_ROSTER1|G134567890",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student A can read their own roster entry
  await assertSucceeds(studentADb.collection("class_rosters").doc("class_1_G134567890").get());

  // Student B cannot read or modify Student A's roster entry
  await assertFails(studentBDb.collection("class_rosters").doc("class_1_G134567890").get());
  await assertFails(
    studentBDb.collection("class_rosters").doc("class_1_G999999999").set({
      classId: "class_1",
      className: "2Bac SVT",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      qrToken: "SVT_ROSTER1",
      fullName: "Intruder",
      massarNumber: "G999999999",
      provisionalPassword: "Svt1111A",
      studentUid: STUDENT_B_UID,
      qrPayload: "BIOTERRE:SVT_ROSTER1|G999999999",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Resource chunks & app updates: teacher writes, only approved students in same class can read", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("classes").doc("class_1").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "2Bac SVT",
      academicYear: "2026-2027",
      level: "2Bac",
      qrToken: "SVT_CHUNK1",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_1",
      className: "2Bac SVT",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "0600000000",
      status: "approved",
      requestedAt: now,
      updatedAt: now,
    });
    await db.collection("enrollments").doc(STUDENT_B_UID).set({
      studentId: STUDENT_B_UID,
      classId: "class_1",
      className: "2Bac SVT",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Youssef",
      studentPhone: "0611111111",
      status: "pending",
      requestedAt: now,
      updatedAt: now,
    });
  });

  const rulesPkgDir = require.resolve("@firebase/rules-unit-testing");
  const fbCompat = require(require.resolve("firebase/compat/app", { paths: [rulesPkgDir] }));
  require(require.resolve("firebase/compat/firestore", { paths: [rulesPkgDir] }));
  const sampleBytes = fbCompat.firestore.Blob.fromUint8Array(new Uint8Array([37, 80, 68, 70]));
  await assertSucceeds(
    teacherDb.collection("resource_chunks").doc("res_1_chunk_0").set({
      resourceId: "res_1",
      classId: "class_1",
      teacherId: TEACHER_UID,
      chunkIndex: 0,
      totalChunks: 1,
      data: sampleBytes,
      createdAt: now,
    })
  );

  // Approved student A can query chunks for class_1; pending student B cannot
  await assertSucceeds(
    studentADb.collection("resource_chunks")
      .where("resourceId", "==", "res_1")
      .where("classId", "==", "class_1")
      .get()
  );
  await assertFails(
    studentBDb.collection("resource_chunks")
      .where("resourceId", "==", "res_1")
      .where("classId", "==", "class_1")
      .get()
  );
  await assertFails(
    studentADb.collection("resource_chunks").doc("res_1_chunk_1").set({
      resourceId: "res_1",
      classId: "class_1",
      teacherId: STUDENT_A_UID,
      chunkIndex: 1,
      totalChunks: 2,
      data: sampleBytes,
      createdAt: now,
    })
  );

  // Teacher can publish HTTPS app update; non-https link fails; approved student reads, pending denied
  await assertFails(
    teacherDb.collection("app_updates").doc("upd_bad").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "Nouvelle version",
      downloadUrl: "http://insecure.example.com/app.apk",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    teacherDb.collection("app_updates").doc("upd_1").set({
      classId: "class_1",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      versionCode: 2,
      versionName: "1.1",
      releaseNotes: "Correction lecture vidéos et fichiers",
      downloadUrl: "https://github.com/example/bioterre/releases/latest",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    studentADb.collection("app_updates").where("classId", "==", "class_1").get()
  );
  await assertFails(
    studentBDb.collection("app_updates").where("classId", "==", "class_1").get()
  );

  // Teacher can query their own resource_chunks using teacherId filter
  await assertSucceeds(
    teacherDb.collection("resource_chunks")
      .where("resourceId", "==", "res_1")
      .where("classId", "==", "class_1")
      .where("teacherId", "==", TEACHER_UID)
      .get()
  );
});

test("Auto-approved QR enrollment and first-time video_tracking + PDF open tracking", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await teacherDb.collection("classes").doc("class_qr").set({
    teacherId: TEACHER_UID,
    teacherName: "Prof. BIROUK",
    name: "2Bac SVT QR",
    academicYear: "2026-2027",
    level: "2Bac",
    qrToken: "SVT-AUTO-99",
    createdAt: now,
    updatedAt: now,
  });
  await teacherDb.collection("class_codes").doc("SVT-AUTO-99").set({
    classId: "class_qr",
    className: "2Bac SVT QR",
    academicYear: "2026-2027",
    teacherId: TEACHER_UID,
    teacherName: "Prof. BIROUK",
    createdAt: now,
  });
  await teacherDb.collection("resources").doc("res_vid_qr").set({
    classId: "class_qr",
    teacherId: TEACHER_UID,
    title: "Vidéo QR",
    description: "Test",
    type: "video",
    fileUrl: "firestore://resource_chunks/res_vid_qr?ext=mp4",
    durationSeconds: 30,
    approvedStudentIds: [],
    isDownloadable: true,
    createdAt: now,
    updatedAt: now,
  });
  await teacherDb.collection("resources").doc("res_pdf_qr").set({
    classId: "class_qr",
    teacherId: TEACHER_UID,
    title: "Cours PDF",
    description: "Test",
    type: "pdf",
    fileUrl: "firestore://resource_chunks/res_pdf_qr?ext=pdf",
    durationSeconds: 0,
    approvedStudentIds: [],
    isDownloadable: true,
    createdAt: now,
    updatedAt: now,
  });

  // Auto-approved enrollment with valid matching qrToken succeeds immediately
  await assertSucceeds(
    studentADb.collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_qr",
      className: "2Bac SVT QR",
      teacherId: TEACHER_UID,
      teacherName: "Prof. BIROUK",
      studentName: "Salma",
      studentPhone: "0600000000",
      qrToken: "SVT-AUTO-99",
      status: "approved",
      requestedAt: now,
      validatedAt: now,
      updatedAt: now,
    })
  );

  // Enrollment with fake qrToken & approved status fails
  await assertFails(
    studentBDb.collection("enrollments").doc(STUDENT_B_UID).set({
      studentId: STUDENT_B_UID,
      classId: "class_qr",
      className: "2Bac SVT QR",
      teacherId: TEACHER_UID,
      teacherName: "Prof. BIROUK",
      studentName: "Intruder",
      studentPhone: "0611111111",
      qrToken: "FAKE-TOKEN",
      status: "approved",
      requestedAt: now,
      validatedAt: now,
      updatedAt: now,
    })
  );

  // Auto-approved student can list class resources via whereEqualTo("classId", ...)
  await assertSucceeds(
    studentADb.collection("resources").where("classId", "==", "class_qr").get()
  );

  // First-time get() on non-existent video_tracking doc succeeds, then set() and update() succeed!
  const vidTrackRef = studentADb.collection("video_tracking").doc(`res_vid_qr_${STUDENT_A_UID}`);
  await assertSucceeds(vidTrackRef.get());
  await assertSucceeds(
    vidTrackRef.set({
      resourceId: "res_vid_qr",
      classId: "class_qr",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Salma",
      maxPositionSeconds: 15,
      durationSeconds: 30,
      watchedPercentage: 50,
      status: "partial",
      lastWatchedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    vidTrackRef.update({
      maxPositionSeconds: 30,
      watchedPercentage: 100,
      status: "completed",
      lastWatchedAt: now,
      updatedAt: now,
    })
  );

  // PDF open tracking succeeds
  await assertSucceeds(
    studentADb.collection("video_tracking").doc(`res_pdf_qr_${STUDENT_A_UID}`).set({
      resourceId: "res_pdf_qr",
      classId: "class_qr",
      teacherId: TEACHER_UID,
      studentId: STUDENT_A_UID,
      studentName: "Salma",
      maxPositionSeconds: 1,
      durationSeconds: 1,
      watchedPercentage: 100,
      status: "completed",
      lastWatchedAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Other student B cannot read Student A's existing video tracking
  await assertFails(
    studentBDb.collection("video_tracking").doc(`res_vid_qr_${STUDENT_A_UID}`).get()
  );
});

test("Single-device student lock: immutable to student once bound, resettable only by enrolled teacher", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  // Seed class and approved enrollment for Student A with TEACHER_UID
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("classes").doc("class_lock").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "2Bac Lock",
      academicYear: "2026-2027",
      level: "2Bac",
      qrToken: "SVT_LOCK_1",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_lock",
      className: "2Bac Lock",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "0600000000",
      status: "approved",
      requestedAt: now,
      updatedAt: now,
    });
  });

  // Student A creates profile initially unbound
  await assertSucceeds(
    studentADb.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      fullName: "Amina El Amrani",
      phoneNumber: "+212600112233",
      role: "student",
      acceptedTerms: false,
      xp: 0,
      loginStreak: 1,
      lastLoginDate: "2026-11-15",
      unlockedBadgeIds: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Student A claims their first physical device
  await assertSucceeds(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "dev_phone_1",
      boundDeviceModel: "Samsung A55",
      updatedAt: now,
    })
  );

  // Student A cannot overwrite or clear their own boundDeviceId once set
  await assertFails(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "dev_phone_2",
      boundDeviceModel: "Redmi Note 13",
      updatedAt: now,
    })
  );
  await assertFails(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "",
      boundDeviceModel: "",
      updatedAt: now,
    })
  );

  // Student B cannot reset Student A's boundDeviceId or read Student A's profile
  await assertFails(
    studentBDb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "",
      boundDeviceModel: "",
      updatedAt: now,
    })
  );
  await assertFails(studentBDb.collection("users").doc(STUDENT_A_UID).get());

  // Teacher cannot read Student A's private profile PII, nor set a fake device ID,
  // but CAN reset Student A's boundDeviceId to ""
  await assertFails(teacherDb.collection("users").doc(STUDENT_A_UID).get());
  await assertFails(
    teacherDb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "hijack_device",
      boundDeviceModel: "Other",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    teacherDb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "",
      boundDeviceModel: "",
      updatedAt: now,
    })
  );

  // Student A can now claim a new phone after teacher reset
  await assertSucceeds(
    studentADb.collection("users").doc(STUDENT_A_UID).update({
      boundDeviceId: "dev_phone_2",
      boundDeviceModel: "Pixel 8a",
      updatedAt: now,
    })
  );
});

test("Class visibleLabDomains: teacher updates valid themes, approved student reads via get, invalid/empty denied", async () => {
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const now = serverNow();

  await assertSucceeds(
    teacherDb.collection("classes").doc("class_themes").set({
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      name: "2Bac Thèmes",
      academicYear: "2026-2027",
      level: "2Bac",
      qrToken: "SVT_THEMES_1",
      visibleLabDomains: [
        "CELL_GENETICS",
        "IMMUNITY",
        "REPRODUCTION",
        "PHYSIOLOGY_IMMUNITY",
        "NERVOUS_MUSCULAR",
        "GEOLOGY",
        "ECOLOGY",
      ],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Teacher updates themes to a subset including IMMUNITY & REPRODUCTION
  await assertSucceeds(
    teacherDb.collection("classes").doc("class_themes").update({
      visibleLabDomains: ["IMMUNITY", "REPRODUCTION", "NERVOUS_MUSCULAR"],
      updatedAt: now,
    })
  );

  // Empty visibleLabDomains or unknown domain name fails
  await assertFails(
    teacherDb.collection("classes").doc("class_themes").update({
      visibleLabDomains: [],
      updatedAt: now,
    })
  );
  await assertFails(
    teacherDb.collection("classes").doc("class_themes").update({
      visibleLabDomains: ["ASTROLOGY"],
      updatedAt: now,
    })
  );

  // Seed approved enrollment for Student A
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("enrollments").doc(STUDENT_A_UID).set({
      studentId: STUDENT_A_UID,
      classId: "class_themes",
      className: "2Bac Thèmes",
      teacherId: TEACHER_UID,
      teacherName: "Prof. Karim",
      studentName: "Amina",
      studentPhone: "0600000000",
      status: "approved",
      requestedAt: now,
      updatedAt: now,
    });
  });

  // Approved student A can read class_themes doc, cannot modify themes, cannot scrape all classes
  await assertSucceeds(studentADb.collection("classes").doc("class_themes").get());
  await assertFails(studentADb.collection("classes").get());
  await assertFails(
    studentADb.collection("classes").doc("class_themes").update({
      visibleLabDomains: ["GEOLOGY"],
      updatedAt: now,
    })
  );

  // Unapproved student B cannot read class_themes doc
  await assertFails(studentBDb.collection("classes").doc("class_themes").get());
});




