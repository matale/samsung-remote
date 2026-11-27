package com.kp.samsungremote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kp.samsungremote.data.KeySequence
import com.kp.samsungremote.tv.TvKey
import com.kp.samsungremote.ui.components.TextKey
import com.kp.samsungremote.ui.theme.RemoteColors
import java.util.Locale
import kotlin.math.roundToLong

private data class StepKey(val label: String, val code: String, val color: Color? = null, val textColor: Color = Color.White)

/** Keys offered in the editor, grouped like the remote. */
private val stepGroups: List<Pair<String, List<StepKey>>> = listOf(
    "Navigation" to listOf(
        StepKey("Up", TvKey.UP),
        StepKey("Down", TvKey.DOWN),
        StepKey("Left", TvKey.LEFT),
        StepKey("Right", TvKey.RIGHT),
        StepKey("OK", TvKey.ENTER),
        StepKey("Return", TvKey.RETURN),
        StepKey("Home", TvKey.HOME),
        StepKey("Exit", TvKey.EXIT),
        StepKey("Settings", TvKey.MENU),
        StepKey("Tools", TvKey.TOOLS),
        StepKey("Info", TvKey.INFO),
        StepKey("Guide", TvKey.GUIDE),
    ),
    "Inputs & power" to listOf(
        StepKey("Source", TvKey.SOURCE),
        StepKey("TV", TvKey.TV),
        StepKey("HDMI 1", TvKey.HDMI1),
        StepKey("HDMI 2", TvKey.HDMI2),
        StepKey("HDMI 3", TvKey.HDMI3),
        StepKey("HDMI 4", TvKey.HDMI4),
        StepKey("Ambient", TvKey.AMBIENT),
        StepKey("Power", TvKey.POWER, RemoteColors.Power),
    ),
    "Volume & channel" to listOf(
        StepKey("Vol +", TvKey.VOL_UP),
        StepKey("Vol -", TvKey.VOL_DOWN),
        StepKey("Mute", TvKey.MUTE),
        StepKey("Ch +", TvKey.CH_UP),
        StepKey("Ch -", TvKey.CH_DOWN),
        StepKey("Ch list", TvKey.CH_LIST),
    ),
    "Playback" to listOf(
        StepKey("Play", TvKey.PLAY),
        StepKey("Pause", TvKey.PAUSE),
        StepKey("Stop", TvKey.STOP),
        StepKey("Rew", TvKey.REWIND),
        StepKey("FF", TvKey.FAST_FORWARD),
        StepKey("Rec", TvKey.RECORD),
    ),
    "Numbers" to (1..9).map { StepKey("$it", TvKey.DIGITS.getValue(it)) } + StepKey("0", TvKey.DIGITS.getValue(0)),
    "Colour keys" to listOf(
        StepKey("A", TvKey.RED, RemoteColors.Red),
        StepKey("B", TvKey.GREEN, RemoteColors.Green),
        StepKey("C", TvKey.YELLOW, RemoteColors.Yellow, Color.Black),
        StepKey("D", TvKey.BLUE, RemoteColors.Blue),
    ),
)

private val stepLabels: Map<String, String> = stepGroups.flatMap { it.second }.associate { it.code to it.label }

/** Short name for a key code: the editor's label, or the code without its KEY_ prefix. */
fun keyLabel(code: String): String = stepLabels[code] ?: code.removePrefix("KEY_")

private fun formatDelay(ms: Long): String = String.format(Locale.US, "%.1f s", ms / 1000.0)

@Composable
fun SequencesScreen(vm: RemoteViewModel, modifier: Modifier = Modifier) {
    val sequences by vm.sequences.collectAsStateWithLifecycle()
    val run by vm.sequenceRun.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<KeySequence?>(null) }
    var deleting by remember { mutableStateOf<KeySequence?>(null) }

    editing?.let { draft ->
        SequenceEditor(
            initial = draft,
            isNew = sequences.none { it.id == draft.id },
            onSave = {
                vm.saveSequence(it)
                editing = null
            },
            onTest = vm::runSequence,
            onCancel = { editing = null },
            modifier = modifier,
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { editing = KeySequence(name = "", keys = emptyList()) }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("New")
            }
        }

        if (sequences.isEmpty()) {
            Text(
                "No sequences yet. Make one for something you do often, for example\nHome → Left → Left → OK.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        sequences.forEachIndexed { index, sequence ->
            SequenceCard(
                sequence = sequence,
                run = run?.takeIf { it.id == sequence.id },
                canMoveUp = index > 0,
                canMoveDown = index < sequences.lastIndex,
                onPlay = { vm.runSequence(sequence) },
                onStop = vm::stopSequence,
                onEdit = { editing = sequence },
                onDelete = { deleting = sequence },
                onMove = { vm.moveSequence(sequence.id, it) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    deleting?.let { sequence ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${sequence.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSequence(sequence.id)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SequenceCard(
    sequence: KeySequence,
    run: SequenceRun?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (run != null) onStop() else onPlay() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Icon(
                if (run != null) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (run != null) "Stop" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sequence.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    sequence.keys.joinToString(" → ") { keyLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (run != null) {
                    LinearProgressIndicator(
                        progress = { if (run.total == 0) 0f else run.step.toFloat() / run.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    Text(
                        if (run.step == 0) "Connecting..." else "Key ${run.step} of ${run.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "${sequence.keys.size} keys · ${formatDelay(sequence.delayMs)} pause",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menu = false; onEdit() },
                    )
                    if (canMoveUp) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                            onClick = { menu = false; onMove(-1) },
                        )
                    }
                    if (canMoveDown) {
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                            onClick = { menu = false; onMove(1) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequenceEditor(
    initial: KeySequence,
    isNew: Boolean,
    onSave: (KeySequence) -> Unit,
    onTest: (KeySequence) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var keys by rememberSaveable(initial.id) { mutableStateOf(initial.keys) }
    var delayMs by rememberSaveable(initial.id) { mutableStateOf(initial.delayMs) }
    var custom by rememberSaveable(initial.id) { mutableStateOf("KEY_") }
    val draft = initial.copy(name = name.trim(), keys = keys, delayMs = delayMs)

    BackHandler(onBack = onCancel)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
            Text(
                if (isNew) "New sequence" else "Edit sequence",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onSave(draft) }, enabled = draft.name.isNotEmpty() && keys.isNotEmpty()) { Text("Save") }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Name") },
            placeholder = { Text("e.g. Open Netflix") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        EditorSection("Keys (${keys.size})")
        if (keys.isEmpty()) {
            Text(
                "Tap the keys below in the order they should be pressed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                keys.forEachIndexed { index, code ->
                    InputChip(
                        selected = false,
                        onClick = { keys = keys.filterIndexed { i, _ -> i != index } },
                        label = { Text("${index + 1}. ${keyLabel(code)}") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { keys = keys.dropLast(1) }) { Text("Undo last") }
                OutlinedButton(onClick = { keys = emptyList() }) { Text("Clear") }
                OutlinedButton(onClick = { onTest(draft) }) { Text("Test on TV") }
            }
        }

        EditorSection("Pause between keys: ${formatDelay(delayMs)}")
        Slider(
            value = delayMs.toFloat(),
            onValueChange = { delayMs = ((it / 100f).roundToLong() * 100).coerceIn(KeySequence.MIN_DELAY_MS, KeySequence.MAX_DELAY_MS) },
            valueRange = KeySequence.MIN_DELAY_MS.toFloat()..KeySequence.MAX_DELAY_MS.toFloat(),
            steps = ((KeySequence.MAX_DELAY_MS - KeySequence.MIN_DELAY_MS) / 100 - 1).toInt(),
        )
        Text(
            "Menus that animate or apps that load need a longer pause.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        stepGroups.forEach { (title, group) ->
            EditorSection("Add: $title")
            group.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        TextKey(
                            text = key.label,
                            onClick = { keys = keys + key.code },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = key.color ?: MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (key.color != null) key.textColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13,
                            modifier = Modifier.weight(1f).height(44.dp),
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        EditorSection("Add: custom key code")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.uppercase().trim() },
                singleLine = true,
                label = { Text("KEY_...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (custom.length > 4) keys = keys + custom }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { keys = keys + custom }, enabled = custom.length > 4) { Text("Add") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EditorSection(title: String) = Text(
    title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 10.dp),
)
