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

    public static void ejecutar(Context c, String accion) {
        try {
            if (accion.equals(INICIO)) {
                c.startActivity(new Intent(c, InicioActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else if (accion.startsWith(APP)) {
                String p = accion.substring(APP.length());
                PackageManager pm = c.getPackageManager();
                Intent i = pm.getLeanbackLaunchIntentForPackage(p);
                if (i == null) i = pm.getLaunchIntentForPackage(p);
                if (i == null) throw new IllegalStateException("no instalada");
                c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception e) {
            Toast.makeText(c, "No se pudo: " + descripcion(c, accion), Toast.LENGTH_SHORT).show();
        }
    }
}
