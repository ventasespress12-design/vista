package com.edgar.inicio;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Escucha los botones del control en cualquier pantalla y ejecuta la acción asignada.
 * Solo intercepta los botones configurados; todos los demás funcionan como siempre.
 * No lee el contenido de la pantalla.
 */
public class ControlService extends AccessibilityService {

    /** Mientras la pantalla de configuración aprende un botón, no se intercepta nada. */
    public static volatile boolean aprendiendo = false;
    public static volatile boolean activo = false;

    private static final long LARGO_MS = 600;

    private final Handler h = new Handler(Looper.getMainLooper());
    private int teclaApretada = -1;
    private boolean largoEjecutado = false;
    private Runnable pendienteLargo;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        activo = true;
    }

    /** Botones que nunca se tocan, para no perder el control del stick. */
    public static boolean protegido(int k) {
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        if (aprendiendo) return false;
        int k = e.getKeyCode();
        if (protegido(k)) return false;

        String corto = Datos.accion(this, k, false);
        String largo = Datos.accion(this, k, true);
        if (corto == null && largo == null) return false;   // botón sin asignar: funciona normal

        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() > 0) return true;        // ignorar repeticiones al mantener
            teclaApretada = k;
            largoEjecutado = false;
            if (largo == null) {
                Acciones.ejecutar(this, corto);             // sin acción larga: responde al instante
                largoEjecutado = true;
            } else {
                pendienteLargo = () -> { largoEjecutado = true; Acciones.ejecutar(this, largo); };
                h.postDelayed(pendienteLargo, LARGO_MS);
            }
            return true;
        }

        if (e.getAction() == KeyEvent.ACTION_UP && k == teclaApretada) {
            if (pendienteLargo != null) h.removeCallbacks(pendienteLargo);
            pendienteLargo = null;
            if (!largoEjecutado && corto != null) Acciones.ejecutar(this, corto);
            teclaApretada = -1;
            return true;
        }
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        activo = false;
        super.onDestroy();
    }
}
