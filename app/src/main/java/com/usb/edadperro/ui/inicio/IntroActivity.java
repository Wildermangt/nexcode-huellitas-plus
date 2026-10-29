package com.usb.edadperro.ui.inicio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.ActivityIntroBinding;
import com.usb.edadperro.ui.auth.AuthActivity;

/**
 * Pantalla de bienvenida animada (Punto 2).
 *
 * Primero se muestra el splash del sistema con el logotipo y luego se
 * reproduce el video de la intro. La primera vez se ve completo; en las
 * siguientes aperturas se puede omitir de inmediato.
 */
public class IntroActivity extends AppCompatActivity {

    private static final String PREFS = "intro";
    private static final String YA_VISTA = "ya_vista";

    private ActivityIntroBinding binding;
    private boolean navegando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash del sistema (Android 12+ y compatibilidad hacia atrás).
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean yaVista = prefs.getBoolean(YA_VISTA, false);
        prefs.edit().putBoolean(YA_VISTA, true).apply();

        binding.btnOmitir.setOnClickListener(v -> continuar());
        if (yaVista) {
            binding.btnOmitir.setVisibility(View.VISIBLE);
        } else {
            // La primera vez el botón aparece a mitad del video.
            binding.btnOmitir.postDelayed(() -> binding.btnOmitir.setVisibility(View.VISIBLE), 4000);
        }

        Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro);
        binding.video.setVideoURI(video);
        binding.video.setOnCompletionListener(mp -> continuar());
        binding.video.setOnErrorListener((mp, what, extra) -> {
            // Si el dispositivo no puede reproducir el video, se continúa sin él.
            continuar();
            return true;
        });
        binding.video.start();
    }

    /** Decide a dónde ir después de la intro. */
    private void continuar() {
        if (navegando || isFinishing()) {
            return;
        }
        navegando = true;
        binding.video.stopPlayback();

        if (!Fb.configurado(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.falta_firebase_titulo)
                    .setMessage(R.string.falta_firebase_mensaje)
                    .setCancelable(false)
                    .setPositiveButton(R.string.entendido, (d, w) -> finish())
                    .show();
            return;
        }
        Class<?> destino = SesionRepo.haySesion() ? MainActivity.class : AuthActivity.class;
        startActivity(new Intent(this, destino));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
