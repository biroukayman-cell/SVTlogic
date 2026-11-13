package com.example.data.badges

import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.QuizAttempt
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking

enum class BadgeCategory(@StringRes val labelRes: Int) {
    PERFORMANCE(R.string.badges_cat_performance),
    REGULARITY(R.string.badges_cat_regularity),
    ATTENDANCE(R.string.badges_cat_attendance)
}

data class SvtBadgeDefinition(
    val id: String,
    val category: BadgeCategory,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val xpReward: Long,
    val iconKey: String
)

data class BadgeProgressItem(
    val definition: SvtBadgeDefinition,
    val isUnlocked: Boolean,
    val progressFraction: Float, // 0f..1f
    val progressLabel: String
)

data class StudentLevelInfo(
    val level: Int,
    @StringRes val titleRes: Int,
    val currentXp: Long,
    val nextLevelXp: Long,
    val progressToNext: Float
)

object BadgeEngine {
    val allBadges: List<SvtBadgeDefinition> = listOf(
        // PERFORMANCE (Notes & Temps)
        SvtBadgeDefinition(
            id = "badge_perf_first",
            category = BadgeCategory.PERFORMANCE,
            titleRes = R.string.badge_perf_first_title,
            descriptionRes = R.string.badge_perf_first_desc,
            xpReward = 50L,
            iconKey = "science"
        ),
        SvtBadgeDefinition(
            id = "badge_perf_elite",
            category = BadgeCategory.PERFORMANCE,
            titleRes = R.string.badge_perf_elite_title,
            descriptionRes = R.string.badge_perf_elite_desc,
            xpReward = 120L,
            iconKey = "biotech"
        ),
        SvtBadgeDefinition(
            id = "badge_perf_perfect",
            category = BadgeCategory.PERFORMANCE,
            titleRes = R.string.badge_perf_perfect_title,
            descriptionRes = R.string.badge_perf_perfect_desc,
            xpReward = 200L,
            iconKey = "star"
        ),
        SvtBadgeDefinition(
            id = "badge_perf_speed",
            category = BadgeCategory.PERFORMANCE,
            titleRes = R.string.badge_perf_speed_title,
            descriptionRes = R.string.badge_perf_speed_desc,
            xpReward = 150L,
            iconKey = "bolt"
        ),

        // RÉGULARITÉ (Connexions & Exercices complétés)
        SvtBadgeDefinition(
            id = "badge_reg_streak3",
            category = BadgeCategory.REGULARITY,
            titleRes = R.string.badge_reg_streak3_title,
            descriptionRes = R.string.badge_reg_streak3_desc,
            xpReward = 100L,
            iconKey = "local_fire"
        ),
        SvtBadgeDefinition(
            id = "badge_reg_streak7",
            category = BadgeCategory.REGULARITY,
            titleRes = R.string.badge_reg_streak7_title,
            descriptionRes = R.string.badge_reg_streak7_desc,
            xpReward = 250L,
            iconKey = "workspace_premium"
        ),
        SvtBadgeDefinition(
            id = "badge_reg_quiz3",
            category = BadgeCategory.REGULARITY,
            titleRes = R.string.badge_reg_quiz3_title,
            descriptionRes = R.string.badge_reg_quiz3_desc,
            xpReward = 120L,
            iconKey = "menu_book"
        ),
        SvtBadgeDefinition(
            id = "badge_reg_quiz5",
            category = BadgeCategory.REGULARITY,
            titleRes = R.string.badge_reg_quiz5_title,
            descriptionRes = R.string.badge_reg_quiz5_desc,
            xpReward = 220L,
            iconKey = "school"
        ),

        // ASSIDUITÉ (Visionnage de vidéos)
        SvtBadgeDefinition(
            id = "badge_vid_first",
            category = BadgeCategory.ATTENDANCE,
            titleRes = R.string.badge_vid_first_title,
            descriptionRes = R.string.badge_vid_first_desc,
            xpReward = 80L,
            iconKey = "play_circle"
        ),
        SvtBadgeDefinition(
            id = "badge_vid_three",
            category = BadgeCategory.ATTENDANCE,
            titleRes = R.string.badge_vid_three_title,
            descriptionRes = R.string.badge_vid_three_desc,
            xpReward = 180L,
            iconKey = "video_library"
        )
    )

    fun evaluateUnlockedBadges(
        profile: UserProfile,
        attempts: List<QuizAttempt>,
        videoTrackings: List<VideoTracking>
    ): List<SvtBadgeDefinition> {
        val alreadyUnlocked = profile.unlockedBadgeIds.toSet()
        val distinctQuizCount = attempts.map { it.quizId }.distinct().size
        val bestPercent = attempts.maxOfOrNull { attempt ->
            if (attempt.maxScore > 0.0) (attempt.score / attempt.maxScore) * 100.0 else 0.0
        } ?: 0.0
        val hasFastGoodAttempt = attempts.any { attempt ->
            val pct = if (attempt.maxScore > 0.0) (attempt.score / attempt.maxScore) * 100.0 else 0.0
            pct >= 80.0 && attempt.totalDurationSeconds in 1..60
        }
        val completedVideosCount = videoTrackings.count { it.status == "completed" || it.watchedPercentage >= 90.0 }
        val streak = profile.loginStreak

        return allBadges.filter { badge ->
            if (badge.id in alreadyUnlocked) return@filter false
            when (badge.id) {
                "badge_perf_first" -> distinctQuizCount >= 1
                "badge_perf_elite" -> bestPercent >= 85.0
                "badge_perf_perfect" -> bestPercent >= 99.9
                "badge_perf_speed" -> hasFastGoodAttempt
                "badge_reg_streak3" -> streak >= 3L
                "badge_reg_streak7" -> streak >= 7L
                "badge_reg_quiz3" -> distinctQuizCount >= 3
                "badge_reg_quiz5" -> distinctQuizCount >= 5
                "badge_vid_first" -> completedVideosCount >= 1
                "badge_vid_three" -> completedVideosCount >= 3
                else -> false
            }
        }
    }

    fun computeProgressItems(
        profile: UserProfile,
        attempts: List<QuizAttempt>,
        videoTrackings: List<VideoTracking>
    ): List<BadgeProgressItem> {
        val unlocked = profile.unlockedBadgeIds.toSet()
        val distinctQuizCount = attempts.map { it.quizId }.distinct().size
        val bestPercent = attempts.maxOfOrNull { attempt ->
            if (attempt.maxScore > 0.0) (attempt.score / attempt.maxScore) * 100.0 else 0.0
        } ?: 0.0
        val hasFastGoodAttempt = attempts.any { attempt ->
            val pct = if (attempt.maxScore > 0.0) (attempt.score / attempt.maxScore) * 100.0 else 0.0
            pct >= 80.0 && attempt.totalDurationSeconds in 1..60
        }
        val completedVideosCount = videoTrackings.count { it.status == "completed" || it.watchedPercentage >= 90.0 }
        val streak = profile.loginStreak.toInt()

        return allBadges.map { def ->
            val isUnlocked = def.id in unlocked
            val (fraction, label) = when (def.id) {
                "badge_perf_first" -> {
                    val v = distinctQuizCount.coerceAtMost(1)
                    (v / 1f) to "$v/1"
                }
                "badge_perf_elite" -> {
                    val pct = bestPercent.toInt().coerceIn(0, 85)
                    (pct / 85f) to "${bestPercent.toInt()}%/85%"
                }
                "badge_perf_perfect" -> {
                    val pct = bestPercent.toInt().coerceIn(0, 100)
                    (pct / 100f) to "${bestPercent.toInt()}%/100%"
                }
                "badge_perf_speed" -> {
                    val v = if (isUnlocked || hasFastGoodAttempt) 1 else 0
                    (v / 1f) to "$v/1"
                }
                "badge_reg_streak3" -> {
                    val s = streak.coerceIn(0, 3)
                    (s / 3f) to "$s/3"
                }
                "badge_reg_streak7" -> {
                    val s = streak.coerceIn(0, 7)
                    (s / 7f) to "$s/7"
                }
                "badge_reg_quiz3" -> {
                    val q = distinctQuizCount.coerceIn(0, 3)
                    (q / 3f) to "$q/3"
                }
                "badge_reg_quiz5" -> {
                    val q = distinctQuizCount.coerceIn(0, 5)
                    (q / 5f) to "$q/5"
                }
                "badge_vid_first" -> {
                    val v = completedVideosCount.coerceIn(0, 1)
                    (v / 1f) to "$v/1"
                }
                "badge_vid_three" -> {
                    val v = completedVideosCount.coerceIn(0, 3)
                    (v / 3f) to "$v/3"
                }
                else -> 0f to "0/1"
            }
            BadgeProgressItem(
                definition = def,
                isUnlocked = isUnlocked,
                progressFraction = if (isUnlocked) 1f else fraction.coerceIn(0f, 1f),
                progressLabel = label
            )
        }
    }

    fun computeLevelInfo(xp: Long): StudentLevelInfo {
        return when {
            xp < 150L -> StudentLevelInfo(1, R.string.level_title_1, xp, 150L, (xp / 150f).coerceIn(0f, 1f))
            xp < 400L -> StudentLevelInfo(2, R.string.level_title_2, xp, 400L, ((xp - 150f) / 250f).coerceIn(0f, 1f))
            xp < 750L -> StudentLevelInfo(3, R.string.level_title_3, xp, 750L, ((xp - 400f) / 350f).coerceIn(0f, 1f))
            xp < 1200L -> StudentLevelInfo(4, R.string.level_title_4, xp, 1200L, ((xp - 750f) / 450f).coerceIn(0f, 1f))
            else -> StudentLevelInfo(5, R.string.level_title_5, xp, 1500L, 1f)
        }
    }
}
