package com.giosoft.pdf.print

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import java.io.FileOutputStream

private const val TAG = "PdfPrinter"

/**
 * Impresion mediante el servicio del sistema.
 *
 * El PDF ya esta paginado, asi que no hay que renderizarlo: basta con volcar el
 * archivo tal cual al descriptor que entrega el servicio de impresion. Eso
 * conserva el documento exactamente como viene, que es justo lo que se busca.
 */
object PdfPrinter {

    fun print(context: Context, uri: Uri, documentName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Log.w(TAG, "El dispositivo no expone el servicio de impresion")
            return
        }
        printManager.print(
            documentName,
            PdfDocumentAdapter(context, uri, documentName),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build(),
        )
    }

    private class PdfDocumentAdapter(
        private val context: Context,
        private val uri: Uri,
        private val documentName: String,
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(documentName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    if (input == null) {
                        callback.onWriteFailed("No se pudo leer el documento")
                        return
                    }
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                } else {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al enviar el documento a la impresora", e)
                callback.onWriteFailed(e.message)
            }
        }
    }
}
