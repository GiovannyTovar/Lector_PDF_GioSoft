# -*- coding: utf-8 -*-
"""
Genera los graficos que Google Play exige en la ficha de la app.

    py -3 play/generar-graficos.py

Produce, en esta misma carpeta:

    icono-512.png                  icono de la ficha (512x512, obligatorio)
    grafico-destacado-1024x500.png cabecera de la ficha (obligatorio)

Se parte del icono real de la app (`mipmap-xxxhdpi/ic_launcher_foreground.webp`
y el color `ic_launcher_background`), de modo que lo que ve la gente en Play es
exactamente lo que vera luego en su lanzador. Si algun dia cambia el icono,
basta con volver a ejecutar este script.

Requiere Pillow:  py -3 -m pip install pillow
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

RAIZ = Path(__file__).resolve().parent.parent
FOREGROUND = RAIZ / "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp"
SALIDA = Path(__file__).resolve().parent

# Los mismos colores de la app: el azul de la barra superior y el rojo del PDF.
AZUL = (13, 47, 73)
AZUL_CLARO = (22, 66, 99)
ROJO = (228, 57, 46)
BLANCO = (255, 255, 255)
GRIS_CLARO = (168, 190, 207)

TITULO = "PDF GioSoft"
LEMA = "Tus documentos, sin publicidad y sin complicaciones."

FUENTE_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FUENTE_NORMAL = "C:/Windows/Fonts/segoeui.ttf"


def icono(lado, radio=0):
    """
    El icono de la app en un cuadrado de `lado` pixeles.

    La capa frontal del icono adaptativo no es solo la marca: trae su propio
    recuadro con textura. Se recorta a ese recuadro y se escala al lienzo
    entero, de modo que la textura llena el icono en vez de quedar como un
    parche mas oscuro flotando sobre el fondo.

    Con `radio` se redondean las esquinas, para verlo como lo muestra Play.
    """
    logo = Image.open(FOREGROUND).convert("RGBA")
    logo = logo.crop(logo.getbbox()).resize((lado, lado), Image.LANCZOS)

    fondo = Image.new("RGBA", (lado, lado), AZUL + (255,))
    fondo.alpha_composite(logo)

    if radio:
        mascara = Image.new("L", (lado, lado), 0)
        ImageDraw.Draw(mascara).rounded_rectangle([0, 0, lado - 1, lado - 1], radio, fill=255)
        fondo.putalpha(mascara)
    return fondo


def envolver(texto, fuente, ancho_max, pincel):
    """Parte el texto en lineas que quepan en `ancho_max`."""
    lineas, actual = [], ""
    for palabra in texto.split():
        prueba = f"{actual} {palabra}".strip()
        if pincel.textlength(prueba, font=fuente) <= ancho_max:
            actual = prueba
        else:
            lineas.append(actual)
            actual = palabra
    if actual:
        lineas.append(actual)
    return lineas


def icono_512():
    """
    Icono de la ficha de Play: 512x512, PNG de 32 bits.

    Google le aplica luego su propia mascara de esquinas redondeadas, asi que
    se entrega el cuadrado completo. La marca se deja al 63 % del lienzo, la
    misma proporcion que reserva el icono adaptativo, para que se reconozca.
    """
    lienzo = icono(512)
    destino = SALIDA / "icono-512.png"
    lienzo.save(destino)
    print(f"{destino.name}: {lienzo.size[0]}x{lienzo.size[1]}")


def grafico_destacado():
    """
    Cabecera de la ficha: 1024x500.

    Play la recorta por los lados en algunas superficies, de ahi que lo
    importante quede lejos de los bordes.
    """
    ancho, alto = 1024, 500
    lienzo = Image.new("RGBA", (ancho, alto), AZUL + (255,))

    # Degradado diagonal muy suave: da profundidad sin competir con la marca.
    capa = Image.new("RGBA", (ancho, alto))
    pincel = ImageDraw.Draw(capa)
    for x in range(ancho):
        mezcla = x / ancho
        color = tuple(
            round(AZUL[i] + (AZUL_CLARO[i] - AZUL[i]) * mezcla) for i in range(3)
        )
        pincel.line([(x, 0), (x, alto)], fill=color + (255,))
    lienzo.alpha_composite(capa)

    lado = 236
    margen = 88
    logo = icono(lado, radio=52)
    lienzo.alpha_composite(logo, (margen, (alto - lado) // 2))

    pincel = ImageDraw.Draw(lienzo)
    x_texto = margen + lado + 64
    disponible = ancho - x_texto - margen
    titulo = ImageFont.truetype(FUENTE_BOLD, 74)
    lema = ImageFont.truetype(FUENTE_NORMAL, 30)
    lineas = envolver(LEMA, lema, disponible, pincel)

    # El bloque de texto se centra respecto al logo, no respecto al lienzo: si
    # el lema ocupa dos lineas, el conjunto sigue alineado con el icono.
    altura_bloque = 88 + 30 + len(lineas) * 42
    y = (alto - altura_bloque) // 2

    pincel.text((x_texto, y), TITULO, font=titulo, fill=BLANCO)
    # Un trazo rojo corto bajo el titulo: el unico acento de color, el mismo
    # rojo del icono.
    pincel.rectangle([x_texto, y + 104, x_texto + 92, y + 110], fill=ROJO)
    for i, linea in enumerate(lineas):
        pincel.text((x_texto, y + 142 + i * 42), linea, font=lema, fill=GRIS_CLARO)

    destino = SALIDA / "grafico-destacado-1024x500.png"
    lienzo.convert("RGB").save(destino)
    print(f"{destino.name}: {lienzo.size[0]}x{lienzo.size[1]}")


if __name__ == "__main__":
    icono_512()
    grafico_destacado()
