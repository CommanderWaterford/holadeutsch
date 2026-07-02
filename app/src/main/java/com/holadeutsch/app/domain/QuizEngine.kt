package com.holadeutsch.app.domain

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
 * Pure question generator + answer checker. Weakest words (lowest Leitner box)
 * are picked more often; distractors prefer same-category words.
 */
class QuizEngine(private val random: Random = Random.Default) {

    fun buildSession(words: List<Word>, boxes: Map<Int, Int>, size: Int = 10): List<Question> {
        val picked = pickWeighted(words, boxes, minOf(size, words.size))
        return picked.map { toQuestion(it, words) }
    }

    private fun pickWeighted(words: List<Word>, boxes: Map<Int, Int>, count: Int): List<Word> {
        val pool = words.toMutableList()
        val result = mutableListOf<Word>()
        repeat(count) {
            val weights = pool.map { 6 - (boxes[it.id] ?: 1).coerceIn(1, 5) }
            var r = random.nextInt(weights.sum())
            var idx = 0
            while (r >= weights[idx]) {
                r -= weights[idx]
                idx++
            }
            result += pool.removeAt(idx)
        }
        return result
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
