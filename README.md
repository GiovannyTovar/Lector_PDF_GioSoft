# 📘 PDF GioSoft

Visor de PDF para Android **sin publicidad, sin rastreo y sin permisos de almacenamiento**.

Creada para amigos, familiares y conocidos, con el objetivo de manejar sus documentos de forma rápida, privada y sin interrupciones.

---

## ✨ Características

- 📂 **Abre tus archivos originales**, estén donde estén: Descargas, documentos de WhatsApp, Drive… La app **no hace copias** de lo que ya tienes guardado.
- 💾 **Los PDF que te comparten no se pierden**: los que llegan por WhatsApp o correo traen un acceso temporal, así que se conservan solos en `Documentos/Mis PDF`. Si reabres el mismo, se reutiliza la copia en lugar de duplicarla.
- ✏️ **Renombra el documento real** en tu celular, no una copia — incluso cuando el proveedor del archivo no lo permite (ver [§6 de la arquitectura](docs/ARQUITECTURA.md)).
- 🗂️ **Organiza por categorías** de colores y marca **favoritos**.
- 🔐 **Dos protecciones distintas**: con **huella** (dentro de la app, en este celular) o con **contraseña dentro del PDF** (viaja con el archivo, se la piden a quien lo reciba).
- 📜 **Desplazamiento vertical y continuo** entre páginas, y **retoma la lectura** donde la dejaste.
- 🔍 **Busca dentro del documento** y también en tu historial.
- 📷 **Escanea papeles y crea un PDF**: detección de bordes, recorte, filtros y varias páginas.
- 🖨️ **Imprime** desde el propio visor y **comparte** por cualquier app.
- 🌙 **Tema claro y oscuro.**
- 🌎 **Español, inglés, francés y portugués.**
- 🗑️ Quitar del historial **nunca borra el archivo** de tu celular.

---

## 🔒 Privacidad

Todo ocurre en tu celular:

- **Ningún permiso peligroso.** El único que declara la app es el de biometría, que Android clasifica como *normal* y que solo sirve para pedirte la huella cuando tú activas esa protección. Los archivos se abren con el selector del sistema, que concede acceso archivo por archivo.
- **Tus documentos no salen del dispositivo.** No hay cuentas, ni servidores propios, ni analítica, ni publicidad.
- **Con transparencia:** el escáner es un componente de Google (ML Kit) y arrastra una librería de telemetría propia que añade permiso de Internet al paquete final y puede enviarle a Google datos técnicos de su propio funcionamiento —no tus documentos—. Está explicado en la [política de privacidad](web/privacidad.html), sección 8.
- Los PDF se procesan en un **proceso aislado**, de modo que un documento malformado no puede afectar al resto de la app.

---

## 🛠️ Tecnologías

- **Kotlin** y **Jetpack Compose** (Material 3)
- **androidx.pdf** como motor de visualización — el mismo renderizador del sistema que usa Android
- **Room** para el historial, **DataStore** para las preferencias
- **ML Kit Document Scanner** para el escaneo
- **PDFBox** para poner y quitar la contraseña del archivo
- **Storage Access Framework** y **MediaStore** para acceder a los archivos originales

Requiere **Android 12** o superior.

---

## 🚀 Compilar

```bash
git clone https://github.com/GiovannyTovar/PDF-GioSoft.git
cd PDF-GioSoft
./gradlew assembleDebug
```

No hacen falta llaves de firma para compilar: sin `keystore.properties`, el paquete de release sale sin firmar y el build **no falla**, a propósito. Para generar el paquete de Play, ver [`PUBLICACION.md`](PUBLICACION.md).

---

## 📚 Documentación

| Documento | Para quién |
|---|---|
| [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) | Quien vaya a tocar el código: qué se usa, por qué se descartaron MuPDF y PDFium, cómo funciona cada pieza y con qué trampas se topará |
| [`web/ayuda.html`](web/ayuda.html) | Quien usa la app: guía de uso y preguntas frecuentes |
| [`web/privacidad.html`](web/privacidad.html) · [`web/terminos.html`](web/terminos.html) | Páginas públicas que exige Google Play |
| [`PUBLICACION.md`](PUBLICACION.md) | Firmar y subir a Play, explicado desde cero |
| [`play/README.md`](play/README.md) | Los gráficos de la ficha de Play y cómo se regeneran |

---

## 📜 Historial de versiones

La versión 5.0 es una reescritura completa (Java → Kotlin, MuPDF → androidx.pdf) que resolvió el problema de las copias y redujo el tamaño de la app de **27 MB a ~4 MB**.

La versión 4.1.0 original se conserva en el tag [`v4.1.0-java`](../../tree/v4.1.0-java) y en la rama `legado-java-v4.1.0`.

---

## 📄 Licencia

MIT. Ver [LICENSE](LICENSE).

Las dependencias son compatibles: AndroidX y ML Kit bajo Apache 2.0. *(La versión 4.x usaba MuPDF, bajo AGPL v3, lo que era incompatible con esta licencia; ese fue uno de los motivos del cambio de motor.)*

---

Desarrollado con ❤️ por Giovanny Tovar — para quienes valoran la simplicidad, la privacidad y el control sobre sus archivos.
