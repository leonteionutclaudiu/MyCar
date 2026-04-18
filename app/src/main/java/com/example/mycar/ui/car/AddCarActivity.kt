package com.example.mycar.ui.car

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.Car
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddCarActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCarTheme {
                Scaffold { padding ->
                    AddCarScreen(Modifier.padding(padding)) {
                            brand, model, year, fuelType, horsepower, licensePlate ->
                        saveCar(brand, model, year, fuelType, horsepower, licensePlate)
                    }
                }
            }
        }
    }

    private fun saveCar(
        brand: String,
        model: String,
        year: String,
        fuelType: String,
        horsepower: String,
        licensePlate: String
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        if (brand.isBlank() || model.isBlank() || year.isBlank()) {
            Toast.makeText(this, "Completează câmpurile obligatorii!", Toast.LENGTH_SHORT).show()
            return
        }

        val yearInt = year.toIntOrNull()
        if (yearInt == null || yearInt < 1900 || yearInt > currentYear) {
            Toast.makeText(this, "An invalid!", Toast.LENGTH_SHORT).show()
            return
        }

        val hpInt = horsepower.toIntOrNull()
        if (hpInt == null || hpInt <= 0) {
            Toast.makeText(this, "Cai putere invalizi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (licensePlate.isBlank()) {
            Toast.makeText(this, "Introdu numărul de înmatriculare!", Toast.LENGTH_SHORT).show()
            return
        }

        val car = Car(
            brand = brand,
            model = model,
            year = year,
            fuelType = fuelType,
            horsepower = horsepower,
            licensePlate = licensePlate
        )

        db.collection("users")
            .document(userId)
            .collection("cars")
            .add(car)
            .addOnSuccessListener {
                Toast.makeText(this, "Mașină adăugată!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Eroare: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun AddCarScreen(
    modifier: Modifier = Modifier,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("") }
    var horsepower by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Adaugă mașină", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text("An") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fuelType,
            onValueChange = { fuelType = it },
            label = { Text("Fuel Type (ex: Benzină)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = horsepower,
            onValueChange = { horsepower = it },
            label = { Text("Cai putere") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = licensePlate,
            onValueChange = { licensePlate = it },
            label = { Text("Număr înmatriculare") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSave(
                    brand,
                    model,
                    year,
                    fuelType,
                    horsepower,
                    licensePlate
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salvează")
        }
    }
}