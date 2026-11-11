package com.edgar.inicio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Apps instaladas, favoritos, ocultas y botones del control (todo guardado en el stick). */
public final class Datos {

    public static final class App {
        public final String paquete;
        public final String nombre;
        public final Drawable icono;
        public final Intent abrir;
        App(String paquete, String nombre, Drawable icono, Intent abrir) {
            this.paquete = paquete; this.nombre = nombre; this.icono = icono; this.abrir = abrir;
        }
    }

    private Datos() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("inicio", Context.MODE_PRIVATE);
    }

    /** Todas las apps que se pueden abrir, sin repetir, ordenadas por nombre. */
    public static List<App> apps(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, App> porPaquete = new LinkedHashMap<>();
        // Primero las de TV (traen banner horizontal), después las comunes
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(c.getPackageName()) || porPaquete.containsKey(p)) continue;
                Intent abrir = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(p, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Drawable icono = ri.activityInfo.loadIcon(pm);
                porPaquete.put(p, new App(p, String.valueOf(ri.loadLabel(pm)), icono, abrir));
            }
        }
        List<App> lista = new ArrayList<>(porPaquete.values());
        Collections.sort(lista, (a, b) -> a.nombre.compareToIgnoreCase(b.nombre));
        return lista;
    }

    // ---------- Favoritos (con orden) ----------

    public static List<String> favoritos(Context c) {
        String s = prefs(c).getString("favoritos", null);
        if (s == null) {
            // Primera vez: algunas sugeridas si están instaladas
            return new ArrayList<>(Arrays.asList("com.android.mgstv", "com.google.android.youtube.tv"));
        }
        List<String> l = new ArrayList<>();
        for (String p : s.split(",")) if (!p.isEmpty()) l.add(p);
        return l;
    }

    public static void guardarFavoritos(Context c, List<String> l) {
        prefs(c).edit().putString("favoritos", String.join(",", l)).apply();
    }

    // ---------- Ocultas ----------

    public static Set<String> ocultas(Context c) {
        return new HashSet<>(prefs(c).getStringSet("ocultas", new HashSet<>()));
    }

    public static void guardarOcultas(Context c, Set<String> s) {
        prefs(c).edit().putStringSet("ocultas", s).apply();
    }

    // ---------- Botones del control ----------
    // Se guardan por el nombre que manda el control (ej. KEY_RO, KEY_SAVE):
    // "boton_<nombre>_corto" / "boton_<nombre>_largo" → acción (ver Acciones)

    public static String accion(Context c, String tecla, boolean largo) {
        return prefs(c).getString("boton_" + tecla + (largo ? "_largo" : "_corto"), null);
    }

    public static void guardarAccion(Context c, String tecla, boolean largo, String accion) {
        SharedPreferences.Editor e = prefs(c).edit();
        String k = "boton_" + tecla + (largo ? "_largo" : "_corto");
        if (accion == null) e.remove(k); else e.putString(k, accion);
        e.apply();
    }

    /** Nombres de botón que tienen alguna acción asignada. */
    public static Set<String> botonesAsignados(Context c) {
        Set<String> s = new HashSet<>();
        for (String k : prefs(c).getAll().keySet()) {
            if (k.startsWith("boton_") && (k.endsWith("_corto") || k.endsWith("_largo"))) {
                s.add(k.substring("boton_".length(), k.length() - "_corto".length()));
            }
        }
        return s;
    }
}
