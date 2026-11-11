package com.edgar.inicio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Colores y "baldosas" (íconos con nombre) de Inicio TV, hechos sin XML para que cargue rápido.
 * Tres paletas fijas o "Mi imagen" (foto propia con colores sacados de la foto), en Ajustes → Colores.
 */
public final class Estilo {

    /** Una paleta completa. */
    public static final class Tema {
        final String nombre;
        final int fondoArriba, fondoAbajo;   // degradé vertical del fondo
        final int baldosa, baldosaFoco;      // íconos normal / seleccionado (blanco translúcido)
        final int detalle;                   // borde fino de la selección y avisos
        final int texto, textoSuave, textoTenue;

        Tema(String nombre, int fondoArriba, int fondoAbajo, int baldosa, int baldosaFoco,
             int detalle, int texto, int textoSuave, int textoTenue) {
            this.nombre = nombre; this.fondoArriba = fondoArriba; this.fondoAbajo = fondoAbajo;
            this.baldosa = baldosa; this.baldosaFoco = baldosaFoco; this.detalle = detalle;
            this.texto = texto; this.textoSuave = textoSuave; this.textoTenue = textoTenue;
        }
    }

    public static final Tema[] TEMAS = {
            new Tema("Grafito",
                    Color.rgb(26, 28, 35), Color.rgb(14, 15, 19),
                    Color.argb(16, 255, 255, 255), Color.argb(41, 255, 255, 255),
                    Color.rgb(94, 234, 212),
                    Color.rgb(245, 245, 244), Color.rgb(168, 162, 158), Color.rgb(120, 113, 108)),
            new Tema("Atardecer",
                    Color.rgb(59, 31, 74), Color.rgb(27, 16, 41),
                    Color.argb(20, 255, 255, 255), Color.argb(46, 255, 255, 255),
                    Color.rgb(253, 186, 116),
                    Color.rgb(255, 247, 237), Color.rgb(216, 180, 254), Color.rgb(167, 139, 250)),
            new Tema("Océano",
                    Color.rgb(15, 59, 76), Color.rgb(7, 26, 36),
                    Color.argb(18, 255, 255, 255), Color.argb(43, 255, 255, 255),
                    Color.rgb(125, 211, 252),
                    Color.rgb(240, 249, 255), Color.rgb(165, 201, 219), Color.rgb(107, 147, 166)),
    };

    /** Número de la opción "Mi imagen" (después de las paletas fijas). */
    public static final int MI_IMAGEN = 3;

    // Colores de la paleta elegida (se cargan con cargar())
    public static int FONDO, DETALLE, TEXTO, TEXTO_SUAVE, TEXTO_TENUE;
    private static Tema actual = TEMAS[0];
    private static boolean conImagen = false;
    private static Bitmap imagen;            // fondo cargado (se guarda en memoria una sola vez)
    private static long imagenVersion = -1;

    private Estilo() {}

    /** Lee la paleta elegida. Llamar al crear cada pantalla. */
    public static void cargar(Context c) {
        int i = Datos.prefs(c).getInt("tema", 0);
        conImagen = i == MI_IMAGEN && ImagenFondo.existe(c);
        if (conImagen) {
            int detalle = Datos.prefs(c).getInt("img_detalle", Color.WHITE);
            int fondo = Datos.prefs(c).getInt("img_fondo", Color.BLACK);
            actual = new Tema("Mi imagen", fondo, fondo,
                    Color.argb(46, 255, 255, 255), Color.argb(84, 255, 255, 255), detalle,
                    Color.WHITE, Color.argb(215, 255, 255, 255), Color.argb(160, 255, 255, 255));
            long v = ImagenFondo.archivo(c).lastModified();
            if (imagen == null || v != imagenVersion) {
                imagen = ImagenFondo.cargar(c);
                imagenVersion = v;
            }
        } else {
            actual = TEMAS[Math.max(0, Math.min(TEMAS.length - 1, i))];
            imagen = null;            // la libera el sistema cuando ya no se muestre
            imagenVersion = -1;
        }
        FONDO = actual.fondoAbajo;
        DETALLE = actual.detalle;
        TEXTO = actual.texto;
        TEXTO_SUAVE = actual.textoSuave;
        TEXTO_TENUE = actual.textoTenue;
    }

    public static int temaActual(Context c) {
        return Datos.prefs(c).getInt("tema", 0);
    }

    public static void elegirTema(Context c, int i) {
        Datos.prefs(c).edit().putInt("tema", i).apply();
        cargar(c);
    }

    /** Fondo de pantalla: degradé suave, o la foto con un velo oscuro para que se lean los textos. */
    public static void ponerFondo(View v) {
        if (conImagen && imagen != null) {
            BitmapDrawable foto = new BitmapDrawable(v.getResources(), imagen);
            GradientDrawable velo = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.argb(95, 0, 0, 0), Color.argb(150, 0, 0, 0), Color.argb(205, 0, 0, 0)});
            v.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{foto, velo}));
            return;
        }
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{actual.fondoArriba, actual.fondoAbajo});
        v.setBackground(g);
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private static GradientDrawable caja(Context c, int color, int borde) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 14));
        if (borde != 0) g.setStroke(dp(c, 2), borde);
        return g;
    }

    /** Fondo translúcido que se ilumina (con borde fino del color de detalle) al seleccionarla. */
    public static StateListDrawable fondoBaldosa(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, caja(c, actual.baldosaFoco, actual.detalle));
        s.addState(new int[]{}, caja(c, actual.baldosa, 0));
        return s;
    }

    /** Ícono + nombre, seleccionable con el control. Grande para favoritos, chica para la lista. */
    public static LinearLayout baldosa(Context c, Drawable icono, String nombre, boolean grande) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackground(fondoBaldosa(c));
        int pad = dp(c, grande ? 12 : 8);
        b.setPadding(pad, pad, pad, pad);

        ImageView img = new ImageView(c);
        img.setImageDrawable(icono);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int t = dp(c, grande ? 56 : 36);
        b.addView(img, new LinearLayout.LayoutParams(t, t));

        TextView tv = new TextView(c);
        tv.setText(nombre);
        tv.setTextColor(actual.texto);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, grande ? 15 : 12);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 6);
        b.addView(tv, lp);

        // Al seleccionarla: crece un poco y se "levanta"
        b.setOnFocusChangeListener((v, foco) -> {
            v.animate().scaleX(foco ? 1.08f : 1f).scaleY(foco ? 1.08f : 1f)
                    .translationZ(foco ? dp(c, 6) : 0).setDuration(140).start();
        });
        return b;
    }

    public static TextView titulo(Context c, String texto) {
        TextView t = new TextView(c);
        t.setText(texto);
        t.setTextColor(actual.textoSuave);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setLetterSpacing(0.04f);
        return t;
    }
}
