package com.example.mycar.ui.car

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.Car
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddCarActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val carId = intent.getStringExtra("carId")
        val isEditMode = carId != null

        setContent {
            MyCarTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if (isEditMode) "Editează mașină" else "Adaugă mașină") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    AddCarScreen(
                        modifier = Modifier.padding(padding),
                        isEditMode = isEditMode,
                        initialBrand = intent.getStringExtra("brand") ?: "",
                        initialModel = intent.getStringExtra("model") ?: "",
                        initialYear = intent.getStringExtra("year") ?: "",
                        initialFuel = intent.getStringExtra("fuelType") ?: "",
                        initialHp = intent.getStringExtra("horsepower") ?: "",
                        initialPlate = intent.getStringExtra("licensePlate") ?: "",
                        initialItp = intent.getStringExtra("itpExpiry") ?: "",
                        initialRca = intent.getStringExtra("rcaExpiry") ?: "",
                        initialRov = intent.getStringExtra("rovinietaExpiry") ?: "",
                        onSave = { brand, model, year, fuel, hp, plate, itp, rca, rov ->
                            saveCar(brand, model, year, fuel, hp, plate, itp, rca, rov, carId)
                        }
                    )
                }
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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Trebuie să fii autentificat pentru a salva mașina.", Toast.LENGTH_LONG).show()
            return
        }

        val formattedPlate = plate.trim().uppercase()
        val ref = db.collection("users")
            .document(userId)
            .collection("cars")

        ref.whereEqualTo("licensePlate", formattedPlate).get()
            .addOnSuccessListener { documents ->
                val isDuplicate = documents.any { document ->
                    carId == null || document.id != carId
                }

                if (isDuplicate) {
                    Toast.makeText(this, "Numărul $formattedPlate este deja înregistrat!", Toast.LENGTH_LONG).show()
                } else {
                    performSave(brand, model, year, fuel, hp, formattedPlate, itp, rca, rov, carId, ref)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Nu pot verifica mașina: ${exception.message}", Toast.LENGTH_LONG).show()
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
        ref: CollectionReference
    ) {
        val docRef = if (carId != null) ref.document(carId) else ref.document()
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
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Nu am putut salva mașina: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun AddCarScreen(
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isEditMode) "Actualizează datele mașinii" else "Completează datele mașinii",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = year, onValueChange = { year = it.filter(Char::isDigit) }, label = { Text("An") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        FuelDropdown(selected = fuelType, onSelected = { fuelType = it })

        OutlinedTextField(value = horsepower, onValueChange = { horsepower = it.filter(Char::isDigit) }, label = { Text("CP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        OutlinedTextField(
            value = plate,
            onValueChange = {
                plate = it.uppercase().replace("\\s+".toRegex(), " ")
            },
            label = { Text("Număr de înmatriculare") },
            placeholder = { Text("ex: B 123 ABC sau CT 12 ABC") },
            isError = plate.isNotEmpty() && !isPlateValid,
            supportingText = {
                if (plate.isNotEmpty() && !isPlateValid) {
                    Text("Format: județ număr litere (ex: B 123 ABC)", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Documente - date de expirare", style = MaterialTheme.typography.titleMedium)

        DatePickerField(label = "Expirare ITP", date = itpDate, onDateSelected = { itpDate = it })
        DatePickerField(label = "Expirare RCA", date = rcaDate, onDateSelected = { rcaDate = it })
        DatePickerField(label = "Expirare rovinietă", date = rovinietaDate, onDateSelected = { rovinietaDate = it })

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
                    { _, year, month, day -> onDateSelected("$day/${month + 1}/$year") },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
