package com.holadeutsch.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.holadeutsch.app.data.model.Word
import com.holadeutsch.app.ui.theme.ArticleDas
import com.holadeutsch.app.ui.theme.ArticleDer
import com.holadeutsch.app.ui.theme.ArticleDie

fun articleColor(article: String?): Color = when (article) {
    "der" -> ArticleDer
    "die" -> ArticleDie
    "das" -> ArticleDas
    else -> Color.Unspecified
}

/** German word with its article rendered in the der/die/das mnemonic color. */
@Composable
fun ArticleText(word: Word, style: TextStyle = LocalTextStyle.current) {
    val text = buildAnnotatedString {
        val article = word.article
        if (article != null && word.german.startsWith("$article ")) {
            withStyle(SpanStyle(color = articleColor(article), fontWeight = FontWeight.Bold)) {
                append(article)
            }
            append(" ")
            append(word.germanBare)
        } else {
            append(word.german)
        }
    }
    Text(text, style = style)
}

/** Animated daily-goal progress ring with a centered label. */
@Composable
fun DailyGoalRing(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "goalRing"
    )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** Five dots showing the Leitner box (mastery level) of a word. */
@Composable
fun MasteryDots(box: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < box) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

/** Small pill chip for stats and tags. */
@Composable
fun StatChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Speaker icon that only shows when a German voice is available and TTS is enabled. */
@Composable
fun SpeakerButton(available: Boolean, onClick: () -> Unit) {
    if (available) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Escuchar pronunciación"
            )
        }
    }
}
