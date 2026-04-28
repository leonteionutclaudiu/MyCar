package com.example.mycar.ui.car

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.MaintenanceRecord
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddMaintenanceActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val carId = intent.getStringExtra("carId") ?: ""

        if (carId.isEmpty()) {
            finish()
            return
        }

        setContent {
            MyCarTheme {
                AddMaintenanceScreen(
                    onBack = { finish() },
                    onSave = { date, mileage, description, cost ->
                        saveMaintenance(carId, date, mileage, description, cost)
                    }
                )
            }
        }
    }

    private fun saveMaintenance(carId: String, date: String, mileage: String, description: String, cost: String) {
        val userId = auth.currentUser?.uid ?: return

        val record = MaintenanceRecord(
            carId = carId,
            date = date,
            mileage = mileage,
            description = description,
            cost = cost
        )

        db.collection("users").document(userId)
            .collection("cars").document(carId)
            .collection("maintenance")
            .add(record)
            .addOnSuccessListener {
                Toast.makeText(this, "Intervenție salvată", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Eroare la salvare", Toast.LENGTH_SHORT).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceScreen(
    onBack: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var date by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    // Helper pentru a curăța zerourile de la început (ex: "0123" -> "123")
    fun formatInput(input: String): String {
        if (input.startsWith("0") && input.length > 1 && !input.startsWith("0.")) {
            val processed = input.dropWhile { it == '0' }
            return if (processed.isEmpty()) "0" else processed
        }
        return input
    }

    // Validări stricte
    val isMileageValid = mileage.isNotEmpty() && (mileage.toLongOrNull() ?: 0L) > 0
    val isCostValid = cost.isNotEmpty() && (cost.toDoubleOrNull() ?: 0.0) > 0.0 && cost.matches(Regex("^\\d+(\\.\\d{1,2})?$"))
    val isDescriptionValid = description.trim().length >= 3
    
    val canSave = date.isNotBlank() && isMileageValid && isDescriptionValid && isCostValid

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adaugă Intervenție") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                label = { Text("Data *") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val dpd = DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                date = "$day/${month + 1}/$year"
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.datePicker.maxDate = System.currentTimeMillis()
                        dpd.show()
                    },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            OutlinedTextField(
                value = mileage,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    mileage = formatInput(filtered)
                },
                label = { Text("Kilometraj (km) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = mileage.isNotEmpty() && !isMileageValid,
                supportingText = {
                    if (mileage.isNotEmpty() && !isMileageValid) {
                        Text("Introdu un kilometraj valid (>0)")
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descriere (ex: Schimb ulei) *") },
                modifier = Modifier.fillMaxWidth(),
                isError = description.isNotEmpty() && !isDescriptionValid,
                supportingText = {
                    if (description.isNotEmpty() && !isDescriptionValid) {
                        Text("Minim 3 caractere")
                    }
                }
            )

            OutlinedTextField(
                value = cost,
                onValueChange = { input ->
                    var sanitized = input.replace(",", ".")
                    if (sanitized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        cost = formatInput(sanitized)
                    }
                },
                label = { Text("Cost (RON) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = cost.isNotEmpty() && !isCostValid,
                supportingText = {
                    if (cost.isNotEmpty() && !isCostValid) {
                        Text("Introdu un preț valid (ex: 150 sau 150.99)")
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(date, mileage, description, cost)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text("Salvează")
            }
        }
    }
}
