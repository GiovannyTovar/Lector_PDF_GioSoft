# -*- coding: utf-8 -*-
"""
Quita las lineas hexagonales decorativas de `ic_launcher_foreground.webp` en
las cinco densidades, dejando un fondo navy solido detras de la marca.

El archivo original traia, horneado en la propia imagen, un fondo navy con un
patron de lineas finas tipo circuito. Se nota sobre todo en la pantalla de
bienvenida, donde ese cuadro se ve completo (en el icono del lanzador queda
recortado por la mascara adaptativa y se nota menos).

No hay ninguna fuente vectorial de este icono en el repositorio (se
comprobo), asi que la unica via es separar por color. La primera version de
este script clasificaba pixel a pixel por color (rojo/blanco = marca, el
resto = fondo) y eso rompio el suavizado de las letras: el borde de cada
trazo tiene un solo pixel de transicion (mezcla de navy y blanco) para que no
se vea dentado, y ese pixel se parece de color a una linea decorativa vista
de lejos. Aplanarlo a navy solido volvia el texto visiblemente mas tosco.

La solucion es no decidir solo por color, sino tambien por CERCANIA a la
marca: se protege un halo de un par de pixeles alrededor de cualquier trazo
rojo o blanco (donde vive el suavizado), y solo se aplana a navy solido lo
que quede fuera de ese halo, en zona de fondo abierto donde con certeza no
hay ninguna letra ni el icono cerca.

    py -3 play/limpiar_icono.py
"""

from pathlib import Path
from PIL import Image, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
NAVY = (0x0D, 0x2F, 0x49, 255)

# Radio del halo que se protege alrededor de la marca, en pixeles de la
# imagen a resolucion xxxhdpi (432x432). Con 1 no bastaba: quedaba un aro
# navy solido justo pegado a las letras, un pelo mas duro que el original.
RADIO_HALO = 3


def es_marca_nucleo(r, g, b) -> bool:
    """Pixeles claramente rojos o blancos: el centro solido de la marca, sin
    contar los bordes suavizados (esos los protege el halo, no este filtro)."""
    rojo = r > 140 and g < 130 and b < 130
    blanco = r > 175 and g > 175 and b > 175
    return rojo or blanco


def limpiar(path: Path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size

    # Mascara del nucleo de la marca, dilatada para formar el halo de
    # proteccion alrededor de cada letra y del icono.
    mascara = Image.new("L", (w, h), 0)
    px_in = im.load()
    px_mask = mascara.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px_in[x, y]
            if a > 0 and es_marca_nucleo(r, g, b):
                px_mask[x, y] = 255
    tamano_filtro = RADIO_HALO * 2 + 1
    mascara = mascara.filter(ImageFilter.MaxFilter(tamano_filtro))
    px_mask = mascara.load()

    out = im.copy()
    px_out = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px_in[x, y]
            if a == 0:
                continue  # fuera del cuadro: se respeta la transparencia
            if px_mask[x, y] == 0:
                # Lejos de cualquier trazo de la marca: aqui solo puede haber
                # navy liso o una linea decorativa. Se aplana sin riesgo.
                px_out[x, y] = NAVY
            # Dentro del halo se deja tal cual: es la marca o su suavizado.
    out.save(path)


if __name__ == "__main__":
    for densidad in ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]:
        f = RAIZ / f"app/src/main/res/mipmap-{densidad}/ic_launcher_foreground.webp"
        limpiar(f)
        print(f"limpiado: mipmap-{densidad}/ic_launcher_foreground.webp")
