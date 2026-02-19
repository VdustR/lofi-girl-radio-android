package com.vdustr.lofiradio.util

/**
 * Returns a relevance score for fuzzy matching [query] against [target].
 * Returns 0 if the query does not match at all.
 *
 * Scoring:
 * - Consecutive character matches are rewarded (streak bonus)
 * - Earlier match positions score higher
 * - Substring containment gets a large bonus
 */
fun fuzzyMatchScore(query: String, target: String): Int {
    if (query.isBlank()) return 1
    val lq = query.lowercase()
    val lt = target.lowercase()

    // Substring containment — strong signal
    if (lt.contains(lq)) {
        val posBonus = (lt.length - lt.indexOf(lq)) // earlier = higher
        return 1000 + posBonus
    }

    // Fuzzy character-by-character match
    var score = 0
    var queryIndex = 0
    var streak = 0
    for ((i, char) in lt.withIndex()) {
        if (queryIndex < lq.length && char == lq[queryIndex]) {
            queryIndex++
            streak++
            score += streak + (lt.length - i) // streak bonus + position bonus
        } else {
            streak = 0
        }
    }

    return if (queryIndex == lq.length) score else 0
}
