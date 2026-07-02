package com.holadeutsch.app

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
        val session = engine.buildSession(words, emptyMap(), 10)
        assertEquals(10, session.size)
        assertEquals(10, session.map { it.word.id }.distinct().size)
    }

    @Test
    fun `multiple choice has four unique options including the answer`() {
        repeat(50) { seed ->
            QuizEngine(Random(seed)).buildSession(words, emptyMap(), 10)
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
            QuizEngine(Random(seed)).buildSession(words, emptyMap(), 10)
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
    fun `weak words are favored by weighted picking`() {
        // Word 1 is in box 1, everything else mastered (box 5).
        val boxes = words.associate { it.id to if (it.id == 1) 1 else 5 }
        val hits = (0 until 100).count { seed ->
            QuizEngine(Random(seed)).buildSession(words, boxes, 5).any { it.word.id == 1 }
        }
        assertTrue("expected weighted picking to favor the weak word, hits=$hits", hits > 60)
    }

    private fun word(id: Int, german: String, spanish: String, article: String?, category: Category) =
        Word(
            id = id,
            german = german,
            spanish = spanish,
            article = article,
            pos = "test",
            exampleDe = "Beispiel.",
            exampleEs = "Ejemplo.",
            category = category,
            level = "A0"
        )
}
