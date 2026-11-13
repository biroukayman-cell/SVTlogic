package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.AppLanguage
import com.example.core.ConnectivityObserver
import com.example.core.NotificationHelper
import com.example.data.ai.GeneratedQuizDraft
import com.example.data.ai.SvtAiRepository
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentNotification
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.example.data.offline.DownloadedResource
import com.example.data.offline.OfflineStorageManager
import com.example.data.offline.PendingQuizAttempt
import com.example.data.offline.PendingVideoTracking
import com.example.data.repository.SvtRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ProfileGateState {
    data object Loading : ProfileGateState
    data object NeedsSetup : ProfileGateState
    data class Ready(val profile: UserProfile) : ProfileGateState
    data class Error(val message: String) : ProfileGateState
}

sealed interface AiQuizState {
    data object Idle : AiQuizState
    data object Loading : AiQuizState
    data class Success(val draft: GeneratedQuizDraft) : AiQuizState
    data class Error(val message: String) : AiQuizState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SvtMainViewModel(
    appContext: Context,
    private val repository: SvtRepository,
    private val aiRepository: SvtAiRepository,
    private val offlineStorage: OfflineStorageManager,
    private val connectivityObserver: ConnectivityObserver,
    private val currentUserId: String
) : ViewModel() {

    private val context = appContext.applicationContext
    private val syncMutex = Mutex()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    val downloadedResources: StateFlow<Map<String, DownloadedResource>> = offlineStorage.downloadedResources
    val downloadedQuizzes: StateFlow<Map<String, Quiz>> = offlineStorage.downloadedQuizzes
    val downloadingProgress: StateFlow<Map<String, Float>> = offlineStorage.downloadingProgress
    val pendingAttempts: StateFlow<List<PendingQuizAttempt>> = offlineStorage.pendingAttempts
    val pendingVideoTrackings: StateFlow<List<PendingVideoTracking>> = offlineStorage.pendingVideoTrackings

    private val _isSyncingOffline = MutableStateFlow(false)
    val isSyncingOffline: StateFlow<Boolean> = _isSyncingOffline.asStateFlow()

    val profileState: StateFlow<ProfileGateState> = repository.observeUserProfile(currentUserId)
        .map { profile ->
            if (profile == null) ProfileGateState.NeedsSetup
            else ProfileGateState.Ready(profile)
        }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Profile stream error", fsErr ?: error)
            emit(ProfileGateState.Error((fsErr ?: error).message ?: "Error loading profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ProfileGateState.Loading
        )

    private val roleFlow = profileState.map { state ->
        (state as? ProfileGateState.Ready)?.profile?.role
    }

    // Teacher streams
    val teacherClasses: StateFlow<List<SvtClass>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherClasses(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherEnrollments: StateFlow<List<Enrollment>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherEnrollments(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherQuizzes: StateFlow<List<Quiz>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherQuizzes(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherAttempts: StateFlow<List<QuizAttempt>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherAttempts(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherResources: StateFlow<List<CourseResource>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherResources(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherVideoTracking: StateFlow<List<VideoTracking>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherVideoTracking(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherAnnouncements: StateFlow<List<Announcement>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherAnnouncements(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teacherMessages: StateFlow<List<PrivateMessage>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherMessages(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Student streams
    val studentEnrollment: StateFlow<Enrollment?> = roleFlow.flatMapLatest { role ->
        if (role == "student") repository.observeStudentEnrollment(currentUserId)
            .catch { emit(null) }
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val isApprovedStudent = studentEnrollment.map { it?.status == "approved" }

    private val remoteStudentQuizzes: StateFlow<List<Quiz>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentQuizzes(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentQuizzes: StateFlow<List<Quiz>> = combine(
        remoteStudentQuizzes,
        downloadedQuizzes
    ) { remote, offlineMap ->
        val byId = LinkedHashMap<String, Quiz>()
        remote.forEach { byId[it.id] = it }
        offlineMap.values.forEach { offlineQuiz ->
            if (!byId.containsKey(offlineQuiz.id)) {
                byId[offlineQuiz.id] = offlineQuiz
            }
        }
        byId.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val remoteStudentAttempts: StateFlow<List<QuizAttempt>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentAttempts(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentAttempts: StateFlow<List<QuizAttempt>> = combine(
        remoteStudentAttempts,
        pendingAttempts
    ) { remote, pending ->
        val remoteQuizIds = remote.map { it.quizId }.toSet()
        val pendingAsAttempts = pending
            .filterNot { remoteQuizIds.contains(it.quizId) }
            .map { it.toQuizAttempt() }
        pendingAsAttempts + remote
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val remoteStudentResources: StateFlow<List<CourseResource>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentResources(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentResources: StateFlow<List<CourseResource>> = combine(
        remoteStudentResources,
        downloadedResources
    ) { remote, offlineMap ->
        val byId = LinkedHashMap<String, CourseResource>()
        remote.forEach { byId[it.id] = it }
        offlineMap.values.forEach { downloaded ->
            if (!byId.containsKey(downloaded.resourceId)) {
                byId[downloaded.resourceId] = downloaded.toCourseResource()
            }
        }
        byId.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val remoteStudentVideoTracking: StateFlow<List<VideoTracking>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentVideoTracking(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentVideoTracking: StateFlow<List<VideoTracking>> = combine(
        remoteStudentVideoTracking,
        pendingVideoTrackings
    ) { remote, pending ->
        val byResId = LinkedHashMap<String, VideoTracking>()
        remote.forEach { byResId[it.resourceId] = it }
        pending.forEach { p ->
            val localTracking = p.toVideoTracking(currentUserId)
            val existing = byResId[p.resource.id]
            if (existing == null || localTracking.watchedPercentage > existing.watchedPercentage) {
                byResId[p.resource.id] = localTracking
            }
        }
        byResId.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentAnnouncements: StateFlow<List<Announcement>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentAnnouncements(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentMessages: StateFlow<List<PrivateMessage>> = isApprovedStudent.flatMapLatest { approved ->
        if (approved) repository.observeStudentMessages(currentUserId).catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentNotifications: StateFlow<List<StudentNotification>> = roleFlow.flatMapLatest { role ->
        if (role == "student") repository.observeStudentNotifications(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedClassId = MutableStateFlow<String?>(null)
    val selectedClassId: StateFlow<String?> = combine(teacherClasses, _selectedClassId) { classes, chosen ->
        if (chosen != null && classes.any { it.id == chosen }) chosen
        else classes.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _newlyUnlockedBadges = MutableStateFlow<List<SvtBadgeDefinition>>(emptyList())
    val newlyUnlockedBadges: StateFlow<List<SvtBadgeDefinition>> = _newlyUnlockedBadges.asStateFlow()

    private val _aiQuizState = MutableStateFlow<AiQuizState>(AiQuizState.Idle)
    val aiQuizState: StateFlow<AiQuizState> = _aiQuizState.asStateFlow()

    private var streakCheckedThisSession = false

    init {
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    syncPendingOfflineData(context)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityObserver.stop()
    }

    fun selectClass(classId: String) {
        _selectedClassId.value = classId
    }

    fun clearError() {
        _actionError.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissUnlockedBadges() {
        _newlyUnlockedBadges.value = emptyList()
    }

    fun resetAiState() {
        _aiQuizState.value = AiQuizState.Idle
    }

    fun setupProfile(fullName: String, phone: String, role: String) {
        viewModelScope.launch {
            try {
                repository.createUserProfile(fullName, phone, role)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to create profile"
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, avatarUrl: String?) {
        viewModelScope.launch {
            try {
                repository.updateProfileInfo(fullName, phone, avatarUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun syncStreakAndBadgesIfNeeded(uiContext: Context) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (profile.role != "student" || streakCheckedThisSession) return
        streakCheckedThisSession = true
        val teacherId = studentEnrollment.value?.teacherId ?: ""
        evaluateAndNotifyBadges(uiContext, profile, studentAttempts.value, studentVideoTracking.value, teacherId)
    }

    private fun evaluateAndNotifyBadges(
        uiContext: Context,
        profile: UserProfile,
        attempts: List<QuizAttempt>,
        trackings: List<VideoTracking>,
        teacherId: String
    ) {
        viewModelScope.launch {
            try {
                val unlocked = repository.refreshLoginStreakAndEvaluateBadges(
                    profile = profile,
                    attempts = attempts,
                    videoTrackings = trackings,
                    teacherId = teacherId
                )
                if (unlocked.isNotEmpty()) {
                    _newlyUnlockedBadges.value = unlocked
                    unlocked.forEach { badge ->
                        val title = uiContext.getString(R.string.badge_unlocked_banner_title)
                        val badgeName = uiContext.getString(badge.titleRes)
                        val body = uiContext.getString(R.string.badge_unlocked_banner_body, badgeName, badge.xpReward)
                        NotificationHelper.showBadgeOrAlertNotification(uiContext, title, body, badge.id.hashCode())
                        repository.createBadgeNotification(profile.userId, teacherId, title, body)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Badge evaluation warning", e)
            }
        }
    }

    fun joinClassByQrToken(
        qrToken: String,
        studentName: String,
        studentPhone: String,
        invalidCodeMessage: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val code = repository.lookupClassByQrToken(qrToken)
                if (code == null) {
                    _actionError.value = invalidCodeMessage
                    onResult(false)
                } else {
                    repository.requestEnrollment(code, studentName, studentPhone)
                    onResult(true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: invalidCodeMessage
                onResult(false)
            }
        }
    }

    fun resetRejectedEnrollment() {
        viewModelScope.launch {
            try {
                repository.clearOwnEnrollment()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun createClass(teacherName: String, name: String, level: String, academicYear: String) {
        viewModelScope.launch {
            try {
                val created = repository.createClass(teacherName, name, level, academicYear)
                _selectedClassId.value = created.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun deleteClass(svtClass: SvtClass) {
        viewModelScope.launch {
            try {
                repository.deleteClassCascade(svtClass)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun approveEnrollment(enrollment: Enrollment, notifTitle: String, notifBody: String) {
        viewModelScope.launch {
            try {
                repository.approveStudentEnrollment(enrollment, notifTitle, notifBody)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun rejectEnrollment(enrollment: Enrollment) {
        viewModelScope.launch {
            try {
                repository.rejectStudentEnrollment(enrollment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun deleteStudent(enrollment: Enrollment) {
        viewModelScope.launch {
            try {
                repository.deleteStudentFromClass(enrollment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun generateQuizWithAi(chapter: String, level: String, count: Int, language: AppLanguage) {
        viewModelScope.launch {
            _aiQuizState.value = AiQuizState.Loading
            try {
                val draft = aiRepository.generateSvtQuiz(chapter, level, count, language)
                _aiQuizState.value = AiQuizState.Success(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiQuizState.value = AiQuizState.Error(e.localizedMessage ?: "AI generation error")
            }
        }
    }

    fun publishQuiz(
        classId: String,
        title: String,
        description: String,
        chapterSvt: String,
        questions: List<QuizQuestion>,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ) {
        val approvedIds = teacherEnrollments.value
            .filter { it.classId == classId && it.status == "approved" }
            .map { it.studentId }
        viewModelScope.launch {
            try {
                repository.createQuizForClass(
                    classId = classId,
                    title = title,
                    description = description,
                    chapterSvt = chapterSvt,
                    questions = questions,
                    approvedStudentIds = approvedIds,
                    notifTitle = notifTitle,
                    notifBody = notifBody,
                    isDownloadable = isDownloadable
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun toggleQuizDownloadable(quiz: Quiz) {
        viewModelScope.launch {
            try {
                repository.updateQuizDownloadable(quiz.id, !quiz.isDownloadable)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun downloadQuizOffline(uiContext: Context, quiz: Quiz) {
        viewModelScope.launch {
            try {
                offlineStorage.saveQuizOffline(quiz)
                _statusMessage.value = uiContext.getString(R.string.offline_download_success, quiz.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun removeOfflineQuiz(quizId: String) {
        viewModelScope.launch {
            offlineStorage.removeOfflineQuiz(quizId)
        }
    }

    fun submitQuizAttempt(
        uiContext: Context,
        quiz: Quiz,
        selectedAnswers: List<Int>,
        perQuestionSeconds: List<Long>,
        totalDurationSeconds: Long,
        onSubmitted: (QuizAttempt) -> Unit
    ) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        val answersRecords = quiz.questions.mapIndexed { index, q ->
            val selected = selectedAnswers.getOrElse(index) { -1 }
            val isCorrect = selected == q.correctIndex
            val timeSpent = perQuestionSeconds.getOrElse(index) { 0L }.coerceAtLeast(0L)
            QuestionAnswerRecord(
                questionIndex = index,
                selectedIndex = selected,
                isCorrect = isCorrect,
                timeSpentSeconds = timeSpent
            )
        }
        val score = answersRecords.count { it.isCorrect }.toDouble()
        val maxScore = quiz.questions.size.coerceAtLeast(1).toDouble()
        val deterministicAttemptId = buildDeterministicAttemptId(quiz.id, currentUserId)

        val localAttempt = QuizAttempt(
            id = deterministicAttemptId,
            quizId = quiz.id,
            quizTitle = quiz.title,
            classId = quiz.classId,
            teacherId = quiz.teacherId,
            studentId = currentUserId,
            studentName = profile.fullName,
            score = score,
            maxScore = maxScore,
            totalDurationSeconds = totalDurationSeconds.coerceAtLeast(0L),
            answers = answersRecords
        )

        val pendingRecord = PendingQuizAttempt(
            attemptId = deterministicAttemptId,
            quizId = quiz.id,
            quizTitle = quiz.title,
            classId = quiz.classId,
            teacherId = quiz.teacherId,
            studentId = currentUserId,
            studentName = profile.fullName,
            score = score,
            maxScore = maxScore,
            totalDurationSeconds = totalDurationSeconds.coerceAtLeast(0L),
            answers = answersRecords,
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (!isOnline.value) {
                offlineStorage.enqueuePendingAttempt(pendingRecord)
                _statusMessage.value = uiContext.getString(R.string.offline_attempt_cached_msg)
                onSubmitted(localAttempt)
                return@launch
            }

            try {
                val savedAttempt = repository.submitQuizAttempt(
                    quiz = quiz,
                    studentName = profile.fullName,
                    selectedAnswers = selectedAnswers,
                    perQuestionSeconds = perQuestionSeconds,
                    totalDurationSeconds = totalDurationSeconds,
                    attemptId = deterministicAttemptId
                )
                onSubmitted(savedAttempt)
                val updatedAttempts = studentAttempts.value.filterNot { it.quizId == quiz.id } + savedAttempt
                evaluateAndNotifyBadges(
                    uiContext = uiContext,
                    profile = profile,
                    attempts = updatedAttempts,
                    trackings = studentVideoTracking.value,
                    teacherId = quiz.teacherId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Online attempt submission failed, caching offline for auto-sync", e)
                offlineStorage.enqueuePendingAttempt(pendingRecord)
                _statusMessage.value = uiContext.getString(R.string.offline_attempt_cached_msg)
                onSubmitted(localAttempt)
            }
        }
    }

    fun syncPendingOfflineData(uiContext: Context) {
        if (!isOnline.value) return
        viewModelScope.launch {
            syncMutex.withLock {
                val attemptsToSync = offlineStorage.pendingAttempts.value
                val trackingsToSync = offlineStorage.pendingVideoTrackings.value
                if (attemptsToSync.isEmpty() && trackingsToSync.isEmpty()) return@withLock

                _isSyncingOffline.value = true
                var syncedCount = 0
                var lastTeacherId = ""
                try {
                    for (pending in attemptsToSync) {
                        try {
                            repository.syncPendingQuizAttempt(
                                attemptId = pending.attemptId,
                                quizId = pending.quizId,
                                quizTitle = pending.quizTitle,
                                classId = pending.classId,
                                teacherId = pending.teacherId,
                                studentName = pending.studentName,
                                score = pending.score,
                                maxScore = pending.maxScore,
                                totalDurationSeconds = pending.totalDurationSeconds,
                                answers = pending.answers
                            )
                            offlineStorage.removePendingAttempt(pending.attemptId)
                            lastTeacherId = pending.teacherId
                            syncedCount++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed syncing pending attempt ${pending.attemptId}", e)
                        }
                    }

                    for (tracking in trackingsToSync) {
                        try {
                            val existing = remoteStudentVideoTracking.value.firstOrNull {
                                it.resourceId == tracking.resource.id
                            }
                            repository.upsertVideoTracking(
                                resource = tracking.resource,
                                studentName = tracking.studentName,
                                positionSeconds = tracking.maxPositionSeconds,
                                totalDurationSeconds = tracking.totalDurationSeconds,
                                existingTracking = existing
                            )
                            offlineStorage.removePendingVideoTracking(tracking.resource.id)
                            lastTeacherId = tracking.resource.teacherId
                            syncedCount++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed syncing pending video tracking ${tracking.resource.id}", e)
                        }
                    }

                    if (syncedCount > 0) {
                        _statusMessage.value = uiContext.getString(R.string.offline_sync_completed, syncedCount)
                        val profile = (profileState.value as? ProfileGateState.Ready)?.profile
                        if (profile != null && profile.role == "student") {
                            evaluateAndNotifyBadges(
                                uiContext = uiContext,
                                profile = profile,
                                attempts = studentAttempts.value,
                                trackings = studentVideoTracking.value,
                                teacherId = lastTeacherId.ifBlank { studentEnrollment.value?.teacherId.orEmpty() }
                            )
                        }
                    }
                } finally {
                    _isSyncingOffline.value = false
                }
            }
        }
    }

    fun addResource(
        classId: String,
        title: String,
        description: String,
        type: String,
        fileUrl: String,
        durationSeconds: Long,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ) {
        val approvedIds = teacherEnrollments.value
            .filter { it.classId == classId && it.status == "approved" }
            .map { it.studentId }
        viewModelScope.launch {
            try {
                repository.createResource(
                    classId = classId,
                    title = title,
                    description = description,
                    type = type,
                    fileUrl = fileUrl,
                    durationSeconds = durationSeconds,
                    approvedStudentIds = approvedIds,
                    notifTitle = notifTitle,
                    notifBody = notifBody,
                    isDownloadable = isDownloadable
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun toggleResourceDownloadable(resource: CourseResource) {
        viewModelScope.launch {
            try {
                repository.updateResourceDownloadable(resource.id, !resource.isDownloadable)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun downloadResourceOffline(uiContext: Context, resource: CourseResource) {
        viewModelScope.launch {
            try {
                offlineStorage.downloadResource(resource)
                _statusMessage.value = uiContext.getString(R.string.offline_download_success, resource.title)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = uiContext.getString(
                    R.string.offline_download_error,
                    e.localizedMessage ?: "Error"
                )
            }
        }
    }

    fun removeOfflineResource(resourceId: String) {
        viewModelScope.launch {
            offlineStorage.removeDownloadedResource(resourceId)
        }
    }

    fun recordVideoProgress(
        uiContext: Context,
        resource: CourseResource,
        positionSeconds: Long,
        durationSeconds: Long
    ) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (profile.role != "student") return
        val existing = studentVideoTracking.value.firstOrNull { it.resourceId == resource.id }
        viewModelScope.launch {
            if (!isOnline.value) {
                offlineStorage.enqueuePendingVideoTracking(
                    PendingVideoTracking(
                        resource = resource,
                        studentName = profile.fullName,
                        maxPositionSeconds = positionSeconds,
                        totalDurationSeconds = durationSeconds,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                return@launch
            }
            try {
                val updated = repository.upsertVideoTracking(
                    resource = resource,
                    studentName = profile.fullName,
                    positionSeconds = positionSeconds,
                    totalDurationSeconds = durationSeconds,
                    existingTracking = existing
                )
                val mergedTrackings = studentVideoTracking.value
                    .filterNot { it.resourceId == resource.id } + updated
                evaluateAndNotifyBadges(
                    uiContext = uiContext,
                    profile = profile,
                    attempts = studentAttempts.value,
                    trackings = mergedTrackings,
                    teacherId = resource.teacherId
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Video tracking offline fallback", e)
                offlineStorage.enqueuePendingVideoTracking(
                    PendingVideoTracking(
                        resource = resource,
                        studentName = profile.fullName,
                        maxPositionSeconds = positionSeconds,
                        totalDurationSeconds = durationSeconds,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun publishAnnouncement(
        classId: String,
        title: String,
        content: String,
        notifTitle: String,
        notifBody: String
    ) {
        val approvedIds = teacherEnrollments.value
            .filter { it.classId == classId && it.status == "approved" }
            .map { it.studentId }
        viewModelScope.launch {
            try {
                repository.createAnnouncement(
                    classId = classId,
                    title = title,
                    content = content,
                    approvedStudentIds = approvedIds,
                    notifTitle = notifTitle,
                    notifBody = notifBody
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun sendMessageAsStudent(content: String) {
        val enrollment = studentEnrollment.value ?: return
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendPrivateMessage(
                    classId = enrollment.classId,
                    teacherId = enrollment.teacherId,
                    studentId = currentUserId,
                    studentName = profile.fullName,
                    senderRole = "student",
                    content = content
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun sendMessageAsTeacher(
        classId: String,
        studentId: String,
        studentName: String,
        content: String,
        notifTitle: String
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendPrivateMessage(
                    classId = classId,
                    teacherId = currentUserId,
                    studentId = studentId,
                    studentName = studentName,
                    senderRole = "teacher",
                    content = content,
                    notifTitleForStudent = notifTitle
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun markThreadRead(messages: List<PrivateMessage>) {
        viewModelScope.launch {
            repository.markMessagesAsRead(messages)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead(studentNotifications.value)
        }
    }

    private fun buildDeterministicAttemptId(quizId: String, studentId: String): String {
        val raw = "att_${quizId}_$studentId"
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120)
    }

    private companion object {
        const val TAG = "SvtMainViewModel"
    }
}
