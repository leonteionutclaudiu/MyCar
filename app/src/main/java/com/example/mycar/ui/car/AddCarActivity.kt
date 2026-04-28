package com.example.mycar.ui.car

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.Car
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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
        val itp = intent.getStringExtra("itpExpiry") ?: ""
        val rca = intent.getStringExtra("rcaExpiry") ?: ""
        val rov = intent.getStringExtra("rovinietaExpiry") ?: ""

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
                    initialItp = itp,
                    initialRca = rca,
                    initialRov = rov,
                    onSave = { b, m, y, f, h, p, itpE, rcaE, rovE ->
                        saveCar(b, m, y, f, h, p, itpE, rcaE, rovE, carId)
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
        itp: String,
        rca: String,
        rov: String,
        carId: String?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val formattedPlate = plate.trim().uppercase()

        val ref = db.collection("users")
            .document(userId)
            .collection("cars")

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
                    performSave(brand, model, year, fuel, hp, formattedPlate, itp, rca, rov, carId, ref)
                }
            }
    }

    private fun performSave(
        brand: String,
        model: String,
        year: String,
        fuel: String,
        hp: String,
        plate: String,
        itp: String,
        rca: String,
        rov: String,
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
            licensePlate = plate,
            itpExpiry = itp,
            rcaExpiry = rca,
            rovinietaExpiry = rov
        )

        docRef.set(car)
            .addOnSuccessListener {
                val message = if (carId != null) "Mașină actualizată" else "Mașină adăugată"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
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
    initialItp: String = "",
    initialRca: String = "",
    initialRov: String = "",
    onSave: (String, String, String, String, String, String, String, String, String) -> Unit
) {
    var brand by remember { mutableStateOf(initialBrand) }
    var model by remember { mutableStateOf(initialModel) }
    var year by remember { mutableStateOf(initialYear) }
    var fuelType by remember { mutableStateOf(initialFuel) }
    var horsepower by remember { mutableStateOf(initialHp) }
    var plate by remember { mutableStateOf(initialPlate) }
    var itpDate by remember { mutableStateOf(initialItp) }
    var rcaDate by remember { mutableStateOf(initialRca) }
    var rovinietaDate by remember { mutableStateOf(initialRov) }

    val isPlateValid = remember(plate) {
        plate.matches("^[A-Z]{1,2} [0-9]{2,3} [A-Z]{3}$".toRegex())
    }

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

        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("An") }, modifier = Modifier.fillMaxWidth())

        FuelDropdown(selected = fuelType, onSelected = { fuelType = it })

        OutlinedTextField(value = horsepower, onValueChange = { horsepower = it }, label = { Text("HP") }, modifier = Modifier.fillMaxWidth())
        
        OutlinedTextField(
            value = plate,
            onValueChange = { 
                // Formatare automată: litere mari și gestionare spații
                plate = it.uppercase().replace("\\s+".toRegex(), " ")
            }, 
            label = { Text("Număr de înmatriculare") }, 
            placeholder = { Text("ex: B 123 ABC sau CT 12 ABC") },
            isError = plate.isNotEmpty() && !isPlateValid,
            supportingText = {
                if (plate.isNotEmpty() && !isPlateValid) {
                    Text("Format: JUDEȚ NUMĂR LITERE (ex: B 123 ABC)", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Documente (Expirație):", style = MaterialTheme.typography.titleMedium)

        DatePickerField(label = "Expirare ITP", date = itpDate, onDateSelected = { itpDate = it })
        DatePickerField(label = "Expirare RCA", date = rcaDate, onDateSelected = { rcaDate = it })
        DatePickerField(label = "Expirare Rovinietă", date = rovinietaDate, onDateSelected = { rovinietaDate = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (brand.isBlank() || model.isBlank() || plate.isBlank() || !isPlateValid) return@Button
                onSave(brand, model, year, fuelType, horsepower, plate, itpDate, rcaDate, rovinietaDate)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = brand.isNotBlank() && model.isNotBlank() && isPlateValid
        ) {
            Text("Salvează")
        }
    }
}

@Composable
fun DatePickerField(label: String, date: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    OutlinedTextField(
        value = date,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected("$day/${month + 1}/$year")
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
        enabled = false, // Setăm false pentru a forța click-ul pe modifier
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelDropdown(selected: String, onSelected: (String) -> Unit) {
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { fuel ->
                DropdownMenuItem(text = { Text(fuel) }, onClick = { onSelected(fuel); expanded = false })
            }
        }
    }
}
