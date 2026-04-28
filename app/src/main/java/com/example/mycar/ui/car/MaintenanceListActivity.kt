package com.example.mycar.ui.car

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.MaintenanceRecord
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MaintenanceListActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var carId: String

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        carId = intent.getStringExtra("carId") ?: ""
        val carName = intent.getStringExtra("carName") ?: "Mașină"

        if (carId.isEmpty()) {
            finish()
            return
        }

        setContent {
            MyCarTheme {
                var records by remember { mutableStateOf(listOf<MaintenanceRecord>()) }

                LaunchedEffect(Unit) {
                    listenForRecords { records = it }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Istoric Service: $carName") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            val intent = Intent(this, AddMaintenanceActivity::class.java)
                            intent.putExtra("carId", carId)
                            startActivity(intent)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Record")
                        }
                    }
                ) { padding ->
                    MaintenanceListScreen(
                        modifier = Modifier.padding(padding),
                        records = records,
                        onDelete = { deleteRecord(it) }
                    )
                }
            }
        }
    }

    private fun listenForRecords(onUpdate: (List<MaintenanceRecord>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("cars").document(carId)
            .collection("maintenance")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MaintenanceRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onUpdate(list)
            }
    }

    private fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("cars").document(carId)
            .collection("maintenance").document(recordId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Înregistrare ștearsă", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun MaintenanceListScreen(
    modifier: Modifier = Modifier,
    records: List<MaintenanceRecord>,
    onDelete: (String) -> Unit
) {
    if (records.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nicio intervenție înregistrată.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                MaintenanceItem(record, onDelete)
            }
        }
    }
}

@Composable
fun MaintenanceItem(record: MaintenanceRecord, onDelete: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text(record.description, style = MaterialTheme.typography.titleMedium)
                Text("Dată: ${record.date}", style = MaterialTheme.typography.bodySmall)
                Text("Kilometraj: ${record.mileage} km", style = MaterialTheme.typography.bodySmall)
                if (record.cost.isNotEmpty()) {
                    Text("Cost: ${record.cost} RON", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmare ștergere") },
            text = { Text("Sigur vrei să ștergi această înregistrare de service?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDelete(record.id)
                }) {
                    Text("Șterge", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Anulează")
                }
            }
        )
    }
}
