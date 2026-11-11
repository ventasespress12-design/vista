package com.edgar.inicio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Al prender el stick, arranca la lectura de botones. */
public class Arranque extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        BotonesService.iniciar(c);
    }
}
