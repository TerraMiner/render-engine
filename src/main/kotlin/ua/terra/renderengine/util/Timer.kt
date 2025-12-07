package ua.terra.renderengine.util

/**
 * Timer utility for tracking game ticks and partial tick interpolation.
 * Used for smooth rendering between game logic updates.
 * @property ticksPerSecond Target number of ticks per second
 */
class Timer(ticksPerSecond: Int) {
    var partialTick: Float = 0f
        private set
    var tickCount: Int = 0
        internal set

    private var tickDelta: Float = 0f
    private var lastMs: Long = System.currentTimeMillis()
    private val msPerTick: Float = 1000.0f / ticksPerSecond

    /** The elapsed time in milliseconds since the timer started or was last reset */
    val elapsed: Long
        get() = System.currentTimeMillis() - lastMs

    /**
     * Advances the timer and returns the number of ticks that should be processed.
     * Updates the partial tick value for interpolation.
     * @param currentMs Current time in milliseconds
     * @return Number of ticks to process
     */
    fun advanceTime(currentMs: Long): Int {
        val deltaMs = (currentMs - lastMs).toFloat()
        lastMs = currentMs

        tickDelta += deltaMs / msPerTick

        val ticks = tickDelta.toInt()
        tickDelta -= ticks.toFloat()

        partialTick = tickDelta

        return ticks
    }

    /** Resets the timer to zero */
    fun reset() {
        lastMs = System.currentTimeMillis()
    }
}