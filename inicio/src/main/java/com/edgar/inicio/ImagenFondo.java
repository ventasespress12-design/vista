package com.edgar.inicio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Fondo con una foto elegida por el usuario.
 * - Guarda una copia recortada a 16:9 (así el fondo no se deforma y no depende del archivo original).
 * - Saca de la foto el color de detalle (el más vivo y frecuente) y un color de fondo de respaldo.
 */
public final class ImagenFondo {

    private static final int ANCHO = 1280, ALTO = 720;

    private ImagenFondo() {}

    public static File archivo(Context c) {
        return new File(c.getFilesDir(), "fondo.jpg");
    }

    public static boolean existe(Context c) {
        return archivo(c).exists();
    }

    /** Cargar la copia guardada (ya viene en 1280x720). */
    public static Bitmap cargar(Context c) {
        try {
            return BitmapFactory.decodeFile(archivo(c).getAbsolutePath());
        } catch (Throwable e) {
            return null;
        }
    }

    /** Miniatura liviana para la galería. */
    public static Bitmap miniatura(File f, int lado) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        if (o.outWidth <= 0) return null;
        o.inSampleSize = Math.max(1, Math.min(o.outWidth, o.outHeight) / lado);
        o.inJustDecodeBounds = false;
        o.inPreferredConfig = Bitmap.Config.RGB_565;   // la mitad de memoria
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    /**
     * Procesa la foto elegida: la recorta, la guarda y calcula los colores.
     * @return true si salió bien
     */
    public static boolean usar(Context c, File original) {
        Bitmap recorte = null;
        try {
            // 1) Leer reducida (cuidando la memoria del stick)
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(original.getAbsolutePath(), o);
            if (o.outWidth <= 0) return false;
            int muestra = 1;
            while (o.outWidth / (muestra * 2) >= ANCHO && o.outHeight / (muestra * 2) >= ALTO) muestra *= 2;
            o.inSampleSize = muestra;
            o.inJustDecodeBounds = false;
            Bitmap b = BitmapFactory.decodeFile(original.getAbsolutePath(), o);
            if (b == null) return false;

            // 2) Recortar al centro a 16:9 y escalar a 1280x720
            float escala = Math.max(ANCHO / (float) b.getWidth(), ALTO / (float) b.getHeight());
            int w = Math.round(ANCHO / escala), h = Math.round(ALTO / escala);
            int x = Math.max(0, (b.getWidth() - w) / 2), y = Math.max(0, (b.getHeight() - h) / 2);
            w = Math.min(w, b.getWidth() - x);
            h = Math.min(h, b.getHeight() - y);
            Bitmap centro = Bitmap.createBitmap(b, x, y, w, h);
            recorte = Bitmap.createScaledBitmap(centro, ANCHO, ALTO, true);
            if (centro != b) centro.recycle();
            b.recycle();

            // 3) Guardar
            try (FileOutputStream out = new FileOutputStream(archivo(c))) {
                recorte.compress(Bitmap.CompressFormat.JPEG, 88, out);
            }

            // 4) Colores
            int[] colores = colores(recorte);
            Datos.prefs(c).edit()
                    .putInt("img_detalle", colores[0])
                    .putInt("img_fondo", colores[1])
                    .apply();
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            if (recorte != null) recorte.recycle();
        }
    }

    /**
     * [0] color de detalle: el tono vivo más frecuente, aclarado para que resalte sobre el fondo oscurecido.
     * [1] color de fondo: el promedio de la foto, oscurecido (se usa de respaldo).
     */
    static int[] colores(Bitmap b) {
        Bitmap chica = Bitmap.createScaledBitmap(b, 64, 36, true);
        float[] hsv = new float[3];
        float[] peso = new float[12];                 // 12 "cajones" de tono (cada 30°)
        float[][] suma = new float[12][3];            // suma de R,G,B por cajón
        long r = 0, g = 0, bl = 0;
        int n = 0;
        for (int yy = 0; yy < chica.getHeight(); yy++) {
            for (int xx = 0; xx < chica.getWidth(); xx++) {
                int p = chica.getPixel(xx, yy);
                r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++;
                Color.colorToHSV(p, hsv);
                if (hsv[1] < 0.25f || hsv[2] < 0.25f) continue;   // grises y oscuros no cuentan
                int cajon = ((int) (hsv[0] / 30f)) % 12;
                float w = hsv[1] * hsv[2];                       // más vivo = más peso
                peso[cajon] += w;
                suma[cajon][0] += Color.red(p) * w;
                suma[cajon][1] += Color.green(p) * w;
                suma[cajon][2] += Color.blue(p) * w;
            }
        }
        if (chica != b) chica.recycle();

        int mejor = 0;
        for (int i = 1; i < 12; i++) if (peso[i] > peso[mejor]) mejor = i;

        int detalle;
        if (peso[mejor] > 0) {
            int c = Color.rgb(Math.round(suma[mejor][0] / peso[mejor]),
                    Math.round(suma[mejor][1] / peso[mejor]),
                    Math.round(suma[mejor][2] / peso[mejor]));
            Color.colorToHSV(c, hsv);
            hsv[1] = Math.min(hsv[1], 0.65f);   // sin colores chillones
            hsv[2] = Math.max(hsv[2], 0.92f);   // claro, para que se vea sobre fondo oscuro
            detalle = Color.HSVToColor(hsv);
        } else {
            detalle = Color.rgb(226, 232, 240);  // foto en blanco y negro: detalle gris claro
        }

        int promedio = Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n));
        Color.colorToHSV(promedio, hsv);
        hsv[2] = Math.min(hsv[2], 0.18f);
        int fondo = Color.HSVToColor(hsv);
        return new int[]{detalle, fondo};
    }
}
