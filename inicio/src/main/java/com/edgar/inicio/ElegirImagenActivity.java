package com.edgar.inicio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Galería para elegir la foto de fondo con el control.
 * Busca en Imágenes, Descargas y Cámara del stick, y en pendrives conectados.
 */
public class ElegirImagenActivity extends Activity {

    private static final int MAX_FOTOS = 80;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout raiz;
    private volatile boolean visible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Estilo.cargar(this);
        ScrollView scroll = new ScrollView(this);
        Estilo.ponerFondo(scroll);
        raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int p = Estilo.dp(this, 40);
        raiz.setPadding(p, Estilo.dp(this, 28), p, p);
        scroll.addView(raiz);
        setContentView(scroll);

        TextView t = new TextView(this);
        t.setText("Elegí una foto de fondo");
        t.setTextColor(Estilo.TEXTO);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        raiz.addView(t);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            buscar();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            buscar();
        } else {
            mensaje("Sin permiso para ver las fotos. Desde la PC:\n"
                    + "adb shell pm grant com.edgar.inicio android.permission.READ_EXTERNAL_STORAGE");
        }
    }

    @Override
    protected void onDestroy() {
        visible = false;
        super.onDestroy();
    }

    private void mensaje(String texto) {
        TextView m = Estilo.titulo(this, texto);
        m.setPadding(0, Estilo.dp(this, 16), 0, 0);
        raiz.addView(m);
    }

    // ---------- Buscar fotos ----------

    private void buscar() {
        TextView buscando = Estilo.titulo(this, "Buscando fotos…");
        raiz.addView(buscando);
        new Thread(() -> {
            List<File> fotos = new ArrayList<>();
            File sd = Environment.getExternalStorageDirectory();
            for (String carpeta : new String[]{"Pictures", "Download", "DCIM", "Wallpapers"}) {
                juntar(new File(sd, carpeta), fotos, 3);
            }
            // Pendrives y tarjetas: /storage/XXXX-XXXX
            File[] almacenes = new File("/storage").listFiles();
            if (almacenes != null) {
                for (File a : almacenes) {
                    String n = a.getName();
                    if (n.equals("emulated") || n.equals("self")) continue;
                    juntar(a, fotos, 3);
                }
            }
            Collections.sort(fotos, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
            List<File> elegidas = fotos.size() > MAX_FOTOS ? fotos.subList(0, MAX_FOTOS) : fotos;
            ui.post(() -> {
                raiz.removeView(buscando);
                mostrar(new ArrayList<>(elegidas));
            });
        }, "buscar-fotos").start();
    }

    private static void juntar(File dir, List<File> fotos, int profundidad) {
        if (profundidad < 0 || dir == null || !dir.isDirectory() || fotos.size() > 500) return;
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                juntar(f, fotos, profundidad - 1);
            } else {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")) {
                    fotos.add(f);
                }
            }
        }
    }

    // ---------- Mostrar la grilla ----------

    private void mostrar(List<File> fotos) {
        if (fotos.isEmpty()) {
            mensaje("No encontré fotos.\n\n"
                    + "Copiá la imagen que te guste al stick, por ejemplo desde la PC:\n"
                    + "adb push mi_foto.jpg /sdcard/Pictures/\n\n"
                    + "o conectá un pendrive con fotos y volvé a entrar.");
            return;
        }
        mensaje("Mové con las flechas y apretá OK sobre la foto que quieras.");

        int anchoDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int columnas = Math.max(3, (anchoDp - 80) / 190);
        GridLayout grilla = new GridLayout(this);
        grilla.setColumnCount(columnas);
        grilla.setClipChildren(false);
        raiz.addView(grilla);

        List<ImageView> vistas = new ArrayList<>();
        for (File f : fotos) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setFocusable(true);
            img.setClickable(true);
            img.setBackground(Estilo.fondoBaldosa(this));
            int pad = Estilo.dp(this, 4);
            img.setPadding(pad, pad, pad, pad);
            img.setOnClickListener(v -> usar(f));
            img.setOnFocusChangeListener((v, foco) ->
                    v.animate().scaleX(foco ? 1.08f : 1f).scaleY(foco ? 1.08f : 1f).setDuration(120).start());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Estilo.dp(this, 170);
            lp.height = Estilo.dp(this, 96);
            int m = Estilo.dp(this, 8);
            lp.setMargins(0, m, Estilo.dp(this, 14), m);
            grilla.addView(img, lp);
            vistas.add(img);
        }
        vistas.get(0).requestFocus();

        // Miniaturas de a una, en segundo plano, para no llenar la memoria del stick
        new Thread(() -> {
            for (int i = 0; i < fotos.size() && visible; i++) {
                Bitmap b = null;
                try { b = ImagenFondo.miniatura(fotos.get(i), 200); } catch (Throwable ignored) { }
                if (b == null) continue;
                Bitmap fb = b;
                ImageView v = vistas.get(i);
                ui.post(() -> v.setImageBitmap(fb));
            }
        }, "miniaturas").start();
    }

    // ---------- Usar la elegida ----------

    private void usar(File f) {
        AlertDialog espera = new AlertDialog.Builder(this)
                .setMessage("Preparando el fondo y sacando los colores de la foto…")
                .setCancelable(false)
                .show();
        new Thread(() -> {
            boolean ok = ImagenFondo.usar(this, f);
            ui.post(() -> {
                espera.dismiss();
                if (ok) {
                    Estilo.elegirTema(this, Estilo.MI_IMAGEN);
                    finish();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("No se pudo usar esa foto. Probá con otra.")
                            .setPositiveButton("OK", null).show();
                }
            });
        }, "usar-foto").start();
    }
}
