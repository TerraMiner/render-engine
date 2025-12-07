package ua.terra.renderengine.util

/**
 * Converts 2D coordinates to a 1D array index.
 * @param x The x-coordinate (column)
 * @param y The y-coordinate (row)
 * @param maxX The maximum width (number of columns)
 * @param maxY The maximum height (number of rows)
 * @return The corresponding 1D index
 */
fun getIndex(x: Int, y: Int, maxX: Int, maxY: Int): Int {
    return x.coerceIn(0,maxX-1) + y.coerceIn(0,maxY-1) * maxX
}

/**
 * Converts a 1D array index to 2D coordinates.
 * @param index The 1D array index
 * @param maxX The maximum width (number of columns)
 * @param maxY The maximum height (number of rows)
 * @return A Point containing the x and y coordinates
 */
fun getCoords(index: Int, maxX: Int, maxY: Int): Point<Int> {
    return Point((index % maxX).coerceIn(0, maxX - 1), (index / maxX).coerceIn(0, maxY - 1))
}