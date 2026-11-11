package com.edgar.inicio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Recibe lo que se escribe en la app "Teclado TV" del celular y lo tipea en la TV (con root).
 *
 * Protocolo (una línea por mensaje, UTF-8):
 *   H|1234     → saludo con el código de vinculación (obligatorio primero)
 *   T|texto    → escribir texto en el campo que tenga el foco
 *   K|67       → apretar una tecla (código de Android: 67 borrar, 66 enter, 19-23 flechas/OK, 4 atrás, 3 inicio)
 * Respuestas: "OK" o "ERROR|motivo".
 */
final class ServidorTeclado implements Runnable {

    static final int PUERTO = 5053;

    private final Context ctx;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean activo = true;
    private ServerSocket servidor;

    ServidorTeclado(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /** Código de 4 dígitos que pide el celular. Se crea una vez y queda guardado. */
    static String codigo(Context c) {
        String k = Datos.prefs(c).getString("codigo_teclado", null);
        if (k == null) k = nuevoCodigo(c);
        return k;
    }

    static String nuevoCodigo(Context c) {
        String k = String.format("%04d", new SecureRandom().nextInt(10000));
        Datos.prefs(c).edit().putString("codigo_teclado", k).apply();
        return k;
    }

    void detener() {
        activo = false;
        try { if (servidor != null) servidor.close(); } catch (Exception ignored) { }
    }

    @Override
    public void run() {
        try {
            servidor = new ServerSocket(PUERTO);
            while (activo) {
                Socket s = servidor.accept();
                new Thread(() -> atender(s), "teclado-cliente").start();
            }
        } catch (Exception ignored) { }
    }

    private void atender(Socket s) {
        try (Socket sock = s;
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))) {
            sock.setTcpNoDelay(true);
            OutputStream out = sock.getOutputStream();
            boolean vinculado = false;
            String linea;
            while (activo && (linea = in.readLine()) != null) {
                if (linea.length() < 2 || linea.charAt(1) != '|') continue;
                char tipo = linea.charAt(0);
                String dato = linea.substring(2);

                if (!vinculado) {
                    if (tipo == 'H' && dato.trim().equals(codigo(ctx))) {
                        vinculado = true;
                        responder(out, "OK");
                    } else {
                        responder(out, "ERROR|Código incorrecto");
                        return;
                    }
                    continue;
                }

                if (tipo == 'T') escribir(dato);
                else if (tipo == 'K') tecla(dato);
                responder(out, "OK");
            }
        } catch (Exception ignored) { }
    }

    private static void responder(OutputStream out, String r) throws Exception {
        out.write((r + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void tecla(String codigo) {
        try {
            int k = Integer.parseInt(codigo.trim());
            Raiz.ejecutar("input keyevent " + k);
        } catch (NumberFormatException ignored) { }
    }

    /**
     * El comando "input text" solo sirve para letras sin tildes. Para tildes, ñ o signos raros
     * se usa el portapapeles + "pegar".
     */
    private void escribir(String texto) {
        if (texto.isEmpty()) return;
        boolean simple = true;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < 32 || c > 126) { simple = false; break; }
        }
        if (simple) {
            // Espacios como %s; el resto entre comillas simples para que la consola no lo interprete
            String seguro = texto.replace("%", "\\%").replace(" ", "%s").replace("'", "'\\''");
            Raiz.ejecutar("input text '" + seguro + "'");
        } else {
            ui.post(() -> {
                try {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("teclado", texto));
                    Raiz.ejecutar("input keyevent 279");   // PEGAR
                } catch (Exception ignored) { }
            });
        }
    }
}
