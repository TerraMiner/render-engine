package ua.terra.renderengine.window

/**
 * Field for measuring execution time of code blocks.
 */
class MeasurementField {
    private var field: Double = 0.0

    /**
     * Measures execution time of the provided block.
     * @param block Code block to measure
     */
    fun measure(block: () -> Unit) {
        val start = System.nanoTime()
        block()
        field = (System.nanoTime() - start) / 1_000_000.0
    }

    /**
     * Returns the last measured value in milliseconds.
     */
    fun getValue() = field

    override fun toString() = getValue().toString()
}

