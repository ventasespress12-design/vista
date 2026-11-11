package com.edgar.inicio;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurar botones del control: se aprieta el botón y se elige qué hace
 * con un toque corto y/o manteniéndolo apretado.
 */
public class ConfigControlActivity extends Activity {

    private LinearLayout lista;
    private TextView aviso;
    private AlertDialog dialogoEspera;
    private boolean esperandoBoton = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Estilo.FONDO);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int p = Estilo.dp(this, 40);
        raiz.setPadding(p, Estilo.dp(this, 28), p, p);
        scroll.addView(raiz);
        setContentView(scroll);

        TextView t = new TextView(this);
        t.setText("Botones del control");
        t.setTextColor(0xFFF8FAFC);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        raiz.addView(t);

        aviso = Estilo.titulo(this, "");
        aviso.setTextColor(Estilo.AMARILLO);
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
                + "Si a un botón solo le asignás «mantener apretado», el toque corto deja de hacer lo que hacía antes.");
        ayuda.setTextColor(Estilo.TEXTO_TENUE);
        ayuda.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams la = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        la.topMargin = Estilo.dp(this, 24);
        raiz.addView(ayuda, la);

        agregar.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aviso.setText(ControlService.activo ? ""
                : "El servicio de botones está apagado. Activalo una vez desde la PC:\n"
                + "adb shell settings put secure enabled_accessibility_services com.edgar.inicio/com.edgar.inicio.ControlService\n"
                + "adb shell settings put secure accessibility_enabled 1");
        refrescar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ControlService.aprendiendo = false;
    }

    private void refrescar() {
        lista.removeAllViews();
        List<Integer> codigos = new ArrayList<>(Datos.botonesAsignados(this));
        java.util.Collections.sort(codigos);
        for (int k : codigos) {
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

    private static String nombre(int k) {
        return "Botón " + KeyEvent.keyCodeToString(k).replace("KEYCODE_", "") + " (" + k + ")";
    }

    // ---------- Aprender un botón ----------

    private void esperarBoton() {
        esperandoBoton = true;
        ControlService.aprendiendo = true;   // que el servicio no se lo "coma" mientras lo aprendemos
        dialogoEspera = new AlertDialog.Builder(this)
                .setTitle("Apretá el botón del control que querés configurar")
                .setMessage("(Para cancelar, apretá ATRÁS)")
                .setOnKeyListener((d, k, e) -> {
                    if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (k == KeyEvent.KEYCODE_BACK) { terminarEspera(); return true; }
                    if (ControlService.protegido(k)) {
                        d.dismiss();
                        terminarEspera();
                        new AlertDialog.Builder(this)
                                .setMessage("Ese botón (" + KeyEvent.keyCodeToString(k).replace("KEYCODE_", "")
                                        + ") no se puede cambiar. Probá con otro.")
                                .setPositiveButton("OK", null).show();
                        return true;
                    }
                    d.dismiss();
                    terminarEspera();
                    elegirTipo(k);
                    return true;
                })
                .show();
    }

    private void terminarEspera() {
        esperandoBoton = false;
        ControlService.aprendiendo = false;
        if (dialogoEspera != null && dialogoEspera.isShowing()) dialogoEspera.dismiss();
    }

    // ---------- Elegir qué hace ----------

    private void elegirTipo(int k) {
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

    private void elegirAccion(int k, boolean largo) {
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

    private void elegirApp(int k, boolean largo) {
        List<Datos.App> apps = Datos.apps(this);
        String[] nombres = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) nombres[i] = apps.get(i).nombre;
        new AlertDialog.Builder(this)
                .setTitle("¿Qué app abre?")
                .setItems(nombres, (d, w) -> guardar(k, largo, Acciones.APP + apps.get(w).paquete))
                .show();
    }

    private void guardar(int k, boolean largo, String accion) {
        Datos.guardarAccion(this, k, largo, accion);
        refrescar();
    }
}
