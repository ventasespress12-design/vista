package com.edgar.inicio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Colores y "baldosas" (íconos con nombre) de Inicio TV, hechos sin XML para que cargue rápido. */
public final class Estilo {

    public static final int FONDO = Color.rgb(15, 23, 42);
    public static final int BALDOSA = Color.rgb(30, 41, 59);
    public static final int BALDOSA_FOCO = Color.rgb(51, 65, 85);
    public static final int AMARILLO = Color.rgb(250, 204, 21);
    public static final int TEXTO = Color.rgb(229, 231, 235);
    public static final int TEXTO_SUAVE = Color.rgb(148, 163, 184);
    public static final int TEXTO_TENUE = Color.rgb(100, 116, 139);

    private Estilo() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private static GradientDrawable caja(Context c, int color, int borde) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 12));
        if (borde != 0) g.setStroke(dp(c, 3), borde);
        return g;
    }

    /** Fondo que se marca en amarillo cuando el control la selecciona. */
    public static StateListDrawable fondoBaldosa(Context c) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, caja(c, BALDOSA_FOCO, AMARILLO));
        s.addState(new int[]{}, caja(c, BALDOSA, 0));
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
        tv.setTextColor(TEXTO);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, grande ? 15 : 12);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 6);
        b.addView(tv, lp);

        // Se agranda un poco al recibir el foco
        b.setOnFocusChangeListener((v, foco) ->
                v.animate().scaleX(foco ? 1.08f : 1f).scaleY(foco ? 1.08f : 1f).setDuration(120).start());
        return b;
    }

    public static TextView titulo(Context c, String texto) {
        TextView t = new TextView(c);
        t.setText(texto);
        t.setTextColor(TEXTO_SUAVE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return t;
    }
}
