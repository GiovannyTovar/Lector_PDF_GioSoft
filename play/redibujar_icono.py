# -*- coding: utf-8 -*-
"""
Redibuja `ic_launcher_foreground.webp` con texto nitido en vez del raster
original, que traia el texto ya suavizado de fabrica (1-2 px de transicion
sobre trazos de solo 8-10 px de ancho: se nota borroso, sobre todo ampliado
en la pantalla de bienvenida). Ni el afilado por software (unsharp mask) lo
arregla: genera aros oscuros porque nunca hubo detalle real que recuperar.

Se mantiene el mismo diseno (icono de documento rojo con la esquina doblada,
"PDF" adentro, "PDF Giosoft" al lado), medido pixel a pixel sobre el archivo
actual para que las proporciones no cambien; lo unico que cambia es que el
texto se dibuja con una fuente de verdad a la resolucion final, en vez de
partir de un mapa de bits ya borroso.

    py -3 play/redibujar_icono.py
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

RAIZ = Path(__file__).resolve().parent.parent

NAVY = (0x0D, 0x2F, 0x49, 255)
ROJO = (221, 43, 28, 255)
ROJO_PLIEGUE = (181, 35, 22, 255)  # sombra del doblez, un poco mas oscuro
BLANCO = (250, 247, 241, 255)

FUENTE_BOLD = "C:/Windows/Fonts/segoeuib.ttf"

# Medidas tomadas del icono actual a 432x432 (xxxhdpi), para que el diseno
# nuevo caiga exactamente en el mismo sitio y del mismo tamano.
LIENZO = 432
CUADRO = (80, 80, 352, 352)          # zona opaca dentro del lienzo
ICONO_RECT = (107, 153, 185, 261)    # el rectangulo rojo
PLIEGUE = 34                         # lado del triangulo doblado, en px
RADIO_ICONO = 10

TAM_PDF_CHICO = 30
TAM_PDF_GRANDE = 61
TAM_GIOSOFT = 37
POS_PDF_CHICO = (119, 205)   # esquina superior izq. del texto "PDF" chico
POS_PDF_GRANDE = (203, 163)
POS_GIOSOFT = (198, 222)


def dibujar_icono_documento(draw: ImageDraw.ImageDraw):
    x0, y0, x1, y1 = ICONO_RECT
    # El rectangulo del documento, con la esquina superior derecha recta
    # (el doblez se dibuja encima) y las otras tres redondeadas.
    draw.rounded_rectangle(
        [x0, y0, x1, y1], radius=RADIO_ICONO, fill=ROJO,
        corners=(True, False, True, True),
    )
    # El doblez: un triangulo del color de fondo que "recorta" la esquina...
    draw.polygon(
        [(x1 - PLIEGUE, y0), (x1, y0), (x1, y0 + PLIEGUE)],
        fill=NAVY,
    )
    # ...y su sombra, un triangulo mas chico y mas oscuro justo debajo, que
    # sugiere el papel doblado hacia adentro.
    draw.polygon(
        [(x1 - PLIEGUE, y0), (x1, y0 + PLIEGUE), (x1 - PLIEGUE, y0 + PLIEGUE)],
        fill=ROJO_PLIEGUE,
    )

    fuente = ImageFont.truetype(FUENTE_BOLD, TAM_PDF_CHICO)
    draw.text(POS_PDF_CHICO, "PDF", font=fuente, fill=BLANCO)


def generar(lado: int) -> Image.Image:
    """El icono completo a `lado`x`lado` px (se genera siempre a 432 y se
    reescala al final, para que el texto salga con el mismo trazo fino en
    todas las densidades en vez de recalcular tamanos de fuente cada vez)."""
    im = Image.new("RGBA", (LIENZO, LIENZO), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    draw.rectangle(CUADRO, fill=NAVY)

    dibujar_icono_documento(draw)

    draw.text(POS_PDF_GRANDE, "PDF", font=ImageFont.truetype(FUENTE_BOLD, TAM_PDF_GRANDE), fill=BLANCO)
    draw.text(POS_GIOSOFT, "Giosoft", font=ImageFont.truetype(FUENTE_BOLD, TAM_GIOSOFT), fill=BLANCO)

    if lado != LIENZO:
        im = im.resize((lado, lado), Image.LANCZOS)
    return im


if __name__ == "__main__":
    densidades = {
        "mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432,
    }
    for nombre, lado in densidades.items():
        f = RAIZ / f"app/src/main/res/mipmap-{nombre}/ic_launcher_foreground.webp"
        generar(lado).save(f)
        print(f"generado: mipmap-{nombre}/ic_launcher_foreground.webp ({lado}x{lado})")
