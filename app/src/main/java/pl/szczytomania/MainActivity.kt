package pl.szczytomania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.ui.screens.auth.AuthScreen
import pl.szczytomania.ui.screens.map.MapScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // Niezalogowany widzi ekran logowania, zalogowany – mapę. Wartość początkowa
                // z FirebaseAuth, żeby po starcie aplikacji nie mignął ekran logowania.
                val authRepository = remember { AuthRepository() }
                val user by authRepository.currentUser
                    .collectAsStateWithLifecycle(initialValue = FirebaseAuth.getInstance().currentUser)

                if (user == null) AuthScreen() else MapScreen()
            }
        }
    }
}
