package com.example.xinqiao.util.text

import kotlin.math.min

object TextMatchUtils {
    private fun latinize(s: String): String {
        return try {
            val cls = Class.forName("android.icu.text.Transliterator")
            val getInstance = cls.getMethod("getInstance", String::class.java)
            val inst = getInstance.invoke(null, "Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")
            val transform = cls.getMethod("transliterate", String::class.java)
            (transform.invoke(inst, s) as String).lowercase().replace(" ", "")
        } catch (_: Throwable) {
            s.lowercase()
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    fun containsFuzzy(source: String, query: String): Boolean {
        if (query.isBlank()) return true
        val s = source.lowercase()
        val q = query.lowercase()
        if (s.contains(q)) return true
        val sl = latinize(source)
        val ql = latinize(query)
        if (sl.contains(ql)) return true
        val dist = levenshtein(sl, ql)
        val thr = 1 + ql.length / 3
        return dist <= thr
    }
}

