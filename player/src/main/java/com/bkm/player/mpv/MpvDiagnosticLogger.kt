package com.bkm.player.mpv

/**
 * Lightweight diagnostic sink so `:player` can forward key libmpv lifecycle,
 * decoder, and error events to the host app's diagnostic log collector without
 * introducing a module dependency on `:app`.
 */
object MpvDiagnosticLogger {
    @Volatile
    var sink: ((tag: String, message: String) -> Unit)? = null

    fun log(tag: String, message: String) {
        sink?.invoke(tag, message)
    }
}
