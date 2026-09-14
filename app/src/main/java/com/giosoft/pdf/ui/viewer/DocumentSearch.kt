package com.giosoft.pdf.ui.viewer

import androidx.pdf.Highlight
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect

/** Una coincidencia concreta dentro del documento. */
data class SearchMatch(
    val pageNumber: Int,
    val rects: List<PdfRect>,
) {
    /** Punto al que desplazar el visor para dejar la coincidencia a la vista. */
    val anchor: PdfPoint?
        get() = rects.firstOrNull()?.let { PdfPoint(it.pageNum, it.left, it.top) }
}

/** Estado de la busqueda dentro del documento abierto. */
data class SearchState(
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val currentIndex: Int = 0,
    val searching: Boolean = false,
    val completed: Boolean = false,
) {
    val total: Int get() = matches.size
    val hasResults: Boolean get() = matches.isNotEmpty()
    /** Numero de coincidencia que se muestra, empezando en 1. */
    val humanIndex: Int get() = if (matches.isEmpty()) 0 else currentIndex + 1
    val current: SearchMatch? get() = matches.getOrNull(currentIndex)
}

/**
 * Busca [query] en todo el documento.
 *
 * androidx.pdf devuelve las coincidencias agrupadas por pagina; aqui se
 * aplanan a una lista ordenada para poder recorrerlas con los botones de
 * anterior y siguiente.
 */
suspend fun PdfDocument.findMatches(query: String): List<SearchMatch> {
    if (query.isBlank()) return emptyList()

    val porPagina = searchDocument(query, 0 until pageCount)
    val resultado = mutableListOf<SearchMatch>()

    for (i in 0 until porPagina.size()) {
        val pagina = porPagina.keyAt(i)
        porPagina.valueAt(i).forEach { match ->
            val rects = match.bounds.map { bounds ->
                PdfRect(pagina, bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
            if (rects.isNotEmpty()) {
                resultado += SearchMatch(pageNumber = pagina, rects = rects)
            }
        }
    }
    return resultado
}

/**
 * Resaltados que se pintan sobre el documento.
 *
 * La coincidencia activa va en un tono mas fuerte para distinguirla del resto
 * mientras se recorren los resultados.
 */
fun SearchState.toHighlights(activeColor: Int, otherColor: Int): List<Highlight> =
    matches.flatMapIndexed { index, match ->
        val color = if (index == currentIndex) activeColor else otherColor
        match.rects.map { Highlight(it, color) }
    }
