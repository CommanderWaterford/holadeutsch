package com.holadeutsch.app.domain

import com.holadeutsch.app.data.local.ProgressEntity
import com.holadeutsch.app.data.model.Word
import java.text.Normalizer
import kotlin.random.Random

enum class Direction { DE_TO_ES, ES_TO_DE }

enum class AnswerResult { CORRECT, PARTIAL, WRONG }

sealed interface Question {
    val word: Word

    data class MultipleChoice(
        override val word: Word,
        val direction: Direction,
        val options: List<String>,
        val correctIndex: Int
    ) : Question

    data class ArticleChoice(override val word: Word) : Question {
        val options: List<String> = listOf("der", "die", "das")
        val correctIndex: Int = options.indexOf(word.article)
    }

    data class Typed(override val word: Word) : Question
}

/**
 * Pure question generator + answer checker. Word selection follows SuperMemo-2:
 * words due for review come first, then unseen words in curriculum order, then
 * the words whose review is closest. Distractors prefer same-category words.
 */
class QuizEngine(private val random: Random = Random.Default) {

    fun buildSession(
        words: List<Word>,
        progress: Map<Int, ProgressEntity>,
        today: Long,
        size: Int = 10,
        distractorPool: List<Word> = words
    ): List<Question> {
        val due = words
            .filter { progress[it.id]?.let { p -> p.repetitions > 0 && p.dueEpochDay <= today } == true }
            .shuffled(random)
        val fresh = words
            .filter { (progress[it.id]?.repetitions ?: 0) == 0 }
            .sortedBy { it.id }
        val upcoming = words
            .filter { progress[it.id]?.let { p -> p.repetitions > 0 && p.dueEpochDay > today } == true }
            .sortedBy { progress[it.id]?.dueEpochDay ?: Long.MAX_VALUE }
        val picked = (due + fresh + upcoming).take(minOf(size, words.size))
        return picked.map { toQuestion(it, distractorPool) }
    }

    private fun toQuestion(word: Word, all: List<Word>): Question {
        val roll = random.nextInt(100)
        return when {
            word.isNoun && roll < 20 -> Question.ArticleChoice(word)
            roll < 40 -> Question.Typed(word)
            roll < 70 -> multipleChoice(word, all, Direction.DE_TO_ES)
            else -> multipleChoice(word, all, Direction.ES_TO_DE)
        }
    }

    private fun multipleChoice(word: Word, all: List<Word>, direction: Direction): Question.MultipleChoice {
        fun label(w: Word) = if (direction == Direction.DE_TO_ES) w.spanish else w.german
        val correct = label(word)
        val candidates = all.filter { it.id != word.id }
        val distractors = (
            candidates.filter { it.category == word.category }.shuffled(random) +
                candidates.shuffled(random)
            )
            .map(::label)
            .distinct()
            .filter { it != correct }
            .take(3)
        val options = (distractors + correct).shuffled(random)
        return Question.MultipleChoice(word, direction, options, options.indexOf(correct))
    }

    /**
     * Checks a typed German answer. Case/diacritic-insensitive, the article is optional
     * and one typo (Levenshtein distance 1) still earns partial credit.
     */
    fun checkTyped(word: Word, input: String): AnswerResult {
        val given = normalize(input)
        if (given.isEmpty()) return AnswerResult.WRONG
        val targets = listOf(normalize(word.germanBare), normalize(word.german))
        if (targets.any { it == given }) return AnswerResult.CORRECT
        if (targets.any { levenshtein(it, given) <= 1 }) return AnswerResult.PARTIAL
        return AnswerResult.WRONG
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("ß", "ss")

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
