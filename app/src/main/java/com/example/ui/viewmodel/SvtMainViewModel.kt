package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AppUpdateUrlHelper
import com.example.core.AuthSessionDraft
import com.example.core.AvatarStorageHelper
import com.example.core.BiometricAuthHelper
import com.example.core.ConnectivityObserver
import com.example.core.DeviceSessionManager
import com.example.core.ImportedStudentRow
import com.example.core.NotificationHelper
import com.example.core.SvtBackgroundNotificationWorker
import com.example.core.SvtLabLogic
import com.example.core.TeacherAnalyticsLogic
import com.example.core.appCheckDiagnosticTag
import com.example.data.ai.AiAppCheckException
import com.example.data.ai.GeneratedQuizDraft
import com.example.data.ai.SvtAiRepository
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.Announcement
import com.example.data.model.AppUpdateRelease
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RosterStudent
import com.example.data.model.StudentNotification
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.example.data.offline.DownloadedResource
import com.example.data.offline.LocalUriUnavailableException
import com.example.data.offline.OfflineStorageManager
import com.example.data.offline.PendingQuizAttempt
import com.example.data.offline.PendingVideoTracking
import com.example.data.repository.ResourceReadException
import com.example.data.repository.ResourceUploadTooLargeException
import com.example.data.repository.SvtRepository
import com.example.data.svt.SvtDomain
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.tasks.await

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

sealed interface LabSvtSearchState {
    data object Idle : LabSvtSearchState
    data object Loading : LabSvtSearchState
    data class Definition(val query: String, val term: String, val definition: String) : LabSvtSearchState
    data class NonSvtRejected(val query: String) : LabSvtSearchState
    data class Error(val query: String, val message: String, val isOfflineOrAppCheck: Boolean = false) : LabSvtSearchState
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
    private val deviceBindMutex = Mutex()
    private var deviceBindAttempted = false

    val currentDeviceId: String = DeviceSessionManager.getDeviceId(context)
    val currentDeviceModel: String = DeviceSessionManager.getDeviceModel()

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    val downloadedResources: StateFlow<Map<String, DownloadedResource>> = offlineStorage.downloadedResources
    val downloadedQuizzes: StateFlow<Map<String, Quiz>> = offlineStorage.downloadedQuizzes
    val downloadingProgress: StateFlow<Map<String, Float>> = offlineStorage.downloadingProgress
    val pendingAttempts: StateFlow<List<PendingQuizAttempt>> = offlineStorage.pendingAttempts
    val pendingVideoTrackings: StateFlow<List<PendingVideoTracking>> = offlineStorage.pendingVideoTrackings

    private val _isSyncingOffline = MutableStateFlow(false)
    val isSyncingOffline: StateFlow<Boolean> = _isSyncingOffline.asStateFlow()

    private val _optimisticProfile = MutableStateFlow<UserProfile?>(null)
    private val _optimisticAvatarUrl = MutableStateFlow<String?>(null)
    private val _optimisticEnrollment = MutableStateFlow<Enrollment?>(null)
    private val _isSettingUpProfile = MutableStateFlow(false)
    val isSettingUpProfile: StateFlow<Boolean> = _isSettingUpProfile.asStateFlow()

    private val _isSavingAvatar = MutableStateFlow(false)
    val isSavingAvatar: StateFlow<Boolean> = _isSavingAvatar.asStateFlow()

    private val _isJoiningClass = MutableStateFlow(false)
    val isJoiningClass: StateFlow<Boolean> = _isJoiningClass.asStateFlow()

    private val _isAcceptingTerms = MutableStateFlow(false)
    val isAcceptingTerms: StateFlow<Boolean> = _isAcceptingTerms.asStateFlow()

    private val _profileRetryTrigger = MutableStateFlow(0)
    private val _authReloadTick = MutableStateFlow(0)

    val isOwnerAdminEmail: StateFlow<Boolean> = _authReloadTick.map {
        Firebase.auth.currentUser?.email.equals(
            SvtBackgroundNotificationWorker.OWNER_ADMIN_EMAIL,
            ignoreCase = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = Firebase.auth.currentUser?.email.equals(
            SvtBackgroundNotificationWorker.OWNER_ADMIN_EMAIL,
            ignoreCase = true
        )
    )

    val isVerifiedOwnerAdmin: StateFlow<Boolean> = _authReloadTick.map {
        val u = Firebase.auth.currentUser
        u != null &&
            u.email.equals(SvtBackgroundNotificationWorker.OWNER_ADMIN_EMAIL, ignoreCase = true) &&
            u.isEmailVerified
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = Firebase.auth.currentUser?.let { u ->
            u.email.equals(SvtBackgroundNotificationWorker.OWNER_ADMIN_EMAIL, ignoreCase = true) &&
                u.isEmailVerified
        } == true
    )

    val profileState: StateFlow<ProfileGateState> = combine(
        _profileRetryTrigger.flatMapLatest {
            repository.observeUserProfile(currentUserId)
                .map<UserProfile?, Result<UserProfile?>> { Result.success(it) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Profile stream error", fsErr ?: error)
                    emit(Result.failure(fsErr ?: error))
                }
        },
        _optimisticProfile,
        _optimisticAvatarUrl
    ) { remoteResult, optimistic, optimisticAvatar ->
        remoteResult.fold(
            onSuccess = { remote ->
                val base = remote ?: optimistic
                val withTerms = if (base != null && optimistic?.acceptedTerms == true && !base.acceptedTerms) {
                    base.copy(acceptedTerms = true)
                } else {
                    base
                }
                val resolved = if (withTerms != null && !optimisticAvatar.isNullOrBlank()) {
                    if (withTerms.avatarUrl == optimisticAvatar) {
                        _optimisticAvatarUrl.value = null
                        withTerms
                    } else {
                        withTerms.copy(avatarUrl = optimisticAvatar)
                    }
                } else {
                    withTerms
                }
                if (resolved == null) ProfileGateState.NeedsSetup
                else ProfileGateState.Ready(resolved)
            },
            onFailure = { err ->
                if (optimistic != null) {
                    val withAvatar = if (!optimisticAvatar.isNullOrBlank()) {
                        optimistic.copy(avatarUrl = optimisticAvatar)
                    } else optimistic
                    ProfileGateState.Ready(withAvatar)
                } else {
                    ProfileGateState.Error(err.message ?: "Error loading profile")
                }
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProfileGateState.Loading
    )

    private val roleFlow = profileState.map { state ->
        (state as? ProfileGateState.Ready)?.profile?.role
    }

    val allTeachersForAdmin: StateFlow<List<UserProfile>> = combine(
        roleFlow,
        isVerifiedOwnerAdmin
    ) { role, isVerifiedAdmin ->
        role == "teacher" && isVerifiedAdmin
    }.flatMapLatest { eligible ->
        if (eligible) repository.observeAllTeachers().catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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

    val teacherRoster: StateFlow<List<RosterStudent>> = roleFlow.flatMapLatest { role ->
        if (role == "teacher") repository.observeTeacherRoster(currentUserId)
            .catch { emit(emptyList()) }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Student streams
    val studentRoster: StateFlow<RosterStudent?> = repository.observeStudentRoster(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val studentEnrollment: StateFlow<Enrollment?> = combine(
        roleFlow.flatMapLatest { role ->
            if (role == "student") repository.observeStudentEnrollment(currentUserId)
                .catch { emit(null) }
            else flowOf(null)
        },
        _optimisticEnrollment
    ) { remote, optimistic ->
        remote ?: optimistic
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val approvedClassId = studentEnrollment.map { enr ->
        if (enr?.status == "approved" && enr.classId.isNotBlank()) enr.classId else null
    }

    private val isApprovedStudent = approvedClassId.map { it != null }

    val studentClass: StateFlow<SvtClass?> = approvedClassId.flatMapLatest { classId ->
        if (classId != null) repository.observeClassById(classId).catch { emit(null) }
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val studentAllowedLabDomains: StateFlow<Set<SvtDomain>> = studentClass.map { cls ->
        if (cls == null) {
            SvtDomain.entries.toSet()
        } else {
            SvtLabLogic.resolveVisibleLabDomains(cls.visibleLabDomains)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SvtDomain.entries.toSet())

    private val remoteStudentQuizzes: StateFlow<List<Quiz>> = approvedClassId.flatMapLatest { classId ->
        if (classId != null) repository.observeStudentQuizzes(classId).catch { emit(emptyList()) }
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

    private val remoteStudentResources: StateFlow<List<CourseResource>> = approvedClassId.flatMapLatest { classId ->
        if (classId != null) repository.observeStudentResources(classId).catch { emit(emptyList()) }
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

    val studentVideoOnlyTracking: StateFlow<List<VideoTracking>> = combine(
        studentVideoTracking,
        studentResources
    ) { trackings, resources ->
        filterVideoOnlyTrackings(trackings, resources)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentAnnouncements: StateFlow<List<Announcement>> = approvedClassId.flatMapLatest { classId ->
        if (classId != null) repository.observeStudentAnnouncements(classId).catch { emit(emptyList()) }
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

    val appUpdates: StateFlow<List<AppUpdateRelease>> = combine(
        roleFlow,
        studentEnrollment
    ) { role, enrollment ->
        role to enrollment
    }.flatMapLatest { (role, enrollment) ->
        when {
            role == "teacher" -> repository.observeTeacherAppUpdates(currentUserId).catch { emit(emptyList()) }
            role == "student" && enrollment?.status == "approved" && enrollment.classId.isNotBlank() ->
                repository.observeClassAppUpdates(enrollment.classId).catch { emit(emptyList()) }
            else -> flowOf(emptyList())
        }
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

    private val _labSvtSearchState = MutableStateFlow<LabSvtSearchState>(LabSvtSearchState.Idle)
    val labSvtSearchState: StateFlow<LabSvtSearchState> = _labSvtSearchState.asStateFlow()
    private var labSearchJob: Job? = null

    private val _isImportingRoster = MutableStateFlow(false)
    val isImportingRoster: StateFlow<Boolean> = _isImportingRoster.asStateFlow()

    private val _isUploadingResource = MutableStateFlow(false)
    val isUploadingResource: StateFlow<Boolean> = _isUploadingResource.asStateFlow()

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

    fun clearLabSvtSearch() {
        labSearchJob?.cancel()
        labSearchJob = null
        _labSvtSearchState.value = LabSvtSearchState.Idle
    }

    fun searchLabSvtTerm(rawQuery: String, language: AppLanguage) {
        val cleanQuery = rawQuery.trim()
        if (!SvtLabLogic.isSearchableSvtQuery(cleanQuery)) {
            labSearchJob?.cancel()
            labSearchJob = null
            _labSvtSearchState.value = if (cleanQuery.isBlank()) {
                LabSvtSearchState.Idle
            } else {
                LabSvtSearchState.NonSvtRejected(cleanQuery)
            }
            return
        }

        if (!isOnline.value) {
            labSearchJob?.cancel()
            labSearchJob = null
            _labSvtSearchState.value = LabSvtSearchState.Error(
                query = cleanQuery,
                message = context.getString(R.string.svt_glossary_offline_miss),
                isOfflineOrAppCheck = true
            )
            return
        }

        labSearchJob?.cancel()
        labSearchJob = viewModelScope.launch {
            _labSvtSearchState.value = LabSvtSearchState.Loading
            try {
                val result = aiRepository.lookupSvtTermDefinition(cleanQuery, language)
                _labSvtSearchState.value = if (result.isSvtTerm && result.definition.isNotBlank()) {
                    LabSvtSearchState.Definition(
                        query = cleanQuery,
                        term = result.term.ifBlank { cleanQuery },
                        definition = result.definition.trim()
                    )
                } else {
                    LabSvtSearchState.NonSvtRejected(cleanQuery)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: AiAppCheckException) {
                _labSvtSearchState.value = LabSvtSearchState.Error(
                    query = cleanQuery,
                    message = context.getString(R.string.svt_glossary_offline_miss),
                    isOfflineOrAppCheck = true
                )
            } catch (e: Exception) {
                _labSvtSearchState.value = LabSvtSearchState.Error(
                    query = cleanQuery,
                    message = context.getString(
                        R.string.svt_glossary_lookup_error,
                        e.localizedMessage ?: "Error"
                    ),
                    isOfflineOrAppCheck = false
                )
            }
        }
    }

    fun setupProfile(
        fullName: String,
        phone: String,
        role: String,
        massarNumber: String = "",
        birthDate: String = "",
        email: String = "",
        establishment: String = "",
        mustChangePassword: Boolean = false,
        pendingQrToken: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_isSettingUpProfile.value) return
        viewModelScope.launch {
            _isSettingUpProfile.value = true
            try {
                val resolvedEmail = if (role == "student") {
                    email.trim().ifBlank { AuthSessionDraft.defaultStudentTaalimEmail(massarNumber) }
                } else {
                    email.trim()
                }
                val createdProfile = repository.createUserProfile(
                    fullName = fullName,
                    phoneNumber = phone,
                    role = role,
                    avatarUrl = null,
                    massarNumber = massarNumber,
                    birthDate = birthDate,
                    email = resolvedEmail,
                    establishment = establishment,
                    mustChangePassword = mustChangePassword,
                    boundDeviceId = if (role == "student") currentDeviceId else "",
                    boundDeviceModel = if (role == "student") currentDeviceModel else ""
                )
                _optimisticProfile.value = createdProfile
                _profileRetryTrigger.value += 1

                val cleanToken = pendingQrToken.trim()
                if (role == "student" && cleanToken.isNotBlank()) {
                    val code = repository.lookupClassByQrToken(cleanToken)
                    if (code != null) {
                        val enrollment = repository.requestEnrollment(
                            classCode = code,
                            studentName = createdProfile.fullName,
                            studentPhone = createdProfile.phoneNumber,
                            massarNumber = createdProfile.massarNumber,
                            qrToken = cleanToken
                        )
                        _optimisticEnrollment.value = enrollment
                    }
                }
                val cleanRecoveryEmail = resolvedEmail.trim()
                if (role == "student" &&
                    cleanRecoveryEmail.isNotBlank() &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(cleanRecoveryEmail).matches()
                ) {
                    runCatching {
                        Firebase.auth.currentUser?.verifyBeforeUpdateEmail(cleanRecoveryEmail)?.await()
                    }
                }
                onComplete(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to create profile"
                onComplete(false)
            } finally {
                _isSettingUpProfile.value = false
            }
        }
    }

    fun updateProfile(
        fullName: String,
        phone: String,
        avatarUrl: String?,
        birthDate: String? = null,
        email: String? = null,
        uiContext: Context? = null
    ) {
        viewModelScope.launch {
            try {
                val currentProfile = (profileState.value as? ProfileGateState.Ready)?.profile
                val isQrRegisteredStudent = currentProfile?.role == "student" &&
                    (!studentEnrollment.value?.qrToken.isNullOrBlank() || !studentRoster.value?.qrToken.isNullOrBlank())
                val effectiveName = if (isQrRegisteredStudent) {
                    currentProfile?.fullName?.takeIf { it.isNotBlank() } ?: fullName
                } else {
                    fullName
                }
                val cleanEmail = email?.trim()
                repository.updateProfileInfo(
                    fullName = effectiveName,
                    phoneNumber = phone,
                    avatarUrl = avatarUrl,
                    birthDate = birthDate,
                    email = cleanEmail
                )
                uiContext?.let { ctx ->
                    BiometricAuthHelper.updateDisplayNameIfSaved(ctx, currentUserId, effectiveName)
                }
                if (!cleanEmail.isNullOrBlank() &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
                ) {
                    val user = Firebase.auth.currentUser
                    if (user != null && !cleanEmail.equals(user.email, ignoreCase = true)) {
                        try {
                            user.verifyBeforeUpdateEmail(cleanEmail).await()
                            uiContext?.let { ctx ->
                                _statusMessage.value = ctx.getString(
                                    R.string.recovery_email_verification_sent,
                                    cleanEmail
                                )
                            }
                        } catch (_: FirebaseAuthRecentLoginRequiredException) {
                            uiContext?.let { ctx ->
                                _statusMessage.value = ctx.getString(R.string.recovery_email_reauth_hint)
                            }
                        } catch (_: Exception) {
                            // Profile email already saved in Firestore; ignore transient auth verification errors
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun updateProfileAvatar(uiContext: Context, uri: Uri) {
        if (_isSavingAvatar.value) return
        viewModelScope.launch {
            _isSavingAvatar.value = true
            try {
                val dataUri = AvatarStorageHelper.saveAndEncodeAvatar(
                    context = uiContext,
                    userId = currentUserId,
                    sourceUri = uri
                ).getOrThrow()
                _optimisticAvatarUrl.value = dataUri
                repository.updateProfileAvatar(dataUri)
                _statusMessage.value = uiContext.getString(R.string.avatar_updated_success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = uiContext.getString(
                    R.string.avatar_update_error,
                    e.localizedMessage ?: "Error"
                )
            } finally {
                _isSavingAvatar.value = false
            }
        }
    }

    fun approveTeacher(uiContext: Context, teacher: UserProfile) {
        viewModelScope.launch {
            try {
                repository.updateTeacherApprovalStatus(teacher.userId, "approved")
                _statusMessage.value = uiContext.getString(R.string.admin_teacher_approved_msg, teacher.fullName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun rejectTeacher(uiContext: Context, teacher: UserProfile) {
        viewModelScope.launch {
            try {
                repository.updateTeacherApprovalStatus(teacher.userId, "rejected")
                _statusMessage.value = uiContext.getString(R.string.admin_teacher_rejected_msg, teacher.fullName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun sendOwnerAdminVerificationEmail(uiContext: Context) {
        val user = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.sendEmailVerification().await()
                _statusMessage.value = uiContext.getString(R.string.admin_verification_email_sent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun refreshAuthAndProfile(uiContext: Context) {
        val user = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.reload().await()
                user.getIdToken(true).await()
                _authReloadTick.value += 1
                _profileRetryTrigger.value += 1
                _statusMessage.value = uiContext.getString(R.string.teacher_status_refreshed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun acceptTermsOfUse(onComplete: (Boolean) -> Unit = {}) {
        if (_isAcceptingTerms.value) return
        val currentProfile = (profileState.value as? ProfileGateState.Ready)?.profile
        viewModelScope.launch {
            _isAcceptingTerms.value = true
            try {
                repository.acceptTermsOfUse()
                if (currentProfile != null) {
                    _optimisticProfile.value = currentProfile.copy(acceptedTerms = true)
                }
                onComplete(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Failed to accept terms"
                onComplete(false)
            } finally {
                _isAcceptingTerms.value = false
            }
        }
    }

    fun changeUserPassword(
        uiContext: Context,
        currentPassword: String?,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val user = Firebase.auth.currentUser
        if (user == null) {
            onResult(false, uiContext.getString(R.string.auth_error_generic))
            return
        }
        viewModelScope.launch {
            try {
                val userEmail = user.email
                if (!currentPassword.isNullOrBlank() && !userEmail.isNullOrBlank()) {
                    val cred = EmailAuthProvider.getCredential(userEmail, currentPassword)
                    user.reauthenticate(cred).await()
                }
                user.updatePassword(newPassword).await()
                repository.markPasswordChanged(mustChange = false)
                BiometricAuthHelper.updatePasswordIfSaved(uiContext, user.uid, newPassword)
                val okMsg = uiContext.getString(R.string.password_changed_success)
                _statusMessage.value = okMsg
                onResult(true, null)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                val msg = uiContext.getString(R.string.password_reauth_required)
                _actionError.value = msg
                onResult(false, msg)
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                val msg = uiContext.getString(R.string.password_current_incorrect)
                onResult(false, msg)
            } catch (_: FirebaseAuthInvalidUserException) {
                val msg = uiContext.getString(R.string.password_current_incorrect)
                onResult(false, msg)
            } catch (e: Exception) {
                val raw = e.localizedMessage.orEmpty()
                val msg = if (raw.contains("credential", ignoreCase = true) ||
                    raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true)
                ) {
                    uiContext.getString(R.string.password_current_incorrect)
                } else {
                    raw.ifBlank { uiContext.getString(R.string.password_change_error) }
                }
                onResult(false, msg)
            }
        }
    }

    fun forgotPasswordWhileSignedIn(
        uiContext: Context,
        registeredPhone: String,
        recoveryEmail: String = "",
        onResult: (ok: Boolean, provisionalPassword: String?, sentResetEmail: String?, error: String?) -> Unit
    ) {
        val user = Firebase.auth.currentUser
        if (user == null) {
            onResult(false, null, null, uiContext.getString(R.string.auth_error_generic))
            return
        }
        viewModelScope.launch {
            val newProvisional = AuthSessionDraft.generateProvisionalPassword()
            try {
                user.updatePassword(newProvisional).await()
                repository.markPasswordChanged(mustChange = true)
                val phoneLabel = registeredPhone.trim().ifBlank { "-" }
                NotificationHelper.showBadgeOrAlertNotification(
                    context = uiContext,
                    title = uiContext.getString(R.string.password_forgot_provisional_notif_title),
                    body = uiContext.getString(
                        R.string.password_forgot_provisional_notif_body,
                        phoneLabel,
                        newProvisional
                    ),
                    notificationId = 90501
                )
                onResult(true, newProvisional, null, null)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                val authEmail = user.email.orEmpty()
                val targetEmail = when {
                    authEmail.isNotBlank() && !authEmail.endsWith("@student.bioterre.svt", ignoreCase = true) -> authEmail
                    recoveryEmail.trim().isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(recoveryEmail.trim()).matches() -> recoveryEmail.trim()
                    else -> ""
                }
                if (targetEmail.isNotBlank()) {
                    try {
                        Firebase.auth.sendPasswordResetEmail(targetEmail).await()
                        onResult(true, null, targetEmail, null)
                    } catch (mailEx: Exception) {
                        onResult(
                            false,
                            null,
                            null,
                            mailEx.localizedMessage ?: uiContext.getString(R.string.password_change_error)
                        )
                    }
                } else {
                    onResult(false, null, null, uiContext.getString(R.string.password_forgot_reauth_student))
                }
            } catch (e: Exception) {
                onResult(
                    false,
                    null,
                    null,
                    e.localizedMessage ?: uiContext.getString(R.string.password_change_error)
                )
            }
        }
    }

    fun resetStudentProvisionalPassword(
        uiContext: Context,
        rosterStudent: RosterStudent,
        onResult: (ok: Boolean, newProvisional: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            val newProvisional = AuthSessionDraft.generateProvisionalPassword()
            val provisionerAuth = try {
                AuthSessionDraft.getProvisionerAuth(uiContext)
            } catch (e: Exception) {
                onResult(false, null, e.localizedMessage)
                return@launch
            }
            try {
                val cleanMassar = AuthSessionDraft.normalizeMassar(rosterStudent.massarNumber)
                val authEmail = AuthSessionDraft.massarToAuthEmail(cleanMassar)
                val oldPassword = rosterStudent.provisionalPassword
                if (oldPassword.length < 6) {
                    onResult(false, null, uiContext.getString(R.string.teacher_reset_student_already_changed))
                    return@launch
                }
                val signInRes = provisionerAuth.signInWithEmailAndPassword(authEmail, oldPassword).await()
                val studentUser = signInRes.user
                if (studentUser == null) {
                    onResult(false, null, uiContext.getString(R.string.auth_error_generic))
                    return@launch
                }
                studentUser.updatePassword(newProvisional).await()
                repository.updateRosterProvisionalPassword(rosterStudent.id, newProvisional)
                onResult(true, newProvisional, null)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                onResult(false, null, uiContext.getString(R.string.teacher_reset_student_already_changed))
            } catch (e: Exception) {
                onResult(false, null, e.localizedMessage ?: uiContext.getString(R.string.password_change_error))
            } finally {
                runCatching { provisionerAuth.signOut() }
            }
        }
    }

    fun sendAdminTeacherPasswordReset(
        uiContext: Context,
        teacher: UserProfile,
        onPreparedSms: (phone: String, smsBody: String) -> Unit
    ) {
        val cleanEmail = teacher.email.trim()
        if (cleanEmail.isBlank()) {
            _actionError.value = uiContext.getString(R.string.auth_error_email_invalid)
            return
        }
        viewModelScope.launch {
            try {
                Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
                val phone = teacher.phoneNumber.trim()
                if (phone.isNotBlank()) {
                    val smsBody = uiContext.getString(
                        R.string.admin_reset_teacher_sms_body,
                        teacher.fullName,
                        cleanEmail
                    )
                    _statusMessage.value = uiContext.getString(
                        R.string.admin_reset_teacher_password_sent,
                        cleanEmail,
                        phone
                    )
                    onPreparedSms(phone, smsBody)
                } else {
                    _statusMessage.value = uiContext.getString(
                        R.string.password_forgot_email_sent,
                        cleanEmail
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: uiContext.getString(R.string.auth_error_generic)
            }
        }
    }

    fun syncStreakAndBadgesIfNeeded(uiContext: Context) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (profile.role != "student" || streakCheckedThisSession) return
        streakCheckedThisSession = true
        val teacherId = studentEnrollment.value?.teacherId ?: ""
        evaluateAndNotifyBadges(uiContext, profile, studentAttempts.value, studentVideoOnlyTracking.value, teacherId)
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
        massarNumber: String = "",
        onResult: (Boolean) -> Unit
    ) {
        if (_isJoiningClass.value) return
        viewModelScope.launch {
            _isJoiningClass.value = true
            try {
                val cleanToken = qrToken.trim()
                val code = repository.lookupClassByQrToken(cleanToken)
                if (code == null) {
                    _actionError.value = invalidCodeMessage
                    onResult(false)
                } else {
                    val enrollment = repository.requestEnrollment(
                        classCode = code,
                        studentName = studentName,
                        studentPhone = studentPhone,
                        massarNumber = massarNumber,
                        qrToken = cleanToken
                    )
                    _optimisticEnrollment.value = enrollment
                    onResult(true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: invalidCodeMessage
                onResult(false)
            } finally {
                _isJoiningClass.value = false
            }
        }
    }

    fun resetRejectedEnrollment() {
        viewModelScope.launch {
            try {
                _optimisticEnrollment.value = null
                repository.clearOwnEnrollment()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun claimStudentDeviceIfNeeded() {
        val currentProfile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (currentProfile.role != "student" || currentProfile.boundDeviceId.isNotBlank()) return
        viewModelScope.launch {
            deviceBindMutex.withLock {
                if (deviceBindAttempted) return@withLock
                deviceBindAttempted = true
                try {
                    val bound = repository.bindStudentDeviceIfUnbound(currentDeviceId, currentDeviceModel)
                    if (bound) {
                        _optimisticProfile.value = currentProfile.copy(
                            boundDeviceId = currentDeviceId,
                            boundDeviceModel = currentDeviceModel
                        )
                    } else {
                        _profileRetryTrigger.value += 1
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    deviceBindAttempted = false
                    Log.w(TAG, "Device lock bind warning", e)
                }
            }
        }
    }

    fun resetStudentBoundDevice(uiContext: Context, studentId: String) {
        viewModelScope.launch {
            try {
                repository.resetStudentBoundDevice(studentId)
                _statusMessage.value = uiContext.getString(R.string.device_lock_reset_success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun updateClassLabDomains(uiContext: Context, classId: String, domains: List<String>) {
        if (domains.isEmpty()) {
            _actionError.value = uiContext.getString(R.string.teacher_lab_themes_min_one)
            return
        }
        viewModelScope.launch {
            try {
                repository.updateClassLabDomains(classId, domains)
                _statusMessage.value = uiContext.getString(R.string.teacher_lab_themes_updated)
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

    fun approveAllPending(enrollments: List<Enrollment>, notifTitle: String, notifBody: String) {
        if (enrollments.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            try {
                for (enrollment in enrollments) {
                    repository.approveStudentEnrollment(enrollment, notifTitle, notifBody)
                    count++
                }
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

    fun importRosterStudentsToClass(
        uiContext: Context,
        svtClass: SvtClass,
        rows: List<ImportedStudentRow>
    ) {
        if (rows.isEmpty()) return
        viewModelScope.launch {
            _isImportingRoster.value = true
            val provisionerAuth = try {
                AuthSessionDraft.getProvisionerAuth(uiContext)
            } catch (e: Exception) {
                _isImportingRoster.value = false
                _actionError.value = e.localizedMessage
                return@launch
            }
            var savedCount = 0
            try {
                val existingByMassar = teacherRoster.value
                    .filter { it.classId == svtClass.id }
                    .associateBy { AuthSessionDraft.normalizeMassar(it.massarNumber) }

                for (row in rows) {
                    val cleanMassar = AuthSessionDraft.normalizeMassar(row.massarNumber)
                    if (cleanMassar.length < 2 || row.fullName.trim().length < 2) continue

                    val existing = existingByMassar[cleanMassar]
                    val password = existing?.provisionalPassword?.takeIf { it.length >= 6 }
                        ?: AuthSessionDraft.generateProvisionalPassword()
                    val authEmail = AuthSessionDraft.massarToAuthEmail(cleanMassar)

                    var studentUid = existing?.studentUid.orEmpty()
                    if (studentUid.isBlank()) {
                        try {
                            val res = provisionerAuth.createUserWithEmailAndPassword(authEmail, password).await()
                            studentUid = res.user?.uid.orEmpty()
                        } catch (_: FirebaseAuthUserCollisionException) {
                            val signInRes = runCatching {
                                provisionerAuth.signInWithEmailAndPassword(authEmail, password).await()
                            }.getOrNull()
                            studentUid = signInRes?.user?.uid ?: studentUid
                        } catch (authEx: FirebaseAuthException) {
                            if (authEx.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                                authEx.message?.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) == true ||
                                authEx.message?.contains("PASSWORD_LOGIN_DISABLED", ignoreCase = true) == true
                            ) {
                                _actionError.value = uiContext.getString(R.string.auth_error_email_disabled)
                                return@launch
                            }
                            throw authEx
                        }
                    }

                    val rosterItem = RosterStudent(
                        id = "${svtClass.id}_$cleanMassar",
                        classId = svtClass.id,
                        className = svtClass.name,
                        teacherId = currentUserId,
                        teacherName = svtClass.teacherName,
                        qrToken = svtClass.qrToken,
                        fullName = row.fullName.trim(),
                        massarNumber = cleanMassar,
                        provisionalPassword = password,
                        studentUid = studentUid,
                        qrPayload = "BIOTERRE:${svtClass.qrToken}|$cleanMassar"
                    )
                    repository.saveRosterStudent(rosterItem)
                    savedCount++
                }
                if (savedCount > 0) {
                    _statusMessage.value = uiContext.getString(R.string.roster_import_success, savedCount)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: uiContext.getString(R.string.excel_read_error)
            } finally {
                runCatching { provisionerAuth.signOut() }
                _isImportingRoster.value = false
            }
        }
    }

    fun addManualRosterStudent(
        uiContext: Context,
        svtClass: SvtClass,
        fullName: String,
        massarNumber: String
    ) {
        val cleanMassar = AuthSessionDraft.normalizeMassar(massarNumber)
        val cleanName = fullName.trim()
        if (cleanMassar.length < 4 || cleanName.length < 2) {
            _actionError.value = uiContext.getString(R.string.error_massar_required)
            return
        }
        importRosterStudentsToClass(
            uiContext = uiContext,
            svtClass = svtClass,
            rows = listOf(ImportedStudentRow(fullName = cleanName, massarNumber = cleanMassar))
        )
    }

    fun deleteRosterStudent(roster: RosterStudent) {
        viewModelScope.launch {
            try {
                repository.deleteRosterStudent(roster.id)
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
            } catch (e: AiAppCheckException) {
                val rawDetail = e.cause?.localizedMessage ?: e.localizedMessage.orEmpty()
                val diag = appCheckDiagnosticTag()
                val detail = if (rawDetail.isNotBlank()) "$rawDetail • $diag" else diag
                val base = context.getString(R.string.quiz_ai_error_appcheck)
                _aiQuizState.value = AiQuizState.Error("$base\n($detail)")
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
        publishQuizToClasses(
            uiContext = context,
            classIds = listOf(classId),
            title = title,
            description = description,
            chapterSvt = chapterSvt,
            questions = questions,
            notifTitle = notifTitle,
            notifBody = notifBody,
            isDownloadable = isDownloadable
        )
    }

    fun publishQuizToClasses(
        uiContext: Context,
        classIds: List<String>,
        title: String,
        description: String,
        chapterSvt: String,
        questions: List<QuizQuestion>,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ) {
        val targetClassIds = classIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (targetClassIds.isEmpty()) {
            _actionError.value = uiContext.getString(R.string.quiz_min_one_class_error)
            return
        }
        val enrollmentsSnapshot = teacherEnrollments.value
        viewModelScope.launch {
            var publishedCount = 0
            try {
                for (targetClassId in targetClassIds) {
                    val approvedIds = enrollmentsSnapshot
                        .filter { it.classId == targetClassId && it.status == "approved" }
                        .map { it.studentId }
                    repository.createQuizForClass(
                        classId = targetClassId,
                        title = title,
                        description = description,
                        chapterSvt = chapterSvt,
                        questions = questions,
                        approvedStudentIds = approvedIds,
                        notifTitle = notifTitle,
                        notifBody = notifBody,
                        isDownloadable = isDownloadable
                    )
                    publishedCount++
                }
                if (publishedCount > 0) {
                    _statusMessage.value = uiContext.getString(
                        R.string.quiz_published_classes_success,
                        publishedCount
                    )
                }
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

    fun deleteQuiz(uiContext: Context, quiz: Quiz) {
        viewModelScope.launch {
            try {
                repository.deleteQuiz(quiz)
                offlineStorage.removeOfflineQuiz(quiz.id)
                _statusMessage.value = uiContext.getString(R.string.quiz_deleted_success)
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
                    trackings = studentVideoOnlyTracking.value,
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
                                existingTracking = existing,
                                watchedPercentOverride = tracking.watchedPercentageOverride.takeIf { it > 0.0 }
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
                                trackings = studentVideoOnlyTracking.value,
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
        uiContext: Context,
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
        val trimmedUrl = fileUrl.trim()
        val isLocalPickedUri = trimmedUrl.startsWith("content://", ignoreCase = true) ||
            trimmedUrl.startsWith("file://", ignoreCase = true)

        viewModelScope.launch {
            if (isLocalPickedUri) {
                _isUploadingResource.value = true
            }
            try {
                if (isLocalPickedUri) {
                    repository.uploadChunkedResource(
                        appContext = uiContext.applicationContext,
                        classId = classId,
                        title = title,
                        description = description,
                        type = type,
                        contentUriString = trimmedUrl,
                        durationSeconds = durationSeconds,
                        approvedStudentIds = approvedIds,
                        notifTitle = notifTitle,
                        notifBody = notifBody,
                        isDownloadable = isDownloadable
                    )
                    _statusMessage.value = uiContext.getString(R.string.resource_upload_success, title.trim())
                } else {
                    val normalized = OfflineStorageManager.normalizeDownloadUrl(trimmedUrl)
                    repository.createResource(
                        classId = classId,
                        title = title,
                        description = description,
                        type = type,
                        fileUrl = normalized,
                        durationSeconds = durationSeconds,
                        approvedStudentIds = approvedIds,
                        notifTitle = notifTitle,
                        notifBody = notifBody,
                        isDownloadable = isDownloadable
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ResourceUploadTooLargeException) {
                val maxMb = (e.maxBytes / (1024L * 1024L)).toInt()
                _actionError.value = uiContext.getString(R.string.resource_too_large_error, maxMb)
            } catch (_: ResourceReadException) {
                _actionError.value = uiContext.getString(R.string.resource_read_error)
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: uiContext.getString(R.string.resource_read_error)
            } finally {
                _isUploadingResource.value = false
            }
        }
    }

    fun deleteResource(resource: CourseResource) {
        viewModelScope.launch {
            try {
                repository.deleteResource(resource)
                offlineStorage.removeDownloadedResource(resource.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun prepareResourceUriForViewing(
        uiContext: Context,
        resource: CourseResource,
        currentUri: String,
        onReady: (String) -> Unit
    ) {
        val scheme = Uri.parse(currentUri).scheme?.lowercase()
        val shouldCacheLocally = scheme == OfflineStorageManager.FIRESTORE_CHUNK_SCHEME ||
            scheme == "content" ||
            (resource.type in listOf("pdf", "word", "image") && (scheme == "http" || scheme == "https"))

        if (!shouldCacheLocally) {
            onReady(currentUri)
            return
        }

        viewModelScope.launch {
            try {
                val cachedFile = offlineStorage.ensureLocalCacheForViewing(resource)
                onReady(Uri.fromFile(cachedFile).toString())
            } catch (e: CancellationException) {
                throw e
            } catch (_: LocalUriUnavailableException) {
                _actionError.value = uiContext.getString(R.string.resource_legacy_local_uri_error)
            } catch (e: Exception) {
                _actionError.value = uiContext.getString(
                    R.string.offline_download_error,
                    e.localizedMessage ?: "Error"
                )
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
                val record = offlineStorage.downloadResource(resource, exportToPublicDownloads = true)
                val sizeLabel = OfflineStorageManager.formatBytesReadable(record.fileSizeBytes)
                _statusMessage.value = if (record.publicDisplayPath.isNotBlank()) {
                    uiContext.getString(R.string.offline_download_saved_device, resource.title, sizeLabel)
                } else {
                    uiContext.getString(R.string.offline_download_success, resource.title)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: LocalUriUnavailableException) {
                _actionError.value = uiContext.getString(R.string.resource_legacy_local_uri_error)
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

    fun recordFileOpened(
        uiContext: Context,
        resource: CourseResource
    ) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (profile.role != "student" || resource.id.isBlank()) return

        val existing = studentVideoTracking.value.firstOrNull { it.resourceId == resource.id }
        if (existing != null && existing.watchedPercentage >= 100.0) return

        viewModelScope.launch {
            if (!isOnline.value) {
                offlineStorage.enqueuePendingVideoTracking(
                    PendingVideoTracking(
                        resource = resource,
                        studentName = profile.fullName,
                        maxPositionSeconds = 1L,
                        totalDurationSeconds = 1L,
                        updatedAtMillis = System.currentTimeMillis(),
                        watchedPercentageOverride = 100.0
                    )
                )
                return@launch
            }
            try {
                repository.recordFileOpenedTracking(
                    resource = resource,
                    studentName = profile.fullName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "File open tracking offline fallback", e)
                offlineStorage.enqueuePendingVideoTracking(
                    PendingVideoTracking(
                        resource = resource,
                        studentName = profile.fullName,
                        maxPositionSeconds = 1L,
                        totalDurationSeconds = 1L,
                        updatedAtMillis = System.currentTimeMillis(),
                        watchedPercentageOverride = 100.0
                    )
                )
            }
        }
    }

    fun recordVideoProgress(
        uiContext: Context,
        resource: CourseResource,
        positionSeconds: Long,
        durationSeconds: Long,
        watchedPercentOverride: Double? = null
    ) {
        val profile = (profileState.value as? ProfileGateState.Ready)?.profile ?: return
        if (profile.role != "student") return
        val hasProgress = positionSeconds > 0L || (watchedPercentOverride != null && watchedPercentOverride > 0.0)
        if (!hasProgress) return

        val existing = studentVideoTracking.value.firstOrNull { it.resourceId == resource.id }
        val safeOverride = watchedPercentOverride?.coerceIn(0.0, 100.0) ?: 0.0
        viewModelScope.launch {
            if (!isOnline.value) {
                offlineStorage.enqueuePendingVideoTracking(
                    PendingVideoTracking(
                        resource = resource,
                        studentName = profile.fullName,
                        maxPositionSeconds = positionSeconds,
                        totalDurationSeconds = durationSeconds,
                        updatedAtMillis = System.currentTimeMillis(),
                        watchedPercentageOverride = safeOverride
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
                    existingTracking = existing,
                    watchedPercentOverride = watchedPercentOverride
                )
                val mergedTrackings = filterVideoOnlyTrackings(
                    studentVideoTracking.value.filterNot { it.resourceId == resource.id } + updated,
                    studentResources.value
                )
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
                        updatedAtMillis = System.currentTimeMillis(),
                        watchedPercentageOverride = safeOverride
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

    fun deleteAnnouncement(uiContext: Context, announcement: Announcement) {
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(announcement)
                _statusMessage.value = uiContext.getString(R.string.announcement_deleted_success)
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

    fun publishAppUpdate(
        uiContext: Context,
        classId: String,
        teacherName: String,
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String
    ) {
        publishAppUpdateToClasses(
            uiContext = uiContext,
            classIds = listOf(classId),
            teacherName = teacherName,
            versionCode = versionCode,
            versionName = versionName,
            releaseNotes = releaseNotes,
            downloadUrl = downloadUrl
        )
    }

    fun publishAppUpdateToClasses(
        uiContext: Context,
        classIds: List<String>,
        teacherName: String,
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String
    ) {
        val targetClassIds = classIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (targetClassIds.isEmpty()) {
            _actionError.value = uiContext.getString(R.string.quiz_min_one_class_error)
            return
        }
        if (AppUpdateUrlHelper.isGoogleDriveFolderUrl(downloadUrl)) {
            _actionError.value = uiContext.getString(R.string.app_update_drive_folder_error)
            return
        }
        val cleanUrl = AppUpdateUrlHelper.normalizeDownloadUrl(downloadUrl)
        if (!cleanUrl.startsWith("https://", ignoreCase = true) || cleanUrl.length < 10) {
            _actionError.value = uiContext.getString(R.string.app_update_invalid_url)
            return
        }
        if (versionCode < 1 || versionName.trim().isEmpty()) {
            _actionError.value = uiContext.getString(R.string.app_update_invalid_version)
            return
        }
        val enrollmentsSnapshot = teacherEnrollments.value
        val cleanVersion = versionName.trim()
        val notifTitle = uiContext.getString(R.string.app_update_notif_title, cleanVersion)
        val notifBody = releaseNotes.trim().ifBlank {
            uiContext.getString(R.string.app_update_available_title, cleanVersion, versionCode)
        }
        viewModelScope.launch {
            var publishedCount = 0
            try {
                for (targetClassId in targetClassIds) {
                    val approvedIds = enrollmentsSnapshot
                        .filter { it.classId == targetClassId && it.status == "approved" }
                        .map { it.studentId }
                    repository.publishAppUpdate(
                        classId = targetClassId,
                        teacherName = teacherName,
                        versionCode = versionCode,
                        versionName = cleanVersion,
                        releaseNotes = releaseNotes,
                        downloadUrl = cleanUrl,
                        approvedStudentIds = approvedIds,
                        notifTitle = notifTitle,
                        notifBody = notifBody
                    )
                    publishedCount++
                }
                if (publishedCount > 0) {
                    _statusMessage.value = if (publishedCount == 1) {
                        uiContext.getString(R.string.app_update_published_success, cleanVersion)
                    } else {
                        uiContext.getString(R.string.app_update_published_classes_success, cleanVersion, publishedCount)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    fun deleteAppUpdate(updateId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAppUpdate(updateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage
            }
        }
    }

    private fun buildDeterministicAttemptId(quizId: String, studentId: String): String {
        val raw = "att_${quizId}_$studentId"
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120)
    }

    companion object {
        private const val TAG = "SvtMainViewModel"

        fun filterVideoOnlyTrackings(
            trackings: List<VideoTracking>,
            resources: List<CourseResource>
        ): List<VideoTracking> {
            if (resources.isEmpty()) return trackings
            val nonVideoIds = resources.filter { it.type != "video" }.map { it.id }.toSet()
            return trackings.filterNot { it.resourceId in nonVideoIds }
        }
    }
}
