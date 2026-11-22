package pl.szczytomania.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/** Czy telefon ma teraz internet. */
fun Context.isOnline(): Boolean {
    val connectivity = getSystemService<ConnectivityManager>() ?: return true
    val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Nasłuch zmian połączenia: [onChange] dostaje true/false przy każdej zmianie (w wątku sieci –
 * wywołujący przenosi to, co trzeba, na główny wątek). Zwraca funkcję kończącą nasłuch.
 */
fun Context.watchConnectivity(onChange: (online: Boolean) -> Unit): () -> Unit {
    val connectivity = getSystemService<ConnectivityManager>() ?: return {}
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            onChange(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        override fun onLost(network: Network) = onChange(false)
    }
    connectivity.registerDefaultNetworkCallback(callback)
    return { connectivity.unregisterNetworkCallback(callback) }
}

/** Stan połączenia dla ekranu – zmienia się na bieżąco (np. przy włączeniu trybu samolotowego). */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val online = remember { mutableStateOf(context.isOnline()) }
    DisposableEffect(context) {
        val stop = context.watchConnectivity { online.value = it }
        onDispose { stop() }
    }
    return online
}
