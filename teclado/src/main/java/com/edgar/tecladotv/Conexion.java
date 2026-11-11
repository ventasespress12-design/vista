package com.edgar.tecladotv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Conexión con Inicio TV en el stick (puerto 5053).
 * Los mensajes se encolan y se mandan en orden desde un hilo aparte.
 */
final class Conexion implements Runnable {

    interface Oyente {
        void conectado();
        void desconectado(String motivo);
    }

    private static final int PUERTO = 5053;

    private final String ip, codigo;
    private final Oyente oyente;
    private final LinkedBlockingQueue<String> cola = new LinkedBlockingQueue<>();
    private volatile boolean activa = true;
    private volatile Socket socket;

    Conexion(String ip, String codigo, Oyente oyente) {
        this.ip = ip;
        this.codigo = codigo;
        this.oyente = oyente;
    }

    void texto(String t) { if (!t.isEmpty()) cola.offer("T|" + t.replace('\n', ' ')); }
    void tecla(int codigoAndroid) { cola.offer("K|" + codigoAndroid); }

    void cerrar() {
        activa = false;
        Socket s = socket;
        if (s != null) try { s.close(); } catch (Exception ignored) { }
    }

    @Override
    public void run() {
        try (Socket s = new Socket()) {
            socket = s;
            s.connect(new InetSocketAddress(ip, PUERTO), 5000);
            s.setTcpNoDelay(true);
            OutputStream out = s.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

            out.write(("H|" + codigo + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            String r = in.readLine();
            if (r == null || !r.equals("OK")) {
                oyente.desconectado(r != null && r.startsWith("ERROR|") ? r.substring(6) : "El stick no respondió");
                return;
            }
            oyente.conectado();

            // Las respuestas "OK" se leen y descartan en otro hilo, para no trabar el envío
            Thread lector = new Thread(() -> {
                try { while (in.readLine() != null) { /* nada */ } } catch (Exception ignored) { }
                cerrar();
            }, "lector");
            lector.setDaemon(true);
            lector.start();

            while (activa) {
                String m = cola.poll(500, TimeUnit.MILLISECONDS);
                if (m == null) continue;
                out.write((m + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (Exception e) {
            if (activa) oyente.desconectado("No se pudo conectar: " + e.getMessage());
            activa = false;
            return;
        }
        oyente.desconectado("Desconectado");
    }
}
