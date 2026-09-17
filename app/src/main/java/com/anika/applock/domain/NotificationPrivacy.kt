package com.anika.applock.domain

/**
 * Per-app notification privacy modes.
 */
enum class NotificationPrivacy {
    /** Default Android behavior, no interception */
    SHOW_NORMALLY,

    /** Notification appears, but title/text are redacted to a generic placeholder */
    HIDE_CONTENT,

    /** Notification suppressed entirely */
    HIDE_COMPLETELY
}
