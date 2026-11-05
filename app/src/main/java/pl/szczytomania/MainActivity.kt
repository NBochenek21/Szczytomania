package pl.szczytomania

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.ui.navigation.AppNavigation
import pl.szczytomania.ui.screens.auth.AuthScreen
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    // Aplikacja jest tylko po polsku – także na telefonie z innym językiem odmiana (plurals)
    // i formaty mają być polskie.
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(Locale.forLanguageTag("pl")) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SzczytomaniaTheme {
                // Niezalogowany widzi ekran logowania, zalogowany – mapę z menu. Wartość początkowa
                // z FirebaseAuth, żeby po starcie aplikacji nie mignął ekran logowania.
                val authRepository = remember { AuthRepository() }
                val user by authRepository.currentUser
                    .collectAsStateWithLifecycle(initialValue = FirebaseAuth.getInstance().currentUser)

                if (user == null) AuthScreen() else AppNavigation()
            }
        }
    }
}
