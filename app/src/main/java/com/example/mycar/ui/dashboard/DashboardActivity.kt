package com.example.mycar.ui.dashboard

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.mycar.ui.auth.LoginActivity
import com.example.mycar.ui.car.AddCarActivity
import com.example.mycar.ui.car.CarListActivity
import com.example.mycar.ui.theme.MyCarTheme
import com.example.mycar.worker.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class DashboardActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Atenție: Fără permisiune nu vei primi alerte despre expirarea documentelor.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cerem permisiunea pentru notificări pe Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Pornim verificarea periodică în fundal
        setupNotificationWorker()

        setContent {
            MyCarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLogout = {
                            logout()
                        }
                    )
                }
            }
        }
    }

    private fun setupNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Verificare imediată (One-Time) la deschiderea aplicației
        val immediateRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniqueWork(
            "ImmediateExpiryCheck",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )

        // 2. Programăm verificarea periodică (Periodic) la 24h
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
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "🚗 MyCar Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Bine ai venit în aplicație!")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(context, AddCarActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Adaugă mașină")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(context, CarListActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Vezi mașinile")
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Deconectare")
        }
    }
}
