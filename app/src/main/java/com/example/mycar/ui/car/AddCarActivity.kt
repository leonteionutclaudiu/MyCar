package com.example.mycar.ui.car

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.Car
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddCarActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val carId = intent.getStringExtra("carId")
        val isEditMode = carId != null

        val brand = intent.getStringExtra("brand") ?: ""
        val model = intent.getStringExtra("model") ?: ""
        val year = intent.getStringExtra("year") ?: ""
        val fuel = intent.getStringExtra("fuelType") ?: ""
        val hp = intent.getStringExtra("horsepower") ?: ""
        val plate = intent.getStringExtra("licensePlate") ?: ""

        setContent {
            MyCarTheme {
                AddCarScreen(
                    isEditMode = isEditMode,
                    initialBrand = brand,
                    initialModel = model,
                    initialYear = year,
                    initialFuel = fuel,
                    initialHp = hp,
                    initialPlate = plate,
                    onSave = { b, m, y, f, h, p ->
                        saveCar(b, m, y, f, h, p, carId)
                    }
                )
            }
        }
    }

    private fun saveCar(
        brand: String,
        model: String,
        year: String,
        fuel: String,
        hp: String,
        plate: String,
        carId: String?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val formattedPlate = plate.trim().uppercase()

        val ref = db.collection("users")
            .document(userId)
            .collection("cars")

        // Validare: Verificăm dacă numărul de înmatriculare există deja la altă mașină
        ref.whereEqualTo("licensePlate", formattedPlate).get()
            .addOnSuccessListener { documents ->
                var isDuplicate = false
                for (document in documents) {
                    if (carId == null || document.id != carId) {
                        isDuplicate = true
                        break
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(this, "Numărul $formattedPlate este deja înregistrat!", Toast.LENGTH_LONG).show()
                } else {
                    performSave(brand, model, year, fuel, hp, formattedPlate, carId, ref)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Eroare la verificarea numărului", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSave(
        brand: String,
        model: String,
        year: String,
        fuel: String,
        hp: String,
        plate: String,
        carId: String?,
        ref: com.google.firebase.firestore.CollectionReference
    ) {
        val docRef = if (carId != null) {
            ref.document(carId)
        } else {
            ref.document()
        }

        val car = Car(
            id = docRef.id,
            brand = brand,
            model = model,
            year = year,
            fuelType = fuel,
            horsepower = hp,
            licensePlate = plate
        )

        docRef.set(car)
            .addOnSuccessListener {
                val message = if (carId != null) "Mașină actualizată" else "Mașină adăugată"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Eroare la salvare", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun AddCarScreen(
    isEditMode: Boolean,
    initialBrand: String = "",
    initialModel: String = "",
    initialYear: String = "",
    initialFuel: String = "",
    initialHp: String = "",
    initialPlate: String = "",
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var brand by remember { mutableStateOf(initialBrand) }
    var model by remember { mutableStateOf(initialModel) }
    var year by remember { mutableStateOf(initialYear) }
    var fuelType by remember { mutableStateOf(initialFuel) }
    var horsepower by remember { mutableStateOf(initialHp) }
    var plate by remember { mutableStateOf(initialPlate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {

        Text(
            text = if (isEditMode) "Editează mașină" else "Adaugă mașină",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("An") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FuelDropdown(
            selected = fuelType,
            onSelected = { fuelType = it }
        )

        OutlinedTextField(
            value = horsepower,
            onValueChange = { horsepower = it },
            label = { Text("HP") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it },
            label = { Text("Număr") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (brand.isBlank() || model.isBlank() || plate.isBlank()) return@Button
                onSave(brand, model, year, fuelType, horsepower, plate)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvează")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf("Benzină", "Diesel", "Electric", "Hybrid", "GPL")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Combustibil") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { fuel ->
                DropdownMenuItem(
                    text = { Text(fuel) },
                    onClick = {
                        onSelected(fuel)
                        expanded = false
                    }
                )
            }
        }
    }
}
