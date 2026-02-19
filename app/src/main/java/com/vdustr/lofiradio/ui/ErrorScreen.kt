package com.vdustr.lofiradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vdustr.lofiradio.R
import com.vdustr.lofiradio.ui.theme.BorderStrong
import com.vdustr.lofiradio.ui.theme.ErrorColor
import com.vdustr.lofiradio.ui.theme.Primary
import com.vdustr.lofiradio.ui.theme.SurfaceVariant
import com.vdustr.lofiradio.ui.theme.TextMuted
import com.vdustr.lofiradio.ui.theme.TextSecondary
import com.vdustr.lofiradio.ui.theme.VarelaRound
import com.vdustr.lofiradio.ui.theme.WarningColor
import com.vdustr.lofiradio.viewmodel.RadioViewModel.ErrorType

@Composable
fun ErrorScreen(
    errorType: ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconBg, iconTint, title, desc, retryLabel) = when (errorType) {
        ErrorType.OFFLINE -> ErrorContent(
            icon = Icons.Default.WifiOff,
            iconBg = TextSecondary.copy(alpha = 0.1f),
            iconTint = TextSecondary,
            title = stringResource(R.string.no_internet_title),
            desc = stringResource(R.string.no_internet_desc),
            retryLabel = stringResource(R.string.retry)
        )
        ErrorType.STREAM_DOWN -> ErrorContent(
            icon = Icons.Default.Warning,
            iconBg = WarningColor.copy(alpha = 0.1f),
            iconTint = WarningColor,
            title = stringResource(R.string.stream_offline_title),
            desc = stringResource(R.string.stream_offline_desc),
            retryLabel = stringResource(R.string.try_again)
        )
        ErrorType.EXTRACTION_ERROR -> ErrorContent(
            icon = Icons.Default.Warning,
            iconBg = ErrorColor.copy(alpha = 0.1f),
            iconTint = ErrorColor,
            title = stringResource(R.string.something_wrong_title),
            desc = stringResource(R.string.something_wrong_desc),
            retryLabel = stringResource(R.string.retry)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = VarelaRound),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Description
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Retry button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariant)
                .clickable(onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = retryLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = Primary
            )
        }
    }
}

private data class ErrorContent(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val desc: String,
    val retryLabel: String
)
