# -*- coding: utf-8 -*-
"""
Quita las lineas hexagonales decorativas de `ic_launcher_foreground.webp` en
las cinco densidades, dejando un fondo navy solido detras de la marca.

El archivo original traia, horneado en la propia imagen, un fondo navy con un
patron de lineas finas tipo circuito. Se nota sobre todo en la pantalla de
bienvenida, donde ese cuadro se ve completo (en el icono del lanzador queda
recortado por la mascara adaptativa y se nota menos).

No hay ninguna fuente vectorial de este icono en el repositorio (se
comprobo), asi que la unica via es separar por color: el rojo del archivo PDF
y el blanco del texto son la marca; todo lo demas dentro del cuadro (navy
liso o con lineas) se reemplaza por el MISMO navy solido que usa el resto de
la app (`@color/ic_launcher_background`, #0D2F49), para que quede parejo con
el fondo que pone el propio sistema de icono adaptativo. Fuera del cuadro se
respeta la transparencia original: es el margen de seguridad que exige el
formato de icono adaptativo, no parte del problema.

    py -3 play/limpiar_icono.py
"""

from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent
NAVY = (0x0D, 0x2F, 0x49, 255)

# Umbrales generosos: capturan el rojo/blanco de la marca, incluidos los
# bordes suavizados que se mezclan hacia esos colores. Cualquier otra cosa
# (navy liso o las lineas, que son un navy mas claro) cae al fondo solido.
def es_marca(r, g, b):
    rojo = r > 140 and g < 130 and b < 130
    blanco = r > 175 and g > 175 and b > 175
    return rojo or blanco


def limpiar(path: Path):
    im = Image.open(path).convert("RGBA")
    pixeles = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixeles[x, y]
            if a == 0:
                continue  # fuera del cuadro: se deja transparente
            if not es_marca(r, g, b):
                pixeles[x, y] = NAVY
    im.save(path)


if __name__ == "__main__":
    for densidad in ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]:
        f = RAIZ / f"app/src/main/res/mipmap-{densidad}/ic_launcher_foreground.webp"
        limpiar(f)
        print(f"limpiado: mipmap-{densidad}/ic_launcher_foreground.webp")
