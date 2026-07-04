package com.holadeutsch.app.data.model

import kotlinx.serialization.Serializable

/** Thematic groups for the word decks. Labels are shown to the (Spanish-speaking) user. */
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
    CONNECTORS("Palabras útiles"),
    BODY("Cuerpo humano"),
    CLOTHING("Ropa"),
    ANIMALS("Animales"),
    NATURE("Naturaleza"),
    WEATHER("El clima"),
    HOUSE("Casa y hogar"),
    TRAVEL("Viajes y transporte"),
    WORK("Trabajo y estudios"),
    FEELINGS("Emociones"),
    SHOPPING("Compras y dinero"),
    HEALTH("Salud"),
    FREETIME("Ocio y deporte"),
    SOCIETY("Sociedad y medios"),
    ABSTRACT("Conceptos")
}

/** Learning levels the user can choose; intermediates can skip ahead. */
enum class Nivel(val number: Int, val cefr: String, val labelEs: String) {
    UNO(1, "A1", "Principiante"),
    DOS(2, "A2", "Básico"),
    TRES(3, "B1", "Intermedio");

    companion object {
        fun of(number: Int): Nivel = entries.firstOrNull { it.number == number } ?: UNO
    }
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
    val level: String,
    /** 1..3; assigned by the repository from the asset file the word came from. */
    val nivel: Int = 1
) {
    val isNoun: Boolean get() = article != null

    /** German word without its leading article ("das Haus" -> "Haus"). */
    val germanBare: String get() = article?.let { german.removePrefix("$it ") } ?: german
}
