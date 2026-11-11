package com.edgar.inicio;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

/**
 * Acciones que se le pueden asignar a un botón del control.
 *  "inicio"            → vuelve a Inicio TV
 *  "app:<paquete>"     → abre esa app
 */
public final class Acciones {

    public static final String INICIO = "inicio";
    public static final String APP = "app:";

    private Acciones() {}

    public static String descripcion(Context c, String accion) {
        if (accion == null) return "Nada";
        if (accion.equals(INICIO)) return "Ir a Inicio TV";
        if (accion.startsWith(APP)) {
            String p = accion.substring(APP.length());
            try {
                PackageManager pm = c.getPackageManager();
                return "Abrir " + pm.getApplicationLabel(pm.getApplicationInfo(p, 0));
            } catch (Exception e) {
                return "Abrir " + p + " (no instalada)";
            }
        }
        return accion;
    }

    /**
     * Abre lo pedido usando root ("am start"): Android 10 no deja que una app
     * en segundo plano abra otras, pero con root sí se puede.
     */
    public static void ejecutar(Context c, String accion) {
        String componente = null;
        if (accion.equals(INICIO)) {
            componente = c.getPackageName() + "/" + InicioActivity.class.getName();
        } else if (accion.startsWith(APP)) {
            String p = accion.substring(APP.length());
            PackageManager pm = c.getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(p);
            if (i == null) i = pm.getLaunchIntentForPackage(p);
            if (i != null && i.getComponent() != null) componente = i.getComponent().flattenToString();
        }
        if (componente == null || !Raiz.ejecutar("am start -n " + componente)) {
            Toast.makeText(c, "No se pudo: " + descripcion(c, accion), Toast.LENGTH_SHORT).show();
        }
    }
}
