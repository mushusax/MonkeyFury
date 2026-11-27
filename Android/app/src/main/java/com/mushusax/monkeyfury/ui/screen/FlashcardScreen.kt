package com.mushusax.monkeyfury.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.R
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.answerRatings
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.CalendarHeatMap
import com.mushusax.monkeyfury.ui.component.ContentPanel
import com.mushusax.monkeyfury.ui.component.NoteEditDialog
import com.mushusax.monkeyfury.ui.component.edit
import com.mushusax.monkeyfury.ui.viewmodel.FlashcardViewModel
import com.mushusax.monkeyfury.ui.viewmodel.StudySession
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** The Start and Reveal buttons' fill; Material 3 has no green, so it is set here. */
private val StartGreen = Color(0xFF2E7D32)

/** Black panels with white text, matching ContentPanel and the Dashboard buttons. */
@Composable
private fun blackCardColors() = CardDefaults.elevatedCardColors(
    containerColor = Color.Black,
    contentColor = Color.White,
    // Stays black when disabled, with dimmed text, instead of switching to the theme's grey
    disabledContainerColor = Color.Black,
    disabledContentColor = Color.White.copy(alpha = 0.38f)
)

private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")

/** Height of the deck picker row and the study buttons; the pencil and Stop are squares of this size. */
private val ControlSize = 64.dp

/**
 * The Flashcard hub, over the wallpaper. The pencil swaps the deck picker, the pencil and
 * the study area for the selected deck's cards, laid out like ActivityListScreen; Back swaps them back.
 * Start studies the deck's first card: Reveal shows its back, then Easy, Hard or Again end the round.
 */
@Composable
fun FlashcardScreen(
    modifier: Modifier = Modifier,
    viewModel: FlashcardViewModel = viewModel(factory = FlashcardViewModel.Factory)
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val selectedDeck by viewModel.selectedDeck.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val dueCounts by viewModel.dueCounts.collectAsStateWithLifecycle()
    val reviewDays by viewModel.reviewDays.collectAsStateWithLifecycle()
    val revealed by viewModel.revealed.collectAsStateWithLifecycle()

    var deckSheetOpen by remember { mutableStateOf(false) }
    // Points up while the sheet is closed, down while it is open
    val arrowRotation by animateFloatAsState(
        targetValue = if (deckSheetOpen) 180f else 0f,
        label = "arrowRotation"
    )

    var noteDialogOpen by remember { mutableStateOf(false) }
    // The card being edited, or null when the open dialog is creating one
    var editingNote by remember { mutableStateOf<Note?>(null) }

    // Back leaves edit mode before it leaves the screen
    BackHandler(enabled = editing) { viewModel.stopEditing() }
    // Back also stops studying before it leaves the screen
    BackHandler(enabled = session != null) { viewModel.stopStudy() }

    ActivityWallpaper(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val deck = selectedDeck
            if (editing && deck != null) {
                // The panel only grows as tall as its content, so short lists leave the bottom of the
                // wallpaper showing; its bottom margin keeps it clear of the + button.
                ContentPanel(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        // Same side and top margins as the deck picker row it replaces
                        .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 88.dp)
                ) {
                    DeckCards(
                        deck = deck,
                        notes = notes,
                        loading = loading,
                        error = error,
                        onRetry = viewModel::refresh,
                        onNoteClick = { note ->
                            editingNote = note
                            noteDialogOpen = true
                        }
                    )
                }
            } else {
                DeckPickerRow(
                    deckName = deck?.name ?: "No decks yet",
                    dueCount = deck?.let { dueCounts.byDeck[it.publicId] },
                    arrowRotation = arrowRotation,
                    onPickDeck = { deckSheetOpen = true },
                    onEdit = if (deck != null) viewModel::startEditing else null,
                    // Locked while studying, so the deck and its cards can't change under the session
                    enabled = session == null
                )

                // Fills the rest of the screen, with the deck row's side margins and the same
                // 16dp gap as between the deck card and the pencil. Blank until Start.
                StudyArea(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    session = session,
                    revealed = revealed,
                    // Null with no deck, so there's no calendar for nothing
                    reviewDays = deck?.let { reviewDays[it.publicId].orEmpty() }
                )

                // Same margins and 16dp gap as the buttons above
                StudyControls(
                    session = session,
                    revealed = revealed,
                    canStart = notes.isNotEmpty(),
                    onStart = viewModel::startStudy,
                    onReveal = viewModel::reveal,
                    onAnswer = viewModel::answer,
                    onKeepStudying = viewModel::keepStudying,
                    onStop = viewModel::stopStudy
                )
            }
        }

        if (editing && selectedDeck != null) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                onClick = {
                    editingNote = null
                    noteDialogOpen = true
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_add),
                    contentDescription = "Add card"
                )
            }
        }
    }

    if (deckSheetOpen) {
        DeckSheet(
            decks = decks,
            dueCounts = dueCounts.byDeck,
            selectedDeck = selectedDeck,
            onDeckSelected = viewModel::selectDeck,
            onDismiss = { deckSheetOpen = false }
        )
    }

    if (noteDialogOpen) {
        val note = editingNote
        NoteEditDialog(
            note = note,
            onDismiss = { noteDialogOpen = false },
            onSave = { params ->
                viewModel.save(
                    note?.publicId,
                    params,
                    onDone = { noteDialogOpen = false })
            },
            onDelete = note?.let {
                {
                    viewModel.delete(
                        it.publicId,
                        onDone = { noteDialogOpen = false })
                }
            },
            saving = loading
        )
    }
}

/**
 * The deck card, which opens the deck sheet, and the pencil, shown only when [onEdit] is set.
 * [dueCount] is how many of the deck's cards are due; null when there's no deck.
 */
@Composable
private fun DeckPickerRow(
    deckName: String,
    dueCount: Int?,
    arrowRotation: Float,
    onPickDeck: () -> Unit,
    onEdit: (() -> Unit)?,
    enabled: Boolean
) {
    // 32dp side margins line the row up with the outer edges of the buttons below.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp)
            .height(ControlSize),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = blackCardColors(),
            enabled = enabled,
            onClick = onPickDeck
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = deckName,
                    style = MaterialTheme.typography.titleSmall
                )
                if (dueCount != null) {
                    DueCount(count = dueCount)
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Icon(
                    modifier = Modifier.rotate(arrowRotation),
                    painter = painterResource(id = R.drawable.ic_arrow),
                    contentDescription = "Choose deck"
                )
            }
        }
        if (onEdit != null) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                // Same colors as the + buttons here and on Activity List
                colors = CardDefaults.elevatedCardColors(
                    containerColor = FloatingActionButtonDefaults.containerColor,
                    contentColor = contentColorFor(FloatingActionButtonDefaults.containerColor),
                    disabledContainerColor = FloatingActionButtonDefaults.containerColor,
                    disabledContentColor = contentColorFor(FloatingActionButtonDefaults.containerColor).copy(alpha = 0.38f)
                ),
                enabled = enabled,
                onClick = onEdit
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = edit, contentDescription = "Edit deck")
                }
            }
        }
    }
}

/**
 * The studied card: its prompt, and its answer once [revealed]. Says so when the session has run
 * out of cards. When not studying, the deck's reviews this month, when there's a deck.
 */
@Composable
private fun StudyArea(
    modifier: Modifier,
    session: StudySession?,
    revealed: Boolean,
    reviewDays: Map<LocalDate, Int>?
) {
    val card = session?.card
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = blackCardColors()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (session == null && reviewDays != null) {
                ReviewCalendar(reviewDays)
            }
            if (session != null && card == null) {
                Text(
                    text = if (session.ahead) "You've studied every card in this deck" else "All caught up",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when {
                        session.ahead -> "${session.reviewed} reviewed this session"
                        session.reviewed == 0 -> "Nothing is due right now. Keep studying to review cards early."
                        else -> "${session.reviewed} reviewed. Keep studying to review cards early."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            if (card != null) {
                Text(
                    text = card.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                if (revealed) {
                    Text(
                        text = card.answer,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * The bottom row: Start; then Reveal and Stop; then, once revealed, the card's answers and Stop:
 * Easy, Hard and Again while it's new or learning, Good, Hard and Again in day-based review.
 * When the due cards run out, Keep studying; when every card is done, Stop alone.
 * Stop is a square the size of the pencil, always in the bottom right while studying.
 */
@Composable
private fun StudyControls(
    session: StudySession?,
    revealed: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onReveal: () -> Unit,
    onAnswer: (Rating) -> Unit,
    onKeepStudying: () -> Unit,
    onStop: () -> Unit
) {
    val studying = session != null
    val studyCard = session?.card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 32.dp)
            .height(ControlSize),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val green = CardDefaults.elevatedCardColors(containerColor = StartGreen, contentColor = Color.White)
        when {
            !studying -> ControlButton("Start", Modifier.weight(1f), onStart, colors = green, enabled = canStart)
            studyCard == null ->
                if (!session.ahead) ControlButton("Keep studying", Modifier.weight(1f), onKeepStudying, colors = green)
                // Nothing left at all: Stop alone, still in the bottom right
                else Spacer(modifier = Modifier.weight(1f))
            !revealed -> ControlButton("Reveal", Modifier.weight(1f), onReveal, colors = green)
            else -> studyCard.card.state.answerRatings.forEach { rating ->
                ControlButton(rating.label, Modifier.weight(1f), onClick = { onAnswer(rating) })
            }
        }
        if (studying) {
            ControlButton(
                text = "Stop",
                modifier = Modifier.aspectRatio(1f),
                onClick = onStop,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    }
}

/** A rounded elevated card that fills its row's height, with [text] centered. */
@Composable
private fun ControlButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit,
    colors: CardColors = blackCardColors(),
    enabled: Boolean = true
) {
    ElevatedCard(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = colors,
        enabled = enabled,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** The deck's name and description, then its cards; tapping a card edits it. */
@Composable
private fun ColumnScope.DeckCards(
    deck: Deck,
    notes: List<Note>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onNoteClick: (Note) -> Unit
) {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        text = deck.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    if (deck.description.isNotEmpty()) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            text = deck.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    error?.let { message ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = message,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }

    if (notes.isEmpty() && !loading && error == null) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = "No cards yet. Tap + to add one.",
            style = MaterialTheme.typography.bodyLarge
        )
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
        items(items = notes, key = { it.publicId }) { note ->
            NoteRow(note = note, onClick = { onNoteClick(note) })
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    val detail = buildList {
        add(note.back)
        if (note.reversible) add("both directions")
    }.joinToString(" · ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = note.front, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Lists the decks; tapping one selects it and slides the sheet away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckSheet(
    decks: List<Deck>,
    dueCounts: Map<String, Int>,
    selectedDeck: Deck?,
    onDeckSelected: (Deck) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items = decks, key = { it.publicId }) { deck ->
                val selected = deck.publicId == selectedDeck?.publicId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeckSelected(deck)
                            // Let the sheet animate out before removing it
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = deck.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else null,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                    DueCount(count = dueCounts[deck.publicId] ?: 0)
                }
            }
        }
    }
}

/** The deck's answers per day this month as a heat map, the busiest day fullest. */
@Composable
private fun ReviewCalendar(reviewDays: Map<LocalDate, Int>) {
    val month = YearMonth.now()
    val monthDays = reviewDays.filterKeys { YearMonth.from(it) == month }
    val total = monthDays.values.sum()
    val busiest = monthDays.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = month.format(MONTH_YEAR_FORMATTER), style = MaterialTheme.typography.titleMedium)
        CalendarHeatMap(
            values = monthDays.mapValues { it.value.toFloat() },
            month = month,
            intensity = { it / busiest },
            valueLabel = { count -> if (count.toInt() == 1) "1 review" else "${count.toInt()} reviews" }
        )
        Text(
            text = if (total == 1) "1 review this month" else "$total reviews this month",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "N due", dimmed when nothing is due. */
@Composable
private fun DueCount(count: Int) {
    Text(
        text = "$count due",
        style = MaterialTheme.typography.labelLarge,
        color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun FlashcardScreenPreview() {
    FlashcardScreen(modifier = Modifier.fillMaxSize())
}
