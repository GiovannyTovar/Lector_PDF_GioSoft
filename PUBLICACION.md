# Publicar en Google Play

Guía para firmar la app y subirla. Lo único que tienes que hacer tú es
**generar el `.jks` y rellenar `keystore.properties`**; el resto del proyecto ya
está configurado.

Estado a 14 de septiembre de 2026: la llave todavía **no está generada**
(`keystore.properties` no existe, y por eso `bundleRelease` produce un paquete
sin firmar sin que el build falle).

---

## 1. Generar el almacén de llaves — una sola vez en la vida de la app

> ⚠️ **Si pierdes este archivo o su contraseña, no podrás volver a actualizar la
> app publicada. Nunca.** No hay forma de recuperarlo por tu cuenta: la única
> red de seguridad es activar *Play App Signing* en el paso 5. Haz copia en al
> menos dos sitios (disco externo y gestor de contraseñas o nube privada).

Crea una carpeta **fuera del repositorio**:

```powershell
mkdir C:\Users\GIOVANNY\llaves
cd C:\Users\GIOVANNY\llaves

keytool -genkeypair -v `
  -keystore pdfgiosoft-release.jks `
  -storetype PKCS12 `
  -keyalg RSA -keysize 4096 `
  -validity 10000 `
  -alias pdfgiosoft
```

`keytool` viene con el JDK. Si el sistema no lo encuentra, está en el JDK de
Android Studio:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
  -keystore pdfgiosoft-release.jks -storetype PKCS12 -keyalg RSA -keysize 4096 `
  -validity 10000 -alias pdfgiosoft
```

Te pedirá una contraseña (apúntala en el gestor de contraseñas **antes** de
escribirla) y unos datos: nombre y apellido, unidad, organización, ciudad,
departamento y código de país (`CO`). No se muestran a los usuarios, pero
quedan dentro del certificado para siempre.

`-validity 10000` son unos 27 años. Play exige que el certificado siga válido
hasta el 22 de octubre de 2033 como mínimo; con 10000 días vas sobrado.

## 2. Rellenar keystore.properties

```powershell
cd C:\Users\GIOVANNY\AndroidStudioProjects\PDFGioSoft
copy keystore.properties.template keystore.properties
```

Edita `keystore.properties`:

```properties
storeFile=C:/Users/GIOVANNY/llaves/pdfgiosoft-release.jks
storePassword=LA_QUE_PUSISTE
keyAlias=pdfgiosoft
keyPassword=LA_QUE_PUSISTE
```

> Barras normales `/`, no `\`.
> Si en `keytool` aceptaste la misma contraseña para la llave que para el
> almacén, `keyPassword` y `storePassword` son iguales.
> Este archivo y los `.jks` están en `.gitignore`: nunca se suben al repositorio.

## 3. Generar el paquete firmado

Google Play exige **Android App Bundle (`.aab`)**:

```powershell
.\gradlew.bat bundleRelease
```

Queda en `app/build/outputs/bundle/release/app-release.aab`.

Comprobar que de verdad está firmado (si falta la llave, el archivo existe
igualmente pero **sin firma**, y Play lo rechaza):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs `
  (Get-ChildItem app\build\outputs\apk\release\*.apk).FullName
```

`apksigner` trabaja sobre APK, así que para esa comprobación genera también
`.\gradlew.bat assembleRelease`. Debe aparecer tu certificado y
`Verified using v2 scheme: true`.

## 4. Probar el `.aab` antes de subirlo

Un `.aab` no se instala directamente. Dos caminos:

- **Recomendado:** súbelo a un canal de **prueba interna** en Play Console e
  instálalo desde el enlace que te da. Es la misma tubería que usará la gente.
- **A mano:** con [`bundletool`](https://github.com/google/bundletool/releases):

```powershell
java -jar bundletool.jar build-apks --bundle=app\build\outputs\bundle\release\app-release.aab `
  --output=pdfgiosoft.apks --connected-device `
  --ks=C:\Users\GIOVANNY\llaves\pdfgiosoft-release.jks --ks-key-alias=pdfgiosoft
java -jar bundletool.jar install-apks --apks=pdfgiosoft.apks
```

## 5. Play App Signing: actívalo

Al crear la app en Play Console, acepta **Play App Signing**. Tu `.jks` pasa a
ser la *llave de carga*: tú firmas el `.aab` con ella y Google lo vuelve a
firmar con la llave de distribución, que custodia él. La ventaja es concreta:
si algún día pierdes tu `.jks`, puedes pedir el cambio de llave de carga y
seguir actualizando la app. Sin esto, perder el archivo significa perder la app.

## 6. Antes de subir

- [ ] `versionCode` incrementado en `app/build.gradle.kts` (Play rechaza repetidos)
- [ ] `versionName` actualizado
- [ ] `.aab` probado en un dispositivo real
- [ ] **Probadas sobre el paquete de release** (no el de debug) las cuatro
      funciones que R8 puede romper: abrir un PDF, escanear, imprimir y poner
      contraseña. R8 solo actúa en release, así que un fallo suyo no aparece
      con `installDebug`
- [ ] **Política de privacidad publicada** en una URL pública y sin login
      (está en [`web/privacidad.html`](web/privacidad.html); ver §7)
- [ ] Formulario de **Seguridad de los datos** completado (ver §8)
- [ ] Capturas (mínimo 2 de teléfono), icono 512×512, gráfico destacado 1024×500
- [ ] Descripción corta y larga
- [ ] Cuestionario de clasificación de contenido
- [ ] País de residencia y datos fiscales del desarrollador

## 7. La página legal

En [`web/`](web) están listas para publicar: `index.html`, `ayuda.html`,
`privacidad.html`, `terminos.html` y `estilos.css`. Se suben a Cloudflare Pages
(o a cualquier hosting estático) arrastrando la carpeta.

A Play se le da la URL directa de `privacidad.html`. **Esa URL vive en Play
Console, no dentro del APK**: cambiar de dominio más adelante no obliga a
publicar una versión nueva de la app.

## 8. Seguridad de los datos: lo que hay que declarar

La app no recoge datos personales, pero el formulario pregunta por todo lo que
la app *puede* hacer, incluidas las librerías que arrastra:

- **Ubicación, contactos, mensajes, fotos:** nada.
- **Archivos y documentos:** el usuario elige cada archivo con el selector del
  sistema. No se recogen ni se envían; solo se copian dentro del propio
  dispositivo en los dos casos descritos en la política.
- **Datos de diagnóstico:** el escáner de ML Kit arrastra
  `com.google.android.datatransport:transport-backend-cct`, que **añade
  `android.permission.INTERNET` y `ACCESS_NETWORK_STATE` al manifiesto final**
  aunque el proyecto no los declare. Es telemetría de Google sobre su propio
  componente. Compruébalo tú mismo antes de responder el formulario:

```powershell
.\gradlew.bat :app:processReleaseManifestForPackage
Select-String "uses-permission" app\build\intermediates\merged_manifest\release\*\AndroidManifest.xml
Select-String -Context 2 "INTERNET" app\build\outputs\logs\manifest-merger-release-report.txt
```

- **Permiso propio:** `USE_BIOMETRIC`, que Android clasifica como *normal*. La
  app nunca accede a datos biométricos; solo recibe del sistema un sí o un no.

## 9. Notas técnicas

- `compileSdk` **37** · `targetSdk` **36** · `minSdk` **31** (Android 12)
- AGP **9.4.0**, Gradle **9.7.1**, Java **17**. Kotlin va integrado en AGP:
  aplicar `org.jetbrains.kotlin.android` da error
- Release con **R8** (`isMinifyEnabled`) y `shrinkResources`
- El motor de PDF es `androidx.pdf`, Apache 2.0 y **sin librerías nativas
  propias** (ver [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) §2)
- El paquete incluye dos `.so` que vienen de AndroidX
  (`libandroidx.graphics.path.so` y `libdatastore_shared_counter.so`).
  **Ambas están alineadas a 16 KB**, el requisito de Play; verificado leyendo
  las cabeceras de programa del ELF (`p_align = 0x4000` en los segmentos LOAD)
- El build de debug usa `applicationIdSuffix .debug`: puedes tener instaladas a
  la vez la de Play y la de desarrollo. Ese sufijo no llega a Play
- El diálogo «Acerca de» ya **no** incluye el número de cuenta para donaciones
  que tenía la v4.x. No lo vuelvas a añadir sin revisar la política de pagos de
  Play: pedir donaciones fuera de su facturación está restringido, y la exención
  suele ser para organizaciones sin ánimo de lucro registradas, no para personas
- Si Play sube el `targetSdk` mínimo (suele anunciarlo cada agosto), comprueba
  el nivel vigente en Play Console antes de publicar
