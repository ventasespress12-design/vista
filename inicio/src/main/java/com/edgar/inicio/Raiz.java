package com.edgar.inicio;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Consola con permisos de root (el stick lo trae de fábrica).
 * Se usa para abrir apps aunque Inicio TV esté en segundo plano,
 * algo que Android 10 no le permite a una app normal.
 */
final class Raiz {

    private static Process shell;
    private static OutputStream entrada;

    private Raiz() {}

    /** Ejecuta un comando como root. Devuelve false si no hay root disponible. */
    static synchronized boolean ejecutar(String comando) {
        try {
            if (shell == null || !vivo()) abrir();
            entrada.write((comando + "\n").getBytes(StandardCharsets.UTF_8));
            entrada.flush();
            return true;
        } catch (Exception e) {
            shell = null;
            return false;
        }
    }

    private static void abrir() throws Exception {
        shell = Runtime.getRuntime().exec("su");
        entrada = shell.getOutputStream();
        descartar(shell.getInputStream());
        descartar(shell.getErrorStream());
    }

    /** Lee y descarta la salida para que la consola nunca se trabe. */
    private static void descartar(InputStream in) {
        Thread t = new Thread(() -> {
            byte[] b = new byte[1024];
            try { while (in.read(b) >= 0) { /* nada */ } } catch (Exception ignored) { }
        }, "raiz-salida");
        t.setDaemon(true);
        t.start();
    }

    private static boolean vivo() {
        try { shell.exitValue(); return false; } catch (IllegalThreadStateException e) { return true; }
    }
}
