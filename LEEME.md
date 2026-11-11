# Inicio TV 1.1

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

## Activar los botones del control (una sola vez)

    adb shell settings put secure enabled_accessibility_services com.edgar.inicio/com.edgar.inicio.ControlService
    adb shell settings put secure accessibility_enabled 1

## Volver a la del fabricante

    adb shell cmd package set-home-activity <lo que se anotó>
