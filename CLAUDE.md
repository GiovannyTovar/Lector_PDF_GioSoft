# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

"Lector PDF GioSoft" (`com.giosoft.lectorpdf`): visor de PDF para Android sin publicidad, con historial de archivos abiertos. Java puro (sin Kotlin, sin Compose), Gradle con Groovy DSL, un solo módulo `:app`.

El código, los comentarios, los strings de UI y los mensajes de commit están en **español**. Mantener ese idioma al editar.

## Comandos

Desde la raíz del proyecto (Windows: usar `.\gradlew.bat`, Git Bash: `./gradlew`):

```bash
./gradlew assembleDebug          # Compilar APK debug
./gradlew installDebug           # Compilar e instalar en dispositivo/emulador conectado
./gradlew assembleRelease        # APK release (minify desactivado)
./gradlew test                   # Tests unitarios JVM
./gradlew connectedAndroidTest   # Tests instrumentados (requiere dispositivo)
./gradlew lint                   # Android Lint -> app/build/reports/lint-results-debug.html
./gradlew clean
```

Ejecutar un solo test:
```bash
./gradlew test --tests "com.giosoft.lectorpdf.ExampleUnitTest"
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.giosoft.lectorpdf.ExampleInstrumentedTest
```

Solo existen los tests de plantilla generados por Android Studio; no hay suite real.

Logs útiles en tiempo de ejecución: los tags son `PDF_DEBUG`, `PDF_ADAPTER`, `DELETE`.
```bash
adb logcat -s PDF_DEBUG:D PDF_ADAPTER:E DELETE:W
```

Versionado: `versionCode` / `versionName` se editan a mano en `app/build.gradle`. Los commits de release siguen el patrón `"Version X.Y.Z Funcional. <cambios>"`.

## Arquitectura

Cuatro clases Java, sin capa de dominio ni inyección de dependencias. Toda la lógica vive en la Activity, el Adapter y un manager estático.

**Flujo central — todo PDF se copia antes de abrirse:**

1. El usuario elige un PDF (SAF `ACTION_OPEN_DOCUMENT`) o el sistema envía un `ACTION_VIEW` con mime `application/pdf` (la app está registrada como visor de PDF en el manifest).
2. `MainActivity.copyPdfToExternalStorage()` copia el contenido de la `content://` URI a `getExternalFilesDir(null)` usando el display name como nombre de archivo. Si ya existe un archivo con ese nombre **no se sobreescribe**: se reutiliza la copia previa.
3. La ruta absoluta resultante se guarda en el historial y se pasa a MuPDF.
4. `openPdfWithMuPDF()` lanza `com.artifex.mupdf.viewer.DocumentActivity` por nombre de clase explícito con una `file://` URI y `FLAG_ACTIVITY_NO_HISTORY`.

Consecuencia importante: la **ruta del archivo copiado es la clave de identidad** de un item del historial (deduplicación, borrado, comparación). No es la URI original.

**Persistencia — `model/PdfHistoryManager`:** API estática sobre `SharedPreferences` (`pdf_history_prefs` / clave `pdf_history`), serializando `List<PdfItem>` a JSON con Gson. No hay Room pese a lo que sugiere el README. Límite de `MAX_HISTORY_ITEMS = 50`: al superarlo, `savePdfItem()` descarta los items más antiguos **y borra su archivo físico** de `getExternalFilesDir`. `savePdfItem()` también se usa para "tocar" un item existente (se reinserta con nuevo timestamp al reabrirlo), de modo que actúa como upsert, no solo como insert.

**Agrupación por fecha:** `getPdfHistoryGrouped()` ordena por timestamp descendente y produce `List<PdfGroup>` con títulos "Hoy", "Ayer" o `EEEE, d MMMM` en el locale del dispositivo. La UI siempre consume esta forma agrupada.

**`adapter/PdfAdapter`:** RecyclerView de dos tipos de vista (`TYPE_HEADER` / `TYPE_ITEM`) sobre la lista aplanada de grupos. La posición del RecyclerView se traduce a grupo/item recorriendo los grupos y acumulando `items.size() + 1` por cabecera — ver `getItemForPosition()`, `getGroupForPosition()`, `getItemViewType()`. **Cualquier cambio en la estructura de grupos debe mantener esas tres funciones coherentes entre sí**; `getItemForPosition()` devuelve `null` para cabeceras y los callers dependen de ello. Refresco siempre vía `updateData(PdfHistoryManager.getPdfHistoryGrouped(context))`, que hace `notifyDataSetChanged()`.

**Swipe para eliminar:** el `ItemTouchHelper.SimpleCallback` está definido inline en `MainActivity.onCreate()`, incluido su `onChildDraw()` personalizado (fondo rojo + icono). Ignora las cabeceras y, tras el `AlertDialog`, siempre llama a `notifyItemChanged(position)` en `setOnDismissListener` para restaurar la fila sea cual sea la salida del diálogo. El borrado elimina el archivo copiado **y** la entrada del historial.

**Compartir:** vía `FileProvider` con authority `${applicationId}.fileprovider`, configurado en `res/xml/file_paths.xml`.

## Detalles que suelen sorprender

- `MainActivity` y `PdfAdapter` importan `com.artifex.mupdf.viewer.BuildConfig`, no el `BuildConfig` de la app. Funciona porque `APPLICATION_ID` coincide, pero es frágil: preferir `context.getPackageName()` (como ya hace `PdfAdapter`).
- El modo noche está forzado a `MODE_NIGHT_NO` en `onCreate()` antes de `super.onCreate()`, aunque existe `values-night/themes.xml`.
- MuPDF viene de un repositorio Maven propio (`https://maven.ghostscript.com`) declarado en `settings.gradle`, con rango de versión abierto `1.15.+`.
- Dependencias duplicadas en `app/build.gradle`: `appcompat` y `material` se declaran tanto por version catalog (`libs.*`) como con coordenadas literales de versión menor.
- Java 17 como source/target; `compileSdk`/`targetSdk` 34, `minSdk` 28.
- Dexter está en las dependencias pero no se usa en el código; el manifest no declara permisos (el acceso es solo por SAF).
