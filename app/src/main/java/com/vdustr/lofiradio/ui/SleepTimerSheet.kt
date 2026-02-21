package com.vdustr.lofiradio.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vdustr.lofiradio.ui.theme.Background
import com.vdustr.lofiradio.ui.theme.Border
import com.vdustr.lofiradio.ui.theme.ErrorColor
import com.vdustr.lofiradio.ui.theme.Primary
import com.vdustr.lofiradio.ui.theme.PrimarySubtle
import com.vdustr.lofiradio.ui.theme.Surface
import com.vdustr.lofiradio.ui.theme.TextMuted
import com.vdustr.lofiradio.ui.theme.TextSecondary
import com.vdustr.lofiradio.ui.theme.VarelaRound
import com.vdustr.lofiradio.util.formatTimer
import com.vdustr.lofiradio.viewmodel.RadioViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private enum class TimerMode { DURATION, AT_TIME }

private data class TimerPreset(val minutes: Int, val label: String)

private val presets = listOf(
    TimerPreset(15, "15m"),
    TimerPreset(30, "30m"),
    TimerPreset(60, "1h"),
    TimerPreset(120, "2h")
)

private val segmentedColors
    @Composable get() = SegmentedButtonDefaults.colors(
        activeContainerColor = Primary.copy(alpha = 0.15f),
        activeContentColor = Primary,
        inactiveContainerColor = Surface,
        inactiveContentColor = TextSecondary,
        activeBorderColor = Primary,
        inactiveBorderColor = Border,
    )

private val timerButtonColors
    @Composable get() = ButtonDefaults.buttonColors(
        containerColor = Primary,
        contentColor = Background,
        disabledContainerColor = Primary.copy(alpha = 0.3f),
        disabledContentColor = Background.copy(alpha = 0.5f)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerState: RadioViewModel.SleepTimerState,
    onStart: (durationMillis: Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    var selectedMode by remember { mutableStateOf(TimerMode.DURATION) }
    var draftHours by remember { mutableIntStateOf(0) }
    var draftMinutes by remember { mutableIntStateOf(30) }
    var draftTargetHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var draftTargetMinute by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // Title row with remaining time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = VarelaRound),
                )
                if (sleepTimerState.isActive) {
                    Text(
                        text = formatTimer(sleepTimerState.remainingMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode segmented button
            val colors = segmentedColors
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedMode == TimerMode.DURATION,
                    onClick = { selectedMode = TimerMode.DURATION },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = colors,
                ) {
                    Text("Duration")
                }
                SegmentedButton(
                    selected = selectedMode == TimerMode.AT_TIME,
                    onClick = { selectedMode = TimerMode.AT_TIME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = colors,
                ) {
                    Text("At Time")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode content
            AnimatedContent(
                targetState = selectedMode,
                label = "timer_mode"
            ) { mode ->
                when (mode) {
                    TimerMode.DURATION -> DurationContent(
                        draftHours = draftHours,
                        draftMinutes = draftMinutes,
                        onDraftHoursChange = { draftHours = it },
                        onDraftMinutesChange = { draftMinutes = it },
                        onPresetStart = { presetMinutes ->
                            onStart(presetMinutes * 60_000L)
                            onDismiss()
                        },
                        onCustomStart = {
                            val millis = draftHours * 3_600_000L + draftMinutes * 60_000L
                            onStart(millis)
                            onDismiss()
                        }
                    )
                    TimerMode.AT_TIME -> AtTimeContent(
                        draftTargetHour = draftTargetHour,
                        draftTargetMinute = draftTargetMinute,
                        onDraftTargetHourChange = { draftTargetHour = it },
                        onDraftTargetMinuteChange = { draftTargetMinute = it },
                        onStart = { millis ->
                            onStart(millis)
                            onDismiss()
                        }
                    )
                }
            }

            // Cancel button (only when active)
            if (sleepTimerState.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel Timer",
                        color = ErrorColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DurationContent(
    draftHours: Int,
    draftMinutes: Int,
    onDraftHoursChange: (Int) -> Unit,
    onDraftMinutesChange: (Int) -> Unit,
    onPresetStart: (Int) -> Unit,
    onCustomStart: () -> Unit,
) {
    Column {
        // Preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimarySubtle)
                        .clickable { onPresetStart(preset.minutes) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom duration stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            StepperField(
                value = draftHours,
                onValueChange = onDraftHoursChange,
                range = 0..23,
                label = "h"
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            StepperField(
                value = draftMinutes,
                onValueChange = onDraftMinutesChange,
                range = 0..59,
                label = "m"
            )

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onCustomStart,
                enabled = draftHours > 0 || draftMinutes > 0,
                colors = timerButtonColors
            ) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun AtTimeContent(
    draftTargetHour: Int,
    draftTargetMinute: Int,
    onDraftTargetHourChange: (Int) -> Unit,
    onDraftTargetMinuteChange: (Int) -> Unit,
    onStart: (durationMillis: Long) -> Unit,
) {
    // Compute fresh each recomposition from single time source
    val nowMs = System.currentTimeMillis()
    val now = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault())
    val target = LocalTime.of(draftTargetHour, draftTargetMinute)
    val durationMs = durationUntilTime(target, now, nowMs)
    val crossesMidnight = !target.isAfter(now.toLocalTime())

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Time stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            StepperField(
                value = draftTargetHour,
                onValueChange = onDraftTargetHourChange,
                range = 0..23,
                label = "H",
                padZero = true
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            StepperField(
                value = draftTargetMinute,
                onValueChange = onDraftTargetMinuteChange,
                range = 0..59,
                label = "M",
                padZero = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview text
        val previewText = formatDurationPreview(durationMs)
        Text(
            text = "Stops in $previewText",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        if (crossesMidnight) {
            Text(
                text = "(tomorrow)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start button — recalculate duration at click time to avoid stale snapshot
        Button(
            onClick = {
                val clickTarget = LocalTime.of(draftTargetHour, draftTargetMinute)
                val clickNowMs = System.currentTimeMillis()
                val clickNow = Instant.ofEpochMilli(clickNowMs).atZone(ZoneId.systemDefault())
                onStart(durationUntilTime(clickTarget, clickNow, clickNowMs))
            },
            enabled = durationMs >= 60_000L,
            colors = timerButtonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Timer")
        }
    }
}

@Composable
private fun StepperField(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    padZero: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            enabled = value < range.last
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = if (value < range.last) TextSecondary else TextMuted.copy(alpha = 0.3f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (padZero) String.format(java.util.Locale.US, "%02d", value)
                       else value.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextMuted,
                modifier = Modifier.padding(start = 2.dp)
            )
        }

        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            enabled = value > range.first
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = if (value > range.first) TextSecondary else TextMuted.copy(alpha = 0.3f)
            )
        }
    }
}

private fun durationUntilTime(
    target: LocalTime,
    now: ZonedDateTime,
    nowMs: Long,
): Long {
    var targetZdt = now.with(target)
    if (!targetZdt.isAfter(now)) targetZdt = targetZdt.plusDays(1)
    return targetZdt.toInstant().toEpochMilli() - nowMs
}

private fun formatDurationPreview(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "~${hours}h ${minutes}m"
        hours > 0 -> "~${hours}h"
        else -> "~${minutes}m"
    }
}
