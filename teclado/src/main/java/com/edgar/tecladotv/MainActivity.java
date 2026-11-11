package com.edgar.tecladotv;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Teclado TV: lo que escribís acá aparece en la TV mientras tipeás.
 * También trae un control remoto (flechas, OK, Atrás, Inicio).
 */
public class MainActivity extends Activity implements Conexion.Oyente {

    // Códigos de tecla de Android
    private static final int BORRAR = 67, ENTER = 66, ARRIBA = 19, ABAJO = 20,
            IZQUIERDA = 21, DERECHA = 22, OK = 23, ATRAS = 4, INICIO = 3;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private EditText campoIp, campoCodigo, texto;
    private Button btnConectar;
    private TextView estado;

    private Conexion conexion;
    private boolean conectado = false;

    /** Lo que ya se mandó a la TV (para enviar solo la diferencia mientras tipeás). */
    private String enviado = "";
    private boolean ignorarCambio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("config", MODE_PRIVATE);

        campoIp = findViewById(R.id.ip);
        campoCodigo = findViewById(R.id.codigo);
        texto = findViewById(R.id.texto);
        btnConectar = findViewById(R.id.btnConectar);
        estado = findViewById(R.id.estado);

        campoIp.setText(prefs.getString("ip", "192.168."));
        campoCodigo.setText(prefs.getString("codigo", ""));
        btnConectar.setOnClickListener(v -> { if (conectado || conexion != null) desconectar(); else conectar(); });

        // Escribir en vivo: se manda solo lo que cambió
        texto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                if (ignorarCambio) return;
                sincronizar(e.toString());
            }
        });
        texto.setOnEditorActionListener((v, accion, ev) -> {
            if (accion == EditorInfo.IME_ACTION_SEND || accion == EditorInfo.IME_ACTION_DONE) {
                tecla(ENTER);
                limpiarLocal();
                return true;
            }
            return false;
        });

        boton(R.id.btnBorrar, () -> {
            tecla(BORRAR);
            String t = texto.getText().toString();
            if (!t.isEmpty()) {
                ignorarCambio = true;
                texto.setText(t.substring(0, t.length() - 1));
                texto.setSelection(texto.length());
                ignorarCambio = false;
                enviado = texto.getText().toString();
            }
        });
        boton(R.id.btnEnter, () -> { tecla(ENTER); limpiarLocal(); });
        boton(R.id.btnLimpiar, this::limpiarLocal);
        boton(R.id.btnArriba, () -> tecla(ARRIBA));
        boton(R.id.btnAbajo, () -> tecla(ABAJO));
        boton(R.id.btnIzquierda, () -> tecla(IZQUIERDA));
        boton(R.id.btnDerecha, () -> tecla(DERECHA));
        boton(R.id.btnOk, () -> tecla(OK));
        boton(R.id.btnAtras, () -> tecla(ATRAS));
        boton(R.id.btnInicio, () -> tecla(INICIO));

        // Si ya había datos guardados, conectar solo
        if (!prefs.getString("codigo", "").isEmpty()) conectar();
    }

    private void boton(int id, Runnable accion) {
        View b = findViewById(id);
        b.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            accion.run();
        });
    }

    /** Compara con lo ya enviado: manda borrados por lo que se quitó y texto por lo que se agregó. */
    private void sincronizar(String nuevo) {
        int comun = 0;
        int max = Math.min(nuevo.length(), enviado.length());
        while (comun < max && nuevo.charAt(comun) == enviado.charAt(comun)) comun++;
        for (int i = enviado.length(); i > comun; i--) tecla(BORRAR);
        if (nuevo.length() > comun && conexion != null) conexion.texto(nuevo.substring(comun));
        enviado = nuevo;
    }

    /** Vacía el campo del celular sin borrar nada en la TV. */
    private void limpiarLocal() {
        ignorarCambio = true;
        texto.setText("");
        ignorarCambio = false;
        enviado = "";
    }

    private void tecla(int k) {
        if (conexion != null) conexion.tecla(k);
        else estado.setText("Primero conectá con el stick.");
    }

    // ---------- Conexión ----------

    private void conectar() {
        String ip = campoIp.getText().toString().trim();
        String codigo = campoCodigo.getText().toString().trim();
        if (!ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) { estado.setText("La IP no es válida (ej. 192.168.100.12)."); return; }
        if (!codigo.matches("\\d{4}")) { estado.setText("El código tiene 4 números."); return; }
        prefs.edit().putString("ip", ip).putString("codigo", codigo).apply();

        estado.setText("Conectando…");
        btnConectar.setText("Cancelar");
        conexion = new Conexion(ip, codigo, this);
        new Thread(conexion, "conexion").start();
    }

    private void desconectar() {
        Conexion c = conexion;
        conexion = null;
        conectado = false;
        if (c != null) c.cerrar();
        estado.setText("Desconectado.");
        btnConectar.setText("Conectar");
        findViewById(R.id.zonaConexion).setVisibility(View.VISIBLE);
    }

    @Override
    public void conectado() {
        ui.post(() -> {
            conectado = true;
            estado.setText("✔ Conectado. Elegí un campo de texto en la TV y escribí acá.");
            btnConectar.setText("Desconectar");
            findViewById(R.id.zonaConexion).setVisibility(View.GONE);
            texto.requestFocus();
        });
    }

    @Override
    public void desconectado(String motivo) {
        ui.post(() -> {
            conectado = false;
            conexion = null;
            estado.setText(motivo);
            btnConectar.setText("Conectar");
            findViewById(R.id.zonaConexion).setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        desconectar();
        super.onDestroy();
    }
}
