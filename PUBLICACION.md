# Publicar en Google Play

Guía para generar la llave de firma y subir la app. Lo único que debes hacer tú
es **generar el `.jks` y rellenar `keystore.properties`**; el resto del proyecto
ya está configurado.

---

## 1. Generar el almacén de llaves (una sola vez en la vida de la app)

> ⚠️ **Si pierdes este archivo o su contraseña, no podrás volver a actualizar la
> app publicada, nunca.** Haz copia de seguridad en al menos dos sitios
> (disco externo + gestor de contraseñas / nube privada).

Crea una carpeta FUERA del repositorio, por ejemplo `C:\Users\GIOVANNY\llaves`:

```powershell
mkdir C:\Users\GIOVANNY\llaves
cd C:\Users\GIOVANNY\llaves

keytool -genkeypair -v `
  -keystore lectorpdf-release.jks `
  -storetype PKCS12 `
  -keyalg RSA -keysize 4096 `
  -validity 10000 `
  -alias lectorpdf
```

`keytool` viene con el JDK. Si no lo encuentra, está en el JDK de Android Studio:
`C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`

Te preguntará una contraseña y tus datos (nombre, organización, ciudad, país).
Puedes poner tu nombre y `CO` como país; no se muestran a los usuarios.

## 2. Rellenar keystore.properties

```powershell
cd C:\Users\GIOVANNY\AndroidStudioProjects\LectorPDFGioSoft
copy keystore.properties.template keystore.properties
```

Edita `keystore.properties` con tus valores:

```properties
storeFile=C:/Users/GIOVANNY/llaves/lectorpdf-release.jks
storePassword=LA_QUE_PUSISTE
keyAlias=lectorpdf
keyPassword=LA_QUE_PUSISTE
```

> Usa barras normales `/`, no `\`.
> Este archivo y los `.jks` están en `.gitignore`: no se suben al repositorio.

## 3. Generar el paquete firmado

Google Play exige **Android App Bundle (`.aab`)**, no APK:

```powershell
.\gradlew.bat bundleRelease
```

Resultado en: `app/build/outputs/bundle/release/app-release.aab`

Para comprobar que quedó firmado:

```powershell
.\gradlew.bat bundleRelease --info | Select-String "signing"
```

Si quieres además un APK instalable a mano para probar:

```powershell
.\gradlew.bat assembleRelease
```

## 4. Checklist antes de subir

- [ ] `versionCode` incrementado en `app/build.gradle` (Play rechaza repetidos)
- [ ] `versionName` actualizado
- [ ] Probado el `.aab` en un dispositivo real (con `bundletool` o vía prueba interna de Play)
- [ ] Política de privacidad publicada en una URL accesible
- [ ] Formulario de **Seguridad de los datos** completado en Play Console
- [ ] Capturas de pantalla (mínimo 2, teléfono), icono 512×512, gráfico destacado 1024×500
- [ ] Descripción corta y larga
- [ ] Clasificación de contenido
- [ ] Revisar el bloque de donaciones del diálogo "Acerca de" (ver aviso abajo)

## 5. Avisos a revisar antes de publicar

**Donaciones.** El diálogo "Acerca de" muestra un número de cuenta Nequi. La
política de pagos de Google Play restringe pedir donaciones fuera de su sistema
de facturación; la exención suele aplicar a organizaciones sin ánimo de lucro
registradas, no a personas. **Consulta la política vigente antes de subir**; lo
más seguro es quitarlo o sustituirlo por un enlace externo fuera de la app.

**Firma de apps de Play.** Se recomienda activar *Play App Signing*: subes tu
`.aab` firmado con tu llave de carga y Google guarda la llave de firma real.
Si algún día pierdes tu `.jks`, Google puede ayudarte a recuperar el acceso.

**Permisos.** La app no declara ningún permiso peligroso: los archivos se leen
mediante el selector del sistema (SAF). Esto evita el formulario de
justificación de `MANAGE_EXTERNAL_STORAGE` y acelera la revisión.

---

## Notas técnicas del proyecto

- `compileSdk` **37** (lo exigen las AndroidX recientes; AGP lo descarga solo)
- `targetSdk` **36** · `minSdk` **31** (Android 12)
- AGP **9.4.0**, Gradle **9.7.1**, Java **17**, Kotlin integrado en AGP
- Kotlin + Jetpack Compose; el motor de PDF es `androidx.pdf` (Apache 2.0)
- Release con **R8** (`minifyEnabled`) y `shrinkResources` activos
- **APK de release: ~4,2 MB** (la v4.1.0 con MuPDF pesaba 27 MB)
- Todas las librerías nativas están alineadas a **16 KB**, requisito de Play
  para apps que apuntan a Android 15+. Comprobado leyendo las cabeceras ELF
  del APK generado.
- El build de debug usa `applicationIdSuffix .debug`, así puedes tener
  instaladas la versión de Play y la de desarrollo a la vez.
- Si en el futuro Play exige un `targetSdk` mayor, verifica el nivel vigente
  en Play Console: cambia cada año (suele ser en agosto).

## Sin permisos peligrosos

La app no declara **ningún** permiso en el manifiesto. Los archivos se abren
con el selector del sistema (SAF), que concede acceso archivo por archivo, y el
escáner corre dentro de los servicios de Google Play, así que ni siquiera hace
falta declarar el permiso de cámara.

Esto simplifica mucho el formulario de **Seguridad de los datos**: la app no
recoge ni comparte nada, y todo el procesamiento es local.
