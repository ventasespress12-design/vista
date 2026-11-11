package com.edgar.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configurar botones del control: se aprieta el botón y se elige qué hace
 * con un toque corto y/o manteniéndolo apretado.
 */
public class ConfigControlActivity extends Activity {

    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout lista;
    private TextView aviso;
    private AlertDialog dialogoEspera;

    // Última tecla que llegó por Android mientras se aprende (para no permitir flechas, OK, etc.)
    private int ultimaTeclaAndroid = -1;
    private long ultimaTeclaHora = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Estilo.cargar(this);
        ScrollView scroll = new ScrollView(this);
        Estilo.ponerFondo(scroll);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int p = Estilo.dp(this, 40);
        raiz.setPadding(p, Estilo.dp(this, 28), p, p);
        scroll.addView(raiz);
        setContentView(scroll);

        TextView t = new TextView(this);
        t.setText("Botones del control");
        t.setTextColor(Estilo.TEXTO);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        raiz.addView(t);

        aviso = Estilo.titulo(this, "");
        aviso.setTextColor(Estilo.DETALLE);
        raiz.addView(aviso);

        View agregar = Estilo.baldosa(this, getDrawable(android.R.drawable.ic_input_add), "Agregar un botón", false);
        agregar.setOnClickListener(v -> esperarBoton());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Estilo.dp(this, 220), Estilo.dp(this, 90));
        lp.topMargin = Estilo.dp(this, 16);
        raiz.addView(agregar, lp);

        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(lista);

        TextView ayuda = Estilo.titulo(this,
                "Las flechas, OK, Atrás, Inicio, volumen y encendido no se pueden cambiar, para no perder el control del stick.\n"
                + "En este stick el botón hace su acción nueva y también la que tenía antes; "
                + "conviene usar botones que hoy no hacen nada.");
        ayuda.setTextColor(Estilo.TEXTO_TENUE);
        ayuda.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams la = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        la.topMargin = Estilo.dp(this, 24);
        raiz.addView(ayuda, la);

        agregar.requestFocus();
        BotonesService.iniciar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescarAviso();
        refrescar();
        // A los 3 s, volver a mirar: el lector puede tardar en arrancar
        ui.postDelayed(this::refrescarAviso, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BotonesService.aprendiz = null;
    }

    private void refrescarAviso() {
        if (BotonesService.leyendo) {
            aviso.setText("");
        } else {
            String e = BotonesService.error;
            aviso.setText((e != null ? e + "\n" : "Iniciando la lectura del control…\n")
                    + "Si aparece un cartel de permiso de superusuario (root) en la TV, aceptalo y elegí «recordar».");
        }
    }

    private void refrescar() {
        lista.removeAllViews();
        List<String> teclas = new ArrayList<>(Datos.botonesAsignados(this));
        Collections.sort(teclas);
        for (String k : teclas) {
            String texto = nombre(k) + "\n   Toque: " + Acciones.descripcion(this, Datos.accion(this, k, false))
                    + "\n   Mantener: " + Acciones.descripcion(this, Datos.accion(this, k, true));
            TextView fila = new TextView(this);
            fila.setText(texto);
            fila.setTextColor(Estilo.TEXTO);
            fila.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            fila.setFocusable(true);
            fila.setClickable(true);
            fila.setBackground(Estilo.fondoBaldosa(this));
            int p = Estilo.dp(this, 14);
            fila.setPadding(p, p, p, p);
            fila.setOnClickListener(v -> elegirTipo(k));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Estilo.dp(this, 12);
            lista.addView(fila, lp);
        }
    }

    private static String nombre(String tecla) {
        return "Botón " + tecla.replace("KEY_", "");
    }

    private static boolean protegidoAndroid(int k) {
        switch (k) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_POWER: case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN: case KeyEvent.KEYCODE_VOLUME_MUTE:
                return true;
            default:
                return false;
        }
    }

    // ---------- Aprender un botón ----------

    private void esperarBoton() {
        if (!BotonesService.leyendo) {
            refrescarAviso();
            new AlertDialog.Builder(this)
                    .setMessage("Todavía no se está leyendo el control. Revisá el aviso amarillo de arriba.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        ultimaTeclaAndroid = -1;
        BotonesService.aprendiz = tecla ->
                // Esperar un instante por si Android también recibió el botón (para saber si es protegido)
                ui.postDelayed(() -> aprendido(tecla), 250);
        dialogoEspera = new AlertDialog.Builder(this)
                .setTitle("Apretá el botón del control que querés configurar")
                .setMessage("Para cancelar, apretá ATRÁS.")
                .setOnKeyListener((d, k, e) -> {
                    if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
                    if (k == KeyEvent.KEYCODE_BACK) { terminarEspera(); return true; }
                    ultimaTeclaAndroid = k;
                    ultimaTeclaHora = SystemClock.elapsedRealtime();
                    return true;
                })
                .setOnDismissListener(d -> BotonesService.aprendiz = null)
                .show();
    }

    private void aprendido(String tecla) {
        if (BotonesService.aprendiz == null) return;   // ya se canceló o ya se aprendió otro
        terminarEspera();
        boolean reciente = SystemClock.elapsedRealtime() - ultimaTeclaHora < 800;
        if (BotonesService.protegido(tecla) || (reciente && protegidoAndroid(ultimaTeclaAndroid))) {
            new AlertDialog.Builder(this)
                    .setMessage(nombre(tecla) + " no se puede cambiar (es de navegación). Probá con otro.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        elegirTipo(tecla);
    }

    private void terminarEspera() {
        BotonesService.aprendiz = null;
        if (dialogoEspera != null && dialogoEspera.isShowing()) dialogoEspera.dismiss();
    }

    // ---------- Elegir qué hace ----------

    private void elegirTipo(String k) {
        String[] op = {
                "Toque corto: " + Acciones.descripcion(this, Datos.accion(this, k, false)),
                "Mantener apretado: " + Acciones.descripcion(this, Datos.accion(this, k, true)),
                "Quitar todo (vuelve a su función normal)"
        };
        new AlertDialog.Builder(this)
                .setTitle(nombre(k))
                .setItems(op, (d, w) -> {
                    if (w == 2) {
                        Datos.guardarAccion(this, k, false, null);
                        Datos.guardarAccion(this, k, true, null);
                        refrescar();
                    } else {
                        elegirAccion(k, w == 1);
                    }
                })
                .show();
    }

    private void elegirAccion(String k, boolean largo) {
        String[] op = {"Ir a Inicio TV", "Abrir una app…", "Nada"};
        new AlertDialog.Builder(this)
                .setTitle(largo ? "Al mantener apretado…" : "Al tocar…")
                .setItems(op, (d, w) -> {
                    if (w == 0) guardar(k, largo, Acciones.INICIO);
                    else if (w == 1) elegirApp(k, largo);
                    else guardar(k, largo, null);
                })
                .show();
    }

    private void elegirApp(String k, boolean largo) {
        List<Datos.App> apps = Datos.apps(this);
        String[] nombres = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) nombres[i] = apps.get(i).nombre;
        new AlertDialog.Builder(this)
                .setTitle("¿Qué app abre?")
                .setItems(nombres, (d, w) -> guardar(k, largo, Acciones.APP + apps.get(w).paquete))
                .show();
    }

    private void guardar(String k, boolean largo, String accion) {
        Datos.guardarAccion(this, k, largo, accion);
        refrescar();
    }
}
