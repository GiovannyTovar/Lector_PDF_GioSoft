# Gráficos para la ficha de Google Play

Lo que Play pide para publicar la app, y dónde está cada cosa.

| Qué pide Play | Medidas | Archivo | Estado |
|---|---|---|---|
| Icono de la app | 512 × 512 px, PNG 32 bits, máx. 1 MB | `icono-512.png` | ✅ generado |
| Gráfico destacado | 1024 × 500 px, PNG o JPG | `grafico-destacado-1024x500.png` | ✅ generado |
| Capturas de teléfono | mín. 2, entre 320 y 3840 px de lado | — | ❌ las haces tú (ver abajo) |

Los dos primeros los genera [`generar-graficos.py`](generar-graficos.py) a partir
del **icono real de la app**, así que lo que se ve en Play es lo mismo que verá
la gente en su lanzador. Si algún día cambia el icono:

```powershell
py -3 play/generar-graficos.py
```

(Necesita Pillow una sola vez: `py -3 -m pip install pillow`.)

## Las capturas de pantalla

Estas no se pueden generar: tienen que ser de la app funcionando. Se hacen con
el celular, con la app instalada:

1. Abre la app y ponla en la pantalla que quieras mostrar.
2. Pulsa **encendido + bajar volumen** a la vez.
3. La imagen queda en la galería, en «Capturas de pantalla».

Play pide **mínimo 2**, pero cuantas más y mejores, más se descarga la app. Un
orden que funciona:

1. La lista de documentos con varios PDF y las categorías de colores.
2. Un documento abierto en el visor.
3. El escáner con un papel encuadrado.
4. El diálogo de proteger con huella, o la lista en modo oscuro.

Consejo: antes de capturar, abre unos cuantos PDF con nombres presentables. Lo
que salga en la captura lo va a ver todo el mundo, así que evita facturas,
documentos personales o nombres con datos tuyos.

## Lo que Play NO necesita

- Vídeo promocional: opcional.
- Gráfico para tablet o TV: solo si publicas para esos dispositivos.
- Icono redondo aparte: no; Play aplica él mismo la máscara de esquinas al
  `icono-512.png`.
