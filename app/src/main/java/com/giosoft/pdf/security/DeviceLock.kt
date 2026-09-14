package com.giosoft.pdf.security

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "DeviceLock"

/**
 * Desbloqueo con huella, rostro, PIN o contrasena del celular.
 *
 * Se usa la API de la plataforma y no androidx.biometric: la version estable
 * de esa libreria es de 2021 y obliga a que la Activity sea una FragmentActivity,
 * mientras que aqui basta con lo que ya trae Android 11 en adelante.
 *
 * **La app nunca ve ni guarda la huella ni el PIN.** Solo recibe del sistema un
 * "si" o un "no"; las credenciales no salen del hardware seguro del celular.
 */
object DeviceLock {

    /**
     * Acepta huella/rostro y, como alternativa, el PIN o la contrasena del
     * celular. Incluir la credencial del dispositivo evita dejar fuera a quien
     * no tenga huella registrada o tenga el sensor sucio.
     */
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** `true` si el celular tiene alguna forma de desbloqueo configurada. */
    fun isAvailable(activity: Activity): Boolean = runCatching {
        val manager = activity.getSystemService(BiometricManager::class.java)
            ?: return@runCatching false
        manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }.onFailure {
        // Nunca debe tumbar la app: si no se puede consultar, simplemente no
        // se ofrece la proteccion.
        Log.w(TAG, "No se pudo consultar el desbloqueo del dispositivo", it)
    }.getOrDefault(false)

    /**
     * Pide desbloquear y espera la respuesta.
     *
     * Devuelve `true` solo si el sistema confirma la identidad. Cancelar,
     * fallar o cerrar el dialogo devuelven `false`, nunca una excepcion: el
     * llamante solo tiene que decidir si abre o no.
     */
    suspend fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation { cancellation.cancel() }

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            // Con DEVICE_CREDENTIAL permitido no se debe poner boton negativo:
            // el propio sistema ofrece la opcion de usar el PIN.
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setConfirmationRequired(false)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(code: Int, message: CharSequence) {
                Log.d(TAG, "Desbloqueo cancelado o fallido ($code): $message")
                if (continuation.isActive) continuation.resume(false)
            }

            // onAuthenticationFailed (huella no reconocida) NO se resuelve aqui:
            // el sistema deja reintentar dentro del mismo dialogo.
        }

        runCatching {
            prompt.authenticate(cancellation, activity.mainExecutor, callback)
        }.onFailure {
            Log.w(TAG, "No se pudo mostrar el dialogo de desbloqueo", it)
            if (continuation.isActive) continuation.resume(false)
        }
    }
}
