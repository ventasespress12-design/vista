package com.edgar.inicio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Lee los botones del control directamente del sistema (con root, comando getevent)
 * y ejecuta la acción asignada. No usa accesibilidad porque este stick la bloquea.
 * Limitación: el botón hace su acción nueva Y también la que tenía antes.
 * También atiende el teclado del celular (ServidorTeclado).
 */
public class BotonesService extends Service {

    /** Mientras la configuración aprende un botón, recibe acá el nombre y no se ejecuta nada. */
    public interface Aprendiz { void tecla(String nombre); }

    public static volatile Aprendiz aprendiz;
    public static volatile boolean leyendo = false;
    public static volatile String error = null;

    private static final long LARGO_MS = 600;
    private static final String CANAL = "botones";

    /** Botones que nunca se tocan, para no perder el control del stick. */
    private static final Set<String> PROTEGIDOS = new HashSet<>(Arrays.asList(
            "KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT", "KEY_ENTER", "KEY_KPENTER", "KEY_OK",
            "KEY_SELECT", "KEY_REPLY", "KEY_BACK", "KEY_ESC", "KEY_HOME", "KEY_HOMEPAGE",
            "KEY_POWER", "KEY_VOLUMEUP", "KEY_VOLUMEDOWN", "KEY_MUTE"));

    private final Handler h = new Handler(Looper.getMainLooper());
    private volatile boolean activo = true;
    private Thread hilo;
    private ServidorTeclado teclado;

    private String teclaApretada;
    private boolean largoHecho;
    private Runnable pendiente;

    public static boolean protegido(String tecla) {
        return PROTEGIDOS.contains(tecla) || tecla.startsWith("BTN_");
    }

    /** Arranca el servicio (se llama al abrir Inicio TV y al prender el stick). */
    public static void iniciar(Context c) {
        Intent i = new Intent(c, BotonesService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception ignored) { }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(CANAL, "Botones del control", NotificationManager.IMPORTANCE_MIN));
            Notification n = new Notification.Builder(this, CANAL)
                    .setContentTitle("Inicio TV: botones del control")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .build();
            startForeground(1, n);
        }
        if (hilo == null) {
            hilo = new Thread(this::bucle, "botones");
            hilo.start();
        }
        if (teclado == null) {
            teclado = new ServidorTeclado(this);       // teclado desde el celular
            new Thread(teclado, "teclado").start();
        }
        return START_STICKY;
    }

    private void bucle() {
        int fallos = 0;
        while (activo) {
            Process p = null;
            try {
                // "LISTO" sale apenas se obtiene root, antes de que se apriete ningún botón
                p = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo LISTO; exec getevent -lq"});
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String linea;
                while (activo && (linea = in.readLine()) != null) {
                    if (linea.trim().equals("LISTO")) {
                        leyendo = true;
                        error = null;
                        fallos = 0;
                        continue;
                    }
                    procesar(linea);
                }
            } catch (Exception e) {
                error = "No se pudo leer el control: " + e.getMessage();
            } finally {
                leyendo = false;
                if (p != null) p.destroy();
            }
            if (!activo) break;
            fallos++;
            if (error == null) error = "No hay permiso de root para leer el control.";
            SystemClock.sleep(Math.min(30000, 3000L * fallos));   // reintenta cada vez más espaciado
        }
    }

    /** Línea típica: "/dev/input/event2: EV_KEY       KEY_RO               DOWN" */
    private void procesar(String linea) {
        String[] t = linea.trim().split("\\s+");
        int i = Arrays.asList(t).indexOf("EV_KEY");
        if (i < 0 || i + 2 >= t.length) return;
        String tecla = t[i + 1];
        String estado = t[i + 2];
        if (tecla.startsWith("BTN_")) return;   // clics del modo mouse

        Aprendiz a = aprendiz;
        if (a != null) {
            if (estado.equals("DOWN")) h.post(() -> a.tecla(tecla));
            return;
        }
        if (protegido(tecla)) return;

        String corto = Datos.accion(this, tecla, false);
        String largo = Datos.accion(this, tecla, true);
        if (corto == null && largo == null) return;

        if (estado.equals("DOWN")) {
            h.post(() -> {
                teclaApretada = tecla;
                largoHecho = false;
                if (largo == null) {
                    largoHecho = true;
                    Acciones.ejecutar(this, corto);
                } else {
                    pendiente = () -> { largoHecho = true; Acciones.ejecutar(this, largo); };
                    h.postDelayed(pendiente, LARGO_MS);
                }
            });
        } else if (estado.equals("UP")) {
            h.post(() -> {
                if (!tecla.equals(teclaApretada)) return;
                if (pendiente != null) h.removeCallbacks(pendiente);
                pendiente = null;
                if (!largoHecho && corto != null) Acciones.ejecutar(this, corto);
                teclaApretada = null;
            });
        }
    }

    @Override
    public void onDestroy() {
        activo = false;
        if (hilo != null) hilo.interrupt();
        if (teclado != null) teclado.detener();
        super.onDestroy();
    }
}
