package com.holadeutsch.app.domain

/** One answer recorded for authoritative daily-progress and XP calculation. */
data class SessionAnswer(val wordId: Int, val result: AnswerResult)

data class SessionReward(
    val practicedWordIds: Set<Int>,
    val newlyRewardedWordIds: Set<Int>,
    val answerXp: Int,
    val correctCount: Int,
    val perfect: Boolean
)

/** Pure reward rules, separated from persistence so repeated-word behavior is testable. */
object RewardPolicy {

    fun evaluate(
        answers: List<SessionAnswer>,
        alreadyRewardedWordIds: Set<Int>
    ): SessionReward {
        val practiced = answers.mapTo(mutableSetOf()) { it.wordId }
        val bestSuccessfulAnswerByWord = answers
            .filter { it.result != AnswerResult.WRONG }
            .groupBy { it.wordId }
            .mapValues { (_, attempts) ->
                attempts.maxOf { answerXp(it.result) }
            }
        val newlyRewarded = bestSuccessfulAnswerByWord.keys - alreadyRewardedWordIds
        return SessionReward(
            practicedWordIds = practiced,
            newlyRewardedWordIds = newlyRewarded,
            answerXp = newlyRewarded.sumOf { bestSuccessfulAnswerByWord.getValue(it) },
            correctCount = answers.count { it.result != AnswerResult.WRONG },
            perfect = answers.isNotEmpty() && answers.all { it.result != AnswerResult.WRONG }
        )
    }

    fun answerXp(result: AnswerResult): Int = when (result) {
        AnswerResult.CORRECT -> 10
        AnswerResult.PARTIAL -> 5
        AnswerResult.WRONG -> 0
    }
}
