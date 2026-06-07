package com.example.mycar.ui.dashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mycar.R
import com.example.mycar.ui.auth.LoginActivity
import com.example.mycar.ui.car.AddCarActivity
import com.example.mycar.ui.car.CarListActivity
import com.example.mycar.ui.catalog.VehicleCatalogActivity
import com.example.mycar.ui.localnotes.LocalNotesActivity
import com.example.mycar.ui.theme.MyCarTheme
import com.example.mycar.worker.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class DashboardActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Atenție: fără permisiune nu vei primi alerte despre expirarea documentelor.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setupNotificationWorker()

        setContent {
            MyCarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLogout = { logout() }
                    )
                }
            }
        }
    }

    private fun setupNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "ImmediateExpiryCheck",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )

        val periodicRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CarExpiryCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("MyCar", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gestionează mașinile, documentele și istoricul de service.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AndroidView(
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.view_drawable_badge, null)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { context.startActivity(Intent(context, AddCarActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adaugă mașină")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { context.startActivity(Intent(context, CarListActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vezi mașinile")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { context.startActivity(Intent(context, LocalNotesActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Note locale SQLite")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { context.startActivity(Intent(context, VehicleCatalogActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Catalog modele auto")
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Deconectare")
        }
    }
}
