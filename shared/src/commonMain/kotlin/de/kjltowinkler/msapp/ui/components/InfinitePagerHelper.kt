package de.kjltowinkler.msapp.ui.components

/**
 * Hilfsfunktionen für endloses (zyklisches) Swipen in einem HorizontalPager mit ButtonGroup.
 */
object InfinitePagerHelper {
    private const val VIRTUAL_CYCLES = 1000

    fun virtualPageCount(realCount: Int): Int =
        if (realCount > 1) realCount * VIRTUAL_CYCLES else realCount

    fun initialVirtualPage(realCount: Int, initialRealIndex: Int = 0): Int {
        if (realCount <= 1) return 0
        val center = virtualPageCount(realCount) / 2
        val alignedCenter = center - (center % realCount)
        return alignedCenter + initialRealIndex.coerceIn(0, realCount - 1)
    }

    fun realIndex(virtualPage: Int, realCount: Int): Int {
        if (realCount <= 0) return 0
        return ((virtualPage % realCount) + realCount) % realCount
    }

    fun nearestVirtualTarget(currentVirtualPage: Int, targetRealIndex: Int, realCount: Int): Int {
        if (realCount <= 1) return 0
        val currentReal = realIndex(currentVirtualPage, realCount)
        return currentVirtualPage + (targetRealIndex - currentReal)
    }
}
