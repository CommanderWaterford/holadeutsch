package com.holadeutsch.app

import com.holadeutsch.app.data.local.ProgressEntity
import com.holadeutsch.app.data.model.Category
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.domain.AnswerResult
import com.holadeutsch.app.domain.Direction
import com.holadeutsch.app.domain.Question
import com.holadeutsch.app.domain.QuizEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizEngineTest {

    private val words = listOf(
        word(1, "das Haus", "la casa", "das", Category.PLACES),
        word(2, "die Stadt", "la ciudad", "die", Category.PLACES),
        word(3, "der Mann", "el hombre", "der", Category.PEOPLE),
        word(4, "gehen", "ir", null, Category.VERBS),
        word(5, "gut", "bueno", null, Category.ADJECTIVES),
        word(6, "weiß", "blanco", null, Category.COLORS),
        word(7, "fünf", "cinco", null, Category.NUMBERS),
        word(8, "haben", "tener", null, Category.VERBS),
        word(9, "und", "y", null, Category.CONNECTORS),
        word(10, "rot", "rojo", null, Category.COLORS),
        word(11, "blau", "azul", null, Category.COLORS),
        word(12, "kommen", "venir", null, Category.VERBS)
    )

    private val engine = QuizEngine(Random(42))

    @Test
    fun `session has requested size and unique words`() {
        val session = engine.buildSession(words, emptyMap(), today = 100, size = 10)
        assertEquals(10, session.size)
        assertEquals(10, session.map { it.word.id }.distinct().size)
    }

    @Test
    fun `multiple choice has four unique options including the answer`() {
        repeat(50) { seed ->
            QuizEngine(Random(seed)).buildSession(words, emptyMap(), today = 100, size = 10)
                .filterIsInstance<Question.MultipleChoice>()
                .forEach { q ->
                    assertEquals(4, q.options.size)
                    assertEquals(4, q.options.distinct().size)
                    val expected =
                        if (q.direction == Direction.DE_TO_ES) q.word.spanish else q.word.german
                    assertEquals(expected, q.options[q.correctIndex])
                }
        }
    }

    @Test
    fun `article questions only appear for nouns`() {
        repeat(50) { seed ->
            QuizEngine(Random(seed)).buildSession(words, emptyMap(), today = 100, size = 10)
                .filterIsInstance<Question.ArticleChoice>()
                .forEach { assertNotNull(it.word.article) }
        }
    }

    @Test
    fun `typed answers accept case, article and diacritic variants`() {
        val haus = words[0]
        assertEquals(AnswerResult.CORRECT, engine.checkTyped(haus, "Haus"))
        assertEquals(AnswerResult.CORRECT, engine.checkTyped(haus, "das haus"))
        assertEquals(AnswerResult.PARTIAL, engine.checkTyped(haus, "Hauss"))
        assertEquals(AnswerResult.WRONG, engine.checkTyped(haus, "Auto"))
        assertEquals(AnswerResult.WRONG, engine.checkTyped(haus, ""))

        val weiss = words[5]
        assertEquals(AnswerResult.CORRECT, engine.checkTyped(weiss, "weiss"))

        val fuenf = words[6]
        assertEquals(AnswerResult.CORRECT, engine.checkTyped(fuenf, "funf"))
    }

    @Test
    fun `due words are served before new and upcoming words`() {
        val today = 100L
        // Word 1 is overdue, words 2-3 reviewed and not yet due, the rest are new.
        val progress = mapOf(
            1 to ProgressEntity(wordId = 1, repetitions = 2, intervalDays = 6, dueEpochDay = 99),
            2 to ProgressEntity(wordId = 2, repetitions = 2, intervalDays = 6, dueEpochDay = 104),
            3 to ProgressEntity(wordId = 3, repetitions = 1, intervalDays = 1, dueEpochDay = 101)
        )
        repeat(20) { seed ->
            val session = QuizEngine(Random(seed)).buildSession(words, progress, today, 10)
            // The overdue word always leads the session.
            assertEquals(1, session.first().word.id)
            // New words fill the rest before not-yet-due reviewed words are recycled.
            val ids = session.map { it.word.id }
            assertTrue("not-yet-due words should come last", 2 !in ids.take(9) && 3 !in ids.take(9))
        }
    }

    @Test
    fun `new words are introduced easiest first`() {
        val mixed = listOf(
            word(1, "die Möglichkeit", "la posibilidad", "die", Category.ABSTRACT, level = "A1"),
            word(2, "ja", "sí", null, Category.GREETINGS, level = "A0"),
            word(3, "das Wasser", "el agua", "das", Category.FOOD, level = "A1"),
            word(4, "danke", "gracias", null, Category.GREETINGS, level = "A0")
        )
        repeat(20) { seed ->
            val session = QuizEngine(Random(seed)).buildSession(mixed, emptyMap(), today = 100, size = 4)
            // A0 before A1, shorter words first within the same level.
            assertEquals(listOf(2, 4, 3, 1), session.map { it.word.id })
        }
    }

    @Test
    fun `hint reveals a few letters in place and never the whole word`() {
        val haus = words[0] // "das Haus" -> answer "Haus"
        repeat(20) { seed ->
            val hint = QuizEngine(Random(seed)).buildHint(haus).split(" ")
            assertEquals(4, hint.size)
            // 4-letter word: exactly 2 letters revealed, at their original positions.
            assertEquals(2, hint.count { it != "_" })
            hint.forEachIndexed { i, part ->
                if (part != "_") assertEquals("Haus"[i].toString(), part)
            }
        }
        // Two-letter words keep at least one letter hidden.
        val ja = word(99, "ja", "sí", null, Category.GREETINGS)
        repeat(20) { seed ->
            assertEquals(1, QuizEngine(Random(seed)).buildHint(ja).split(" ").count { it == "_" })
        }
    }

    @Test
    fun `sentence builder shuffles tokens and preserves the correct order`() {
        val sentenceWord = words[0].copy(
            exampleDe = "Das Haus ist sehr groß.",
            exampleEs = "La casa es muy grande."
        )
        repeat(20) { seed ->
            val quizEngine = QuizEngine(Random(seed))
            val question = requireNotNull(quizEngine.buildSentenceQuestion(sentenceWord))
            assertEquals(
                listOf("Das", "Haus", "ist", "sehr", "groß."),
                question.tokens.map { it.text }
            )
            assertTrue(
                "word bank must not begin already solved",
                question.shuffledTokens.map { it.id } != question.tokens.map { it.id }
            )
            assertEquals(
                question.tokens.map { it.id }.sorted(),
                question.shuffledTokens.map { it.id }.sorted()
            )
        }
    }

    @Test
    fun `sentence builder gives full credit first try and partial after an error`() {
        val quizEngine = QuizEngine(Random(1))
        val question = requireNotNull(
            quizEngine.buildSentenceQuestion(words[0].copy(exampleDe = "Das ist gut."))
        )
        val correctOrder = question.tokens.map { it.id }

        assertEquals(
            AnswerResult.WRONG,
            quizEngine.checkSentence(question, correctOrder.reversed(), hadPreviousError = false)
        )
        assertEquals(
            AnswerResult.CORRECT,
            quizEngine.checkSentence(question, correctOrder, hadPreviousError = false)
        )
        assertEquals(
            AnswerResult.PARTIAL,
            quizEngine.checkSentence(question, correctOrder, hadPreviousError = true)
        )
    }

    private fun word(
        id: Int,
        german: String,
        spanish: String,
        article: String?,
        category: Category,
        level: String = "A0"
    ) =
        Word(
            id = id,
            german = german,
            spanish = spanish,
            article = article,
            pos = "test",
            exampleDe = "Beispiel.",
            exampleEs = "Ejemplo.",
            category = category,
            level = level
        )
}
