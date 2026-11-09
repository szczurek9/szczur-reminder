package com.szczurek9.reminder.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

sealed interface MediaBarStatus {
    data object NotInstalled : MediaBarStatus

    /** Zainstalowany, ale nie da się połączyć (np. inny podpis, brak uprawnienia mostu, starsza wersja bez mostu). */
    data class Unreachable(val detail: String) : MediaBarStatus

    data class Incompatible(val mediaBarProtocol: Int) : MediaBarStatus

    /** [canShow] = false oznacza, że Media Bar odebrał połączenie, ale chwilowo nie pokaże paska ([reason]). */
    data class Available(val canShow: Boolean, val reason: String?, val versionCode: Long) : MediaBarStatus
}

data class ShowResult(val status: Int, val reason: String?) {
    val shown: Boolean get() = status == MediaBarProtocol.STATUS_SHOWN
}

/**
 * Klient mostu do Szczur Media Bar. Każde wywołanie to krótka sesja: bind -> wiadomość -> odpowiedź -> unbind.
 * Nigdy nie rzuca wyjątku do wywołującego – brak Media Bar to normalny stan (wtedy działają zwykłe powiadomienia).
 */
class MediaBarClient(private val context: Context) {

    fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(MediaBarProtocol.PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    suspend fun probe(): MediaBarStatus {
        if (!isInstalled()) return MediaBarStatus.NotInstalled
        val reply = request(MediaBarProtocol.MSG_HELLO, Bundle(), MediaBarProtocol.MSG_HELLO_REPLY, TIMEOUT_PROBE_MS)
            ?: return MediaBarStatus.Unreachable("Brak połączenia z mostem (zła wersja lub podpis Media Bar, brak uprawnienia)")
        val proto = reply.getInt(MediaBarProtocol.K_PROTO, 0)
        if (proto != MediaBarProtocol.PROTOCOL_VERSION) return MediaBarStatus.Incompatible(proto)
        return MediaBarStatus.Available(
            canShow = reply.getBoolean(MediaBarProtocol.K_CAN_SHOW, false),
            reason = reply.getString(MediaBarProtocol.K_REASON),
            versionCode = reply.getLong(MediaBarProtocol.K_MB_VERSION, 0L),
        )
    }

    /** null = Media Bar nieosiągalny (traktuj jak niedostępny). */
    suspend fun show(payload: Bundle): ShowResult? {
        if (!isInstalled()) return null
        val reply = request(MediaBarProtocol.MSG_SHOW, payload, MediaBarProtocol.MSG_SHOW_RESULT, TIMEOUT_SHOW_MS) ?: return null
        return ShowResult(
            status = reply.getInt(MediaBarProtocol.K_STATUS, MediaBarProtocol.STATUS_UNAVAILABLE),
            reason = reply.getString(MediaBarProtocol.K_REASON),
        )
    }

    /** Best effort: chowa pasek, jeśli pokazuje przypomnienie o tym id. */
    suspend fun hide(reminderId: Long) {
        if (!isInstalled()) return
        request(MediaBarProtocol.MSG_HIDE, Bundle().apply { putLong(MediaBarProtocol.K_ID, reminderId) }, null, TIMEOUT_HIDE_MS)
    }

    /** Wysyła wiadomość; gdy [expectReply] != null, czeka na odpowiedź tego typu. */
    private suspend fun request(what: Int, data: Bundle, expectReply: Int?, timeoutMs: Long): Bundle? {
        val connected = CompletableDeferred<IBinder?>()
        val replies = Channel<Bundle>(Channel.UNLIMITED)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) { connected.complete(service) }
            override fun onServiceDisconnected(name: ComponentName) { connected.complete(null) }
            override fun onBindingDied(name: ComponentName) { connected.complete(null) }
            override fun onNullBinding(name: ComponentName) { connected.complete(null) }
        }
        val intent = Intent(MediaBarProtocol.ACTION_BIND)
            .setComponent(ComponentName(MediaBarProtocol.PACKAGE, MediaBarProtocol.SERVICE_CLASS))

        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.w(TAG, "Brak uprawnienia do mostu Media Bar", e)
            false
        }
        try {
            if (!bound) return null
            return withTimeoutOrNull(timeoutMs) {
                val binder = connected.await() ?: return@withTimeoutOrNull null
                val target = Messenger(binder)
                val handler = Handler(Looper.getMainLooper()) { m ->
                    if (expectReply != null && m.what == expectReply) replies.trySend(Bundle(m.data))
                    true
                }
                val msg = Message.obtain(null, what).also {
                    it.data = data
                    if (expectReply != null) it.replyTo = Messenger(handler)
                }
                try {
                    target.send(msg)
                } catch (e: RemoteException) {
                    Log.w(TAG, "send nie powiodło się", e)
                    return@withTimeoutOrNull null
                }
                if (expectReply == null) Bundle.EMPTY else replies.receive()
            }
        } finally {
            runCatching { context.unbindService(connection) }
            replies.close()
        }
    }

    private companion object {
        const val TAG = "MediaBarClient"
        const val TIMEOUT_PROBE_MS = 2_500L
        const val TIMEOUT_SHOW_MS = 3_000L
        const val TIMEOUT_HIDE_MS = 1_500L
    }
}
