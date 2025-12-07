package ua.terra.renderengine.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * A cooldown timer that tracks when an action can be performed again.
 * @property cooldownMs The cooldown duration in milliseconds
 */
class Cooldown(
    private var start: Long,
    private var delay: Long
) : Cloneable {

    constructor() : this(0L, 0L)
    constructor(delay: Long) : this(0L, delay)
    constructor(start: Boolean) : this() {
        if (start) start()
    }
    constructor(delay: Long, start: Boolean) : this(0, delay) {
        if (start) start()
    }

    /**
     * Checks if required time has passed since start.
     * @return true if time expired, false if cooldown still active
     */
    fun isEnded() = !isPaused() || start + delay < System.currentTimeMillis()

    /**
     * Returns remaining time in milliseconds.
     */
    fun remain() = if (isPaused()) delay else start + delay - System.currentTimeMillis()

    /**
     * Checks if cooldown is paused.
     */
    fun isPaused() = start == 0L

    /**
     * Returns absolute expiration time.
     */
    fun endTime() = start + delay

    /**
     * Returns elapsed time since start.
     */
    fun elapsed() = System.currentTimeMillis() - start

    /**
     * Starts cooldown. Records current time.
     */
    fun start(): Cooldown {
        start = System.currentTimeMillis()
        return this
    }

    /**
     * Stops cooldown and returns it to initial state.
     */
    fun stop(): Cooldown {
        start = 0L
        return this
    }

    /**
     * Adds additional time to cooldown.
     * If cooldown already expired, restarts it with new time.
     */
    fun addDelay(value: Long): Cooldown {
        if (isEnded()) {
            start()
            delay = value
        } else delay += value
        return this
    }

    /**
     * Sets new cooldown time.
     */
    fun setDelay(value: Long): Cooldown {
        delay = value
        return this
    }

    /**
     * Returns configured cooldown time.
     */
    fun getDelay() = delay

    /**
     * Pauses cooldown.
     * Remaining time is saved and can be resumed.
     */
    fun pause(): Cooldown {
        setDelay(remain())
        stop()
        return this
    }

    /**
     * Returns remaining time formatted as "MM:SS" or "HH:MM:SS".
     */
    fun format() = format(remain().coerceAtLeast(0))

    /**
     * Returns expiration time as date string.
     */
    fun formatEndTime() = formatDate(endTime(), "dd.MM.yyyy HH:mm")

    public override fun clone() = Cooldown(start, delay)

    companion object {
        /**
         * Formats time in milliseconds to string "MM:SS" or "HH:MM:SS".
         * @param time Time in milliseconds
         * @param trim If true, omits hours when zero
         * @return Formatted time string
         */
        fun format(time: Long, trim: Boolean = true) = StringJoiner(":").apply {
            val totalSeconds = time.milliseconds.inWholeMilliseconds / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            if (!trim || hours > 0) add(hours.twoSym)
            add(minutes.twoSym)
            add(seconds.twoSym)
        }.toString()

        /**
         * Formats timestamp as date string.
         * @param time Timestamp in milliseconds
         * @param format Date format pattern
         * @return Formatted date string
         */
        fun formatDate(time: Long, format: String = "dd.MM.yyyy HH:mm") = SimpleDateFormat(format).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }.format(Date(time))

        /** Formats number to two-digit string with leading zero if needed */
        private val Long.twoSym get() = if (this < 10) "0$this" else "$this"
    }
}
