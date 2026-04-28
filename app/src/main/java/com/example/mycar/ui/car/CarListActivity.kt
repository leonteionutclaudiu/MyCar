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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.*
import java.util.concurrent.TimeUnit

class CarListActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyCarTheme {

                var carList by remember { mutableStateOf(listOf<Car>()) }

                Scaffold { padding ->
                    CarListScreen(
                        modifier = Modifier.padding(padding),
                        cars = carList,
                        onDelete = { carId ->
                            deleteCar(carId)
                        }
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
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        listener = db.collection("users")
            .document(userId)
            .collection("cars")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Toast.makeText(this, "Eroare: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val cars = snapshot?.documents?.mapNotNull { doc ->
                    val car = doc.toObject(Car::class.java)
                    car?.copy(id = doc.id)
                } ?: emptyList()

                onUpdate(cars)
            }
    }

    private fun deleteCar(carId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("cars")
            .document(carId)
            .delete()
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
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Mașinile mele", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (cars.isEmpty()) {
            Text("Nu ai mașini adăugate")
        } else {
            LazyColumn {
                items(cars) { car ->
                    CarItem(
                        car = car,
                        onDelete = { onDelete(car.id) },
                        onEdit = {
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
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {

    // Funcție helper pentru a calcula culoarea în funcție de dată
    fun getExpiryColor(dateString: String): Color {
        if (dateString.isEmpty()) return Color.Unspecified
        return try {
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val expiryDate = sdf.parse(dateString) ?: return Color.Unspecified
            val today = Calendar.getInstance().time

            val diffInMillies = expiryDate.time - today.time
            val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)

            when {
                diffInDays < 0 -> Color.Red // Expirat
                diffInDays <= 15 -> Color.Red // Critic (sub 15 zile)
                diffInDays <= 30 -> Color(0xFFFFA500) // Atenție (Portocaliu - sub 30 zile)
                else -> Color(0xFF2E7D32) // Ok (Verde închis)
            }
        } catch (e: Exception) {
            Color.Unspecified
        }
    }
    
    var showDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDialog = true
                false
            } else {
                false
            }
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
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                onClick = { onEdit() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        "${car.brand} ${car.model}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Număr: ${car.licensePlate}", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    if (car.itpExpiry.isNotEmpty()) {
                        Text(
                            "📅 ITP: ${car.itpExpiry}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = getExpiryColor(car.itpExpiry)
                        )
                    }
                    if (car.rcaExpiry.isNotEmpty()) {
                        Text(
                            "📅 RCA: ${car.rcaExpiry}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = getExpiryColor(car.rcaExpiry)
                        )
                    }
                    if (car.rovinietaExpiry.isNotEmpty()) {
                        Text(
                            "📅 Rovinietă: ${car.rovinietaExpiry}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = getExpiryColor(car.rovinietaExpiry)
                        )
                    }

                    if (car.itpExpiry.isEmpty() && car.rcaExpiry.isEmpty() && car.rovinietaExpiry.isEmpty()) {
                        Text("Status documente: Nesetat. Apasă pentru a adăuga.",
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
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
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text("Anulează")
                }
            }
        )
    }
}
