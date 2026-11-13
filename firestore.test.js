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
      xp: 0,
      loginStreak: 1,
      lastLoginDate: "2026-11-12",
      unlockedBadgeIds: [],
      createdAt: now,
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
