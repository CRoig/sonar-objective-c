/*
 * Copyright (c) 2018 Tobias Raatiniemi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

internal fun headerTemplate(): String {
    return """
        Available issues:

        OCLint
        ======

    """.trimIndent()
}

internal fun ruleTemplate(it: Rule): String {
    return "${it.name.toLowerCase()}\n----------\n\nSummary: Name: ${it.name.toLowerCase()}\n${it.description}\n\nSeverity: ${it.severity}\nCategory: OCLint\n"
}
