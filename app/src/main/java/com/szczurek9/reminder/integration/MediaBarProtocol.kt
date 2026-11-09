package com.szczurek9.reminder.integration

/**
 * Stałe protokołu mostu (v1) – identyczne jak ReminderProtocol w Szczur Media Bar >= 1.1.0.
 */
object MediaBarProtocol {
    const val PROTOCOL_VERSION = 1

    const val PACKAGE = "com.szczurek9.mediabar"
    const val SERVICE_CLASS = "com.szczurek9.mediabar.integration.ReminderBridgeService"
    const val ACTION_BIND = "com.szczurek9.mediabar.action.REMINDER_BRIDGE"

    const val MSG_HELLO = 1
    const val MSG_HELLO_REPLY = 2
    const val MSG_SHOW = 3
    const val MSG_SHOW_RESULT = 4
    const val MSG_HIDE = 5

    const val K_PROTO = "proto"
    const val K_MB_VERSION = "mb_version"
    const val K_CAN_SHOW = "can_show"
    const val K_REASON = "reason"
    const val K_ID = "id"
    const val K_TITLE = "title"
    const val K_DESCRIPTION = "description"
    const val K_WHEN = "when"
    const val K_DISPLAY_SEC = "display_sec"
    const val K_DONE_PI = "done_pi"
    const val K_OPEN_PI = "open_pi"
    const val K_SNOOZE_PIS = "snooze_pis"
    const val K_SNOOZE_LABELS = "snooze_labels"
    const val K_STATUS = "status"

    const val STATUS_SHOWN = 0
    const val STATUS_UNAVAILABLE = 1
    const val STATUS_REJECTED = 2
}
