package com.example.mycar.ui.localnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mycar.data.local.LocalNote
import com.example.mycar.data.local.LocalNoteDatabaseHelper
import com.example.mycar.ui.theme.MyCarTheme

class LocalNotesActivity : ComponentActivity() {

    private val databaseHelper by lazy { LocalNoteDatabaseHelper(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCarTheme {
                var notes by remember { mutableStateOf(databaseHelper.getNotes()) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Note locale SQLite") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    LocalNotesScreen(
                        modifier = Modifier.padding(padding),
                        notes = notes,
                        onAddNote = { text ->
                            val noteText = text.trim()
                            if (noteText.isEmpty()) {
                                Toast.makeText(this, "Scrie o notă înainte de salvare", Toast.LENGTH_SHORT).show()
                            } else {
                                databaseHelper.insertNote(noteText)
                                notes = databaseHelper.getNotes()
                            }
                        },
                        onDeleteNote = { id ->
                            databaseHelper.deleteNote(id)
                            notes = databaseHelper.getNotes()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LocalNotesScreen(
    modifier: Modifier = Modifier,
    notes: List<LocalNote>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var newNote by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Note rapide salvate doar pe telefon",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = newNote,
            onValueChange = { newNote = it },
            label = { Text("Ex: Verifică presiunea roților") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onAddNote(newNote)
                newNote = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvează local")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notes.isEmpty()) {
            Text("Nu există note locale salvate.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    LocalNoteItem(note = note, onDelete = { onDeleteNote(note.id) })
                }
            }
        }
    }
}

@Composable
fun LocalNoteItem(note: LocalNote, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Șterge nota", tint = Color.Red)
            }
        }
    }
}
