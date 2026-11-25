package com.example.foodloop.scanner

import java.text.SimpleDateFormat
import java.util.Locale

object ExpirationDateParser {

    private val datePatterns = listOf(

        Regex("""\b\d{4}[-./]\d{1,2}[-./]\d{1,2}\b"""),

        Regex("""\b\d{1,2}[-./]\d{1,2}[-./]\d{4}\b"""),

        Regex("""\b\d{1,2}[-./]\d{1,2}[-./]\d{2}\b""")

    )

    private val expirationKeywords = listOf(
        "MHD",
        "MINDESTENS HALTBAR",
        "HALTBAR BIS",
        "VERBRAUCHEN BIS",
        "EXP",
        "EXPIRY",
        "BEST BEFORE",
        "USE BY",
        "VALABIL PANA",
        "VALABIL PÂNĂ",
        "EXPIRA"
    )

    fun extractDates(text: String): List<String> {

        val dates = mutableListOf<String>()

        for (pattern in datePatterns) {

            for (match in pattern.findAll(text)) {

                val normalizedDate = normalizeDate(match.value)

                if (normalizedDate != null) {
                    dates.add(normalizedDate)
                }
            }
        }

        return dates.distinct()
    }

    fun findExpirationDate(text: String): String? {

        val lines = text.lines()

        val preferredDates = mutableListOf<String>()

        for (index in lines.indices) {

            val line = lines[index].uppercase(Locale.ROOT)

            val containsKeyword = expirationKeywords.any {
                line.contains(it)
            }

            if (containsKeyword) {

                val dates = extractDates(lines[index])

                if (dates.isNotEmpty()) {

                    preferredDates.addAll(dates)

                } else if (index + 1 < lines.size) {

                    preferredDates.addAll(
                        extractDates(lines[index + 1])
                    )
                }
            }
        }

        val uniquePreferredDates = preferredDates.distinct()

        if (uniquePreferredDates.isNotEmpty()) {

            return uniquePreferredDates.singleOrNull()

        }

        return extractDates(text).singleOrNull()
    }

    private fun normalizeDate(date: String): String? {

        val parts = date.split('-', '.', '/')

        if (parts.size != 3) {
            return null
        }

        val day: Int
        val month: Int
        var year: Int

        if (parts[0].length == 4) {

            year = parts[0].toIntOrNull() ?: return null
            month = parts[1].toIntOrNull() ?: return null
            day = parts[2].toIntOrNull() ?: return null

        } else {

            day = parts[0].toIntOrNull() ?: return null
            month = parts[1].toIntOrNull() ?: return null
            year = parts[2].toIntOrNull() ?: return null

            if (parts[2].length == 2) {
                year += 2000
            }
        }

        if (year !in 2000..2099) {
            return null
        }

        val normalizedDate = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            year,
            month,
            day
        )

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        formatter.isLenient = false

        return try {

            if (formatter.parse(normalizedDate) != null) {
                normalizedDate
            } else {
                null
            }

        } catch (exception: Exception) {

            null

        }
    }
}
