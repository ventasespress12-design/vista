# Inicio TV 1.5

Pantalla de inicio liviana para el TV Stick: reloj, favoritos, todas las apps, ocultar apps
y botones del control configurables (ir a Inicio TV o abrir una app). Sin publicidad ni librerías extra.

## Compilar
Subir esta carpeta a un repositorio de GitHub. En Actions se descarga `InicioTV-apk`.

## Instalar y probar (como app normal, sin cambiar nada)

    adb install -r InicioTV.apk

## Anotar la pantalla de inicio actual (para poder volver)

    adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

## Dejarla como pantalla de inicio

    adb shell cmd package set-home-activity com.edgar.inicio/.InicioActivity

## Botones del control
Este stick bloquea la accesibilidad, así que Inicio TV lee el control con **root**.
La primera vez puede aparecer en la TV un cartel de permiso de superusuario: aceptar y elegir «recordar».
Después: Inicio TV → Ajustes → Configurar botones del control → Agregar un botón.
El botón hace su acción nueva y también la que tenía antes: conviene usar botones que hoy no hacen nada.

## Volver a la del fabricante

    adb shell cmd package set-home-activity <lo que se anotó>

## Colores
Inicio TV → Ajustes → Colores: Grafito (oscuro con verde agua), Atardecer (violeta con durazno)
u Océano (azul petróleo con celeste). Se cambia al instante, sin reinstalar.

## Mi imagen (fondo con foto propia)
Ajustes → Colores → Mi imagen: elegís una foto del stick (Imágenes, Descargas, Cámara) o de un pendrive.
Los colores de detalle se sacan solos de la foto. Para copiar una foto desde la PC:

    adb push mi_foto.jpg /sdcard/Pictures/

## Teclado desde el celular (TecladoTV-CELULAR.apk)
1. Instalar TecladoTV-CELULAR.apk en el celular.
2. En la TV: Inicio TV → Ajustes → Teclado desde el celular (muestra IP y código de 4 dígitos).
3. En el celular: poner IP y código → Conectar. Lo que se escribe aparece en la TV mientras se tipea.
Trae también flechas, OK, Atrás e Inicio. Las tildes y la ñ se pegan por portapapeles.
