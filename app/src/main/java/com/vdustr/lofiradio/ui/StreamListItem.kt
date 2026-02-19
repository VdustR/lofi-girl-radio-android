package com.vdustr.lofiradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vdustr.lofiradio.data.LofiStream
import com.vdustr.lofiradio.ui.theme.Primary
import com.vdustr.lofiradio.ui.theme.PrimarySubtle
import com.vdustr.lofiradio.ui.theme.TextMuted
import java.text.NumberFormat
import java.util.Locale

private val thumbGradients = listOf(
    listOf(Color(0xFF1A0533), Color(0xFF2D1B4E), Color(0xFF1E3A5F)), // study
    listOf(Color(0xFF0A1628), Color(0xFF1A1040), Color(0xFF0D1F3D)), // sleep
    listOf(Color(0xFF1A0A2E), Color(0xFF2A1A3E), Color(0xFF1A2A1A)), // piano
    listOf(Color(0xFF0A0A0A), Color(0xFF1A0A1A), Color(0xFF0A0A1A)), // dark
    listOf(Color(0xFF1A0A2E), Color(0xFF2E0A2E), Color(0xFF1A1A3E))  // synth
)

@Composable
fun StreamListItem(
    stream: LofiStream,
    isActive: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgModifier = if (isActive) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PrimarySubtle)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = thumbGradients[index % thumbGradients.size]
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (stream.thumbnailUrl != null) {
                AsyncImage(
                    model = stream.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) Primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = formatViewerCount(stream.viewerCount),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }

    }
}

private fun formatViewerCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM watching", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK watching", count / 1_000.0)
        else -> "$count watching"
    }
}
