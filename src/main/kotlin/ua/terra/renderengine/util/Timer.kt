package ua.terra.renderengine.util

class Timer(ticksPerSecond: Int) {
    var partialTick: Float = 0f
        private set

    private var tickDelta: Float = 0f
    private var lastMs: Long = System.currentTimeMillis()
    private val msPerTick: Float = 1000.0f / ticksPerSecond

    fun advanceTime(currentMs: Long): Int {
        val deltaMs = (currentMs - lastMs).toFloat()
        lastMs = currentMs

        tickDelta += deltaMs / msPerTick

        val ticks = tickDelta.toInt()
        tickDelta -= ticks.toFloat()

        partialTick = tickDelta

        return ticks
    }
}