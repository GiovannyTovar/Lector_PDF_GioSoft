# Reglas de R8 para el build de release (minifyEnabled true).
#
# Room, Compose, androidx.pdf y ML Kit traen sus propias reglas embebidas en
# los artefactos (consumer rules), asi que aqui solo va lo especifico de la app.

# --- Entidades de Room ---
# Los nombres de campo de la entidad son las columnas de la base de datos y el
# codigo que genera Room las referencia por nombre.
-keep class com.giosoft.lectorpdf.data.db.** { *; }

# --- Excepciones que la app distingue por tipo ---
# El visor decide que mostrar segun la excepcion exacta que lanza el cargador:
# PdfPasswordException pide contrasena, mientras que otra SecurityException
# indica que el dispositivo no sabe descifrar. Si R8 fusionara estas clases,
# el visor mostraria el mensaje equivocado.
-keep class androidx.pdf.PdfPasswordException { *; }

# --- Codigo nativo (JNI) ---
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Numeros de linea legibles en los informes de fallos de Play ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- PDFBox (cifrado de PDF con contrasena) ---
# Referencia un codec JPEG2000 opcional que no se incluye: solo hace falta para
# imagenes JPX dentro de un PDF, y cifrar no las toca.
-dontwarn com.gemalto.jp2.**

# El cifrado resuelve algoritmos y filtros por nombre, asi que esas clases no
# se pueden renombrar ni eliminar.
-keep class com.tom_roush.pdfbox.pdmodel.encryption.** { *; }
-keep class com.tom_roush.pdfbox.filter.** { *; }
-keep class com.tom_roush.pdfbox.cos.** { *; }
-dontwarn com.tom_roush.pdfbox.**
