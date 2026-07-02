package com.holadeutsch.app.data.model

import kotlinx.serialization.Serializable

/** Thematic groups for the 100-word deck. Labels are shown to the (Spanish-speaking) user. */
@Serializable
enum class Category(val labelEs: String) {
    GREETINGS("Saludos y cortesía"),
    PEOPLE("Personas y familia"),
    PRONOUNS("Pronombres"),
    QUESTIONS("Preguntas"),
    NUMBERS("Números"),
    VERBS("Verbos"),
    ADJECTIVES("Adjetivos"),
    COLORS("Colores"),
    TIME("Tiempo y días"),
    FOOD("Comida y bebida"),
    PLACES("Lugares"),
    CONNECTORS("Palabras útiles")
}

@Serializable
data class Word(
    val id: Int,
    val german: String,
    val spanish: String,
    val article: String? = null,
    val pos: String,
    val exampleDe: String,
    val exampleEs: String,
    val category: Category,
    val level: String
) {
    val isNoun: Boolean get() = article != null

    /** German word without its leading article ("das Haus" -> "Haus"). */
    val germanBare: String get() = article?.let { german.removePrefix("$it ") } ?: german
}
