# 📘 PDF GioSoft

Visor de PDF para Android **sin publicidad, sin rastreo y sin permisos de almacenamiento**.

Creada para amigos, familiares y conocidos, con el objetivo de manejar sus documentos de forma rápida, privada y sin interrupciones.

---

## ✨ Características

- 📂 **Abre tus archivos originales**, estén donde estén: Descargas, documentos de WhatsApp, Drive… La app **no hace copias** de lo que ya tienes guardado.
- 💾 **Los PDF que te comparten no se pierden**: los que llegan por WhatsApp o correo traen un acceso temporal, así que se conservan solos en `Documentos/Mis PDF`. Si reabres el mismo, se reutiliza la copia en lugar de duplicarla.
- ✏️ **Renombra el documento real** en tu celular, no una copia.
- 📜 **Desplazamiento vertical y continuo** entre páginas.
- 🔖 **Retoma la lectura** en la página donde la dejaste.
- 🔐 **PDF protegidos con contraseña**, con un diálogo propio y claro.
- 📷 **Escanea fotos y crea un PDF**: detección de bordes, recorte, filtros y varias páginas.
- 🖨️ **Imprime** desde el propio visor.
- 🔍 **Busca** en tu historial, marca **favoritos** y agrupa por fecha.
- 📤 **Comparte** por WhatsApp, correo o cualquier app.
- 🌙 **Tema claro y oscuro**, siguiendo al sistema.
- 🗑️ Quitar del historial **nunca borra el archivo** de tu celular.

---

## 🔒 Privacidad

Todo ocurre en tu celular:

- **Cero permisos peligrosos.** La app no declara ningún permiso en el manifiesto. Los archivos se abren con el selector del sistema, que concede acceso archivo por archivo.
- **Nada sale del dispositivo.** No hay servidores, ni analítica, ni publicidad.
- Los PDF se procesan en un **proceso aislado**, de modo que un documento malformado no puede afectar al resto de la app.

---

## 🛠️ Tecnologías

- **Kotlin** y **Jetpack Compose** (Material 3)
- **androidx.pdf** como motor de visualización — el mismo renderizador del sistema que usa Android
- **Room** para el historial
- **ML Kit Document Scanner** para el escaneo
- **Storage Access Framework** para acceder a los archivos originales

Requiere **Android 12** o superior.

---

## 🚀 Compilar

```bash
git clone https://github.com/GiovannyTovar/Lector_PDF_GioSoft.git
cd Lector_PDF_GioSoft
./gradlew assembleDebug
```

No hacen falta llaves de firma para compilar. Para generar el paquete de Play, ver [`PUBLICACION.md`](PUBLICACION.md).

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
