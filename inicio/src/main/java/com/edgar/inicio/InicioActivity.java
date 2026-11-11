package com.edgar.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pantalla de inicio liviana: reloj, favoritos y todas las apps. */
public class InicioActivity extends Activity {

    private LinearLayout raiz;
    private List<Datos.App> apps;
    private Map<String, Datos.App> porPaquete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Estilo.FONDO);
        scroll.setFillViewport(true);
        raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int px = Estilo.dp(this, 40), py = Estilo.dp(this, 26);
        raiz.setPadding(px, py, px, py);
        scroll.addView(raiz);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        construir();   // se rearma al volver: refleja apps instaladas o desinstaladas
    }

    @Override
    public void onBackPressed() {
        // Es la pantalla de inicio: ATRÁS no hace nada
    }

    // ---------- Armado de la pantalla ----------

    private void construir() {
        raiz.removeAllViews();
        apps = Datos.apps(this);
        porPaquete = new HashMap<>();
        for (Datos.App a : apps) porPaquete.put(a.paquete, a);
        Set<String> ocultas = Datos.ocultas(this);

        raiz.addView(encabezado());

        // Favoritos
        List<String> favs = Datos.favoritos(this);
        raiz.addView(Estilo.titulo(this, "Favoritos"), margenArriba(18));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false);
        LinearLayout fila = new LinearLayout(this);
        fila.setClipChildren(false);
        fila.setPadding(0, Estilo.dp(this, 8), 0, Estilo.dp(this, 8));
        View primero = null;
        for (String p : favs) {
            Datos.App a = porPaquete.get(p);
            if (a == null) continue;
            View b = Estilo.baldosa(this, a.icono, a.nombre, true);
            b.setOnClickListener(v -> abrir(a));
            b.setOnLongClickListener(v -> { menuApp(a, true); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Estilo.dp(this, 170), Estilo.dp(this, 128));
            lp.rightMargin = Estilo.dp(this, 16);
            fila.addView(b, lp);
            if (primero == null) primero = b;
        }
        if (fila.getChildCount() == 0) {
            TextView vacio = Estilo.titulo(this, "Mantené OK apretado sobre una app de abajo para agregarla acá.");
            vacio.setTextColor(Estilo.TEXTO_TENUE);
            fila.addView(vacio);
        }
        hs.addView(fila);
        raiz.addView(hs);

        // Todas las apps
        raiz.addView(Estilo.titulo(this, "Todas las apps"), margenArriba(14));
        int anchoDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int columnas = Math.max(3, (anchoDp - 80) / 150);
        GridLayout grilla = new GridLayout(this);
        grilla.setColumnCount(columnas);
        grilla.setClipChildren(false);
        grilla.setPadding(0, Estilo.dp(this, 8), 0, 0);
        List<Datos.App> listaOcultas = new ArrayList<>();
        for (Datos.App a : apps) {
            if (ocultas.contains(a.paquete)) { listaOcultas.add(a); continue; }
            View b = Estilo.baldosa(this, a.icono, a.nombre, false);
            b.setOnClickListener(v -> abrir(a));
            b.setOnLongClickListener(v -> { menuApp(a, false); return true; });
            grilla.addView(b, celda());
            if (primero == null) primero = b;
        }
        if (!listaOcultas.isEmpty()) {
            View b = Estilo.baldosa(this, getDrawable(android.R.drawable.ic_menu_view),
                    "Ocultas (" + listaOcultas.size() + ")", false);
            b.setOnClickListener(v -> menuOcultas(listaOcultas));
            grilla.addView(b, celda());
        }
        raiz.addView(grilla);

        TextView ayuda = Estilo.titulo(this, "Mantené OK apretado sobre una app para agregarla a favoritos u ocultarla.");
        ayuda.setTextColor(Estilo.TEXTO_TENUE);
        ayuda.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        raiz.addView(ayuda, margenArriba(20));

        if (primero != null) primero.requestFocus();
    }

    private View encabezado() {
        LinearLayout e = new LinearLayout(this);
        e.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout reloj = new LinearLayout(this);
        reloj.setOrientation(LinearLayout.VERTICAL);
        TextClock hora = new TextClock(this);
        hora.setFormat24Hour("HH:mm");
        hora.setFormat12Hour("HH:mm");
        hora.setTextColor(0xFFF8FAFC);
        hora.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        reloj.addView(hora);
        TextClock fecha = new TextClock(this);
        fecha.setFormat24Hour("EEEE d 'de' MMMM");
        fecha.setFormat12Hour("EEEE d 'de' MMMM");
        fecha.setTextColor(Estilo.TEXTO_SUAVE);
        fecha.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        reloj.addView(fecha);
        e.addView(reloj, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        View ajustes = Estilo.baldosa(this, getDrawable(android.R.drawable.ic_menu_manage), "Ajustes", false);
        ajustes.setOnClickListener(v -> menuAjustes());
        e.addView(ajustes, new LinearLayout.LayoutParams(Estilo.dp(this, 110), Estilo.dp(this, 90)));
        return e;
    }

    private GridLayout.LayoutParams celda() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = Estilo.dp(this, 134);
        lp.height = Estilo.dp(this, 96);
        int m = Estilo.dp(this, 8);
        lp.setMargins(0, m, Estilo.dp(this, 14), m);
        return lp;
    }

    private LinearLayout.LayoutParams margenArriba(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Estilo.dp(this, dp);
        return lp;
    }

    // ---------- Acciones ----------

    private void abrir(Datos.App a) {
        try { startActivity(a.abrir); } catch (Exception ignored) { }
    }

    private void menuApp(Datos.App a, boolean esFavorito) {
        List<String> favs = Datos.favoritos(this);
        Set<String> ocultas = Datos.ocultas(this);
        List<String> opciones = new ArrayList<>();
        List<Runnable> hacer = new ArrayList<>();

        opciones.add("Abrir");
        hacer.add(() -> abrir(a));
        if (favs.contains(a.paquete)) {
            opciones.add("Quitar de favoritos");
            hacer.add(() -> { favs.remove(a.paquete); Datos.guardarFavoritos(this, favs); construir(); });
            int i = favs.indexOf(a.paquete);
            if (i > 0) {
                opciones.add("Mover a la izquierda");
                hacer.add(() -> { favs.remove(i); favs.add(i - 1, a.paquete); Datos.guardarFavoritos(this, favs); construir(); });
            }
            if (i < favs.size() - 1) {
                opciones.add("Mover a la derecha");
                hacer.add(() -> { favs.remove(i); favs.add(i + 1, a.paquete); Datos.guardarFavoritos(this, favs); construir(); });
            }
        } else {
            opciones.add("Agregar a favoritos");
            hacer.add(() -> { favs.add(a.paquete); Datos.guardarFavoritos(this, favs); construir(); });
        }
        if (!esFavorito) {
            opciones.add("Ocultar de la lista");
            hacer.add(() -> { ocultas.add(a.paquete); Datos.guardarOcultas(this, ocultas); construir(); });
        }

        new AlertDialog.Builder(this)
                .setTitle(a.nombre)
                .setItems(opciones.toArray(new String[0]), (d, w) -> hacer.get(w).run())
                .show();
    }

    private void menuOcultas(List<Datos.App> lista) {
        String[] nombres = new String[lista.size()];
        for (int i = 0; i < lista.size(); i++) nombres[i] = lista.get(i).nombre;
        new AlertDialog.Builder(this)
                .setTitle("Apps ocultas (elegí una para volver a mostrarla)")
                .setItems(nombres, (d, w) -> {
                    Set<String> ocultas = Datos.ocultas(this);
                    ocultas.remove(lista.get(w).paquete);
                    Datos.guardarOcultas(this, ocultas);
                    construir();
                })
                .show();
    }

    private void menuAjustes() {
        String[] op = {"Configurar botones del control", "Ajustes del sistema"};
        new AlertDialog.Builder(this)
                .setTitle("Ajustes")
                .setItems(op, (d, w) -> {
                    try {
                        if (w == 0) startActivity(new Intent(this, ConfigControlActivity.class));
                        else startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ignored) { }
                })
                .show();
    }
}
