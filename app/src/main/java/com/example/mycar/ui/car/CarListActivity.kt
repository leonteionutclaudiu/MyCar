package com.example.mycar.ui.car

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mycar.data.model.Car
import com.example.mycar.ui.theme.MyCarTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class CarListActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCarTheme {
                var carList by remember { mutableStateOf(listOf<Car>()) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Mașinile mele") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { startActivity(Intent(this, AddCarActivity::class.java)) }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă mașină")
                        }
                    }
                ) { padding ->
                    CarListScreen(
                        modifier = Modifier.padding(padding),
                        cars = carList,
                        onDelete = { carId -> deleteCar(carId) }
                    )
                }

                listenForCars { cars ->
                    carList = cars
                }
            }
        }
    }

    private fun listenForCars(onUpdate: (List<Car>) -> Unit) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Trebuie să fii autentificat pentru a vedea mașinile.", Toast.LENGTH_LONG).show()
            return
        }

        listener = db.collection("users")
            .document(userId)
            .collection("cars")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Eroare Firestore: ${error.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val cars = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                onUpdate(cars)
            }
    }

    private fun deleteCar(carId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Trebuie să fii autentificat pentru a șterge mașina.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("users")
            .document(userId)
            .collection("cars")
            .document(carId)
            .delete()
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Nu am putut șterge mașina: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}

@Composable
fun CarListScreen(
    modifier: Modifier = Modifier,
    cars: List<Car>,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (cars.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nu ai mașini adăugate.")
            }
        } else {
            LazyColumn {
                items(cars) { car ->
                    CarItem(
                        car = car,
                        onDelete = { onDelete(car.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarItem(
    car: Car,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    fun getExpiryColor(dateString: String): Color {
        if (dateString.isEmpty()) return Color.Unspecified
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val expiryDate = sdf.parse(dateString) ?: return Color.Unspecified
            val today = Calendar.getInstance().time
            val diffInMillies = expiryDate.time - today.time
            val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)

            when {
                diffInDays < 0 -> Color.Red
                diffInDays <= 15 -> Color.Red
                diffInDays <= 30 -> Color(0xFFFFA500)
                else -> Color(0xFF2E7D32)
            }
        } catch (e: Exception) {
            Color.Unspecified
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDialog = true
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Șterge",
                    tint = Color.Red
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                onClick = {
                    val intent = Intent(context, AddCarActivity::class.java).apply {
                        putExtra("carId", car.id)
                        putExtra("brand", car.brand)
                        putExtra("model", car.model)
                        putExtra("year", car.year)
                        putExtra("fuelType", car.fuelType)
                        putExtra("horsepower", car.horsepower)
                        putExtra("licensePlate", car.licensePlate)
                        putExtra("itpExpiry", car.itpExpiry)
                        putExtra("rcaExpiry", car.rcaExpiry)
                        putExtra("rovinietaExpiry", car.rovinietaExpiry)
                    }
                    context.startActivity(intent)
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${car.brand} ${car.model}", style = MaterialTheme.typography.titleMedium)
                    Text("Număr: ${car.licensePlate}", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    if (car.itpExpiry.isNotEmpty()) {
                        Text("ITP: ${car.itpExpiry}", style = MaterialTheme.typography.bodySmall, color = getExpiryColor(car.itpExpiry))
                    }
                    if (car.rcaExpiry.isNotEmpty()) {
                        Text("RCA: ${car.rcaExpiry}", style = MaterialTheme.typography.bodySmall, color = getExpiryColor(car.rcaExpiry))
                    }
                    if (car.rovinietaExpiry.isNotEmpty()) {
                        Text("Rovinietă: ${car.rovinietaExpiry}", style = MaterialTheme.typography.bodySmall, color = getExpiryColor(car.rovinietaExpiry))
                    }

                    if (car.itpExpiry.isEmpty() && car.rcaExpiry.isEmpty() && car.rovinietaExpiry.isEmpty()) {
                        Text(
                            "Status documente: nesetat. Apasă pentru a adăuga.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val intent = Intent(context, MaintenanceListActivity::class.java).apply {
                                putExtra("carId", car.id)
                                putExtra("carName", "${car.brand} ${car.model}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Vezi istoric service")
                    }
                }
            }
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmare") },
            text = { Text("Sigur vrei să ștergi această mașină?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDelete()
                }) {
                    Text("Șterge")
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
