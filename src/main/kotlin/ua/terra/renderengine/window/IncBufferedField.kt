package ua.terra.renderengine.window

/**
 * Buffered counter that accumulates increments and flushes periodically.
 */
class IncBufferedField {
    private var incremental = 0
    private var field: Int = 0

    /**
     * Increments the counter.
     */
    fun increase() {
        incremental++
    }

    /**
     * Flushes accumulated value to the field and resets the counter.
     */
    fun flush() {
        field = incremental
        incremental = 0
    }

    /**
     * Returns the last flushed value.
     */
    fun getValue() = field

    override fun toString() = getValue().toString()
}

