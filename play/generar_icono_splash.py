# -*- coding: utf-8 -*-
"""
Genera `ic_launcher_splash.png` en las cinco densidades: el icono de la app
(fondo + marca, compuestos) con las esquinas redondeadas, para usar en la
pantalla de bienvenida.

Por que hace falta un archivo aparte del icono normal: la pantalla de
bienvenida (`windowSplashScreenAnimatedIcon`) no aplica la mascara redondeada
que sí aplica el lanzador sobre el icono adaptativo — si se le pasa
directamente `ic_launcher_adaptive_fore`, sale como un cuadrado de esquinas
totalmente rectas. Aqui se redondea a mano, con el mismo look que un icono
adaptativo normal.

    py -3 play/generar_icono_splash.py
"""

from pathlib import Path
from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
MIPMAP_ORIGEN = RAIZ / "app/src/main/res/mipmap-xxxhdpi"
FONDO_ICONO = MIPMAP_ORIGEN / "ic_launcher_adaptive_back.png"
FRENTE_ICONO = MIPMAP_ORIGEN / "ic_launcher_adaptive_fore.png"

# Radio de esquina como fraccion del lado: ~20% da el look "squircle"
# habitual de los iconos modernos de Android, mas marcado que el recorte por
# defecto que aplicaba el sistema.
FRACCION_RADIO = 0.20


def generar(lado: int) -> Image.Image:
    fondo = Image.open(FONDO_ICONO).convert("RGBA").resize((lado, lado), Image.LANCZOS)

    frente = Image.open(FRENTE_ICONO).convert("RGBA")
    bbox = frente.getbbox()
    proporcion = (bbox[2] - bbox[0]) / frente.width
    marca = frente.crop(bbox).resize(
        (round(lado * proporcion), round(lado * proporcion)), Image.LANCZOS
    )
    pos = ((lado - marca.width) // 2, (lado - marca.height) // 2)
    fondo.alpha_composite(marca, pos)

    mascara = Image.new("L", (lado, lado), 0)
    ImageDraw.Draw(mascara).rounded_rectangle(
        [0, 0, lado - 1, lado - 1], radius=round(lado * FRACCION_RADIO), fill=255
    )
    fondo.putalpha(mascara)
    return fondo


if __name__ == "__main__":
    densidades = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    for nombre, lado in densidades.items():
        destino = RAIZ / f"app/src/main/res/mipmap-{nombre}/ic_launcher_splash.png"
        generar(lado).save(destino)
        print(f"generado: mipmap-{nombre}/ic_launcher_splash.png ({lado}x{lado})")
