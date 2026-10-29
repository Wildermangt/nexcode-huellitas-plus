package com.usb.edadperro.ui.pago;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.remoto.PagosServicio;
import com.usb.edadperro.databinding.FragmentPagoWompiBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Hilos;

/**
 * Pasarela de pago Wompi (Punto 8).
 *
 * 1. Se pide al backend (Vercel) la firma de integridad del pedido.
 * 2. Se abre el Web Checkout de Wompi en un WebView.
 * 3. Cuando Wompi redirige a la URL de retorno, se toma el id de la
 *    transacción y se vuelve al detalle del pedido para verificar el estado.
 *
 * Los datos de la tarjeta se escriben directamente en la página de Wompi:
 * la app nunca los ve ni los guarda.
 */
public class PagoWompiFragment extends Fragment {

    public static final String ARG_PEDIDO = "pedidoId";

    private FragmentPagoWompiBinding b;
    private String pedidoId;
    private String urlRetorno;
    private boolean terminado = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentPagoWompiBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        pedidoId = requireArguments().getString(ARG_PEDIDO);

        WebSettings s = b.web.getSettings();
        s.setJavaScriptEnabled(true);      // el checkout de Wompi lo requiere
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);       // sin acceso a archivos del teléfono
        s.setAllowContentAccess(false);
        b.web.setWebViewClient(new ClienteWompi());

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmarSalida();
                    }
                });

        iniciar();
    }

    private void iniciar() {
        FirebaseUser u = Fb.auth().getCurrentUser();
        if (u == null) {
            return;
        }
        // El token de Firebase demuestra al backend quién es el usuario.
        u.getIdToken(false).addOnSuccessListener(token -> Hilos.fondo(() -> {
            try {
                PagosServicio.Checkout c = PagosServicio.iniciarPago(pedidoId, token.getToken());
                Hilos.principal(() -> abrirCheckout(c));
            } catch (Exception e) {
                Hilos.principal(() -> error(Errores.mensaje(e)));
            }
        })).addOnFailureListener(e -> error(Errores.mensaje(e)));
    }

    private void abrirCheckout(PagosServicio.Checkout c) {
        if (b == null) {
            return;
        }
        urlRetorno = c.urlRetorno;
        b.web.loadUrl(c.urlCheckout);
    }

    private void error(String mensaje) {
        if (b == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.no_se_pudo_iniciar_pago)
                .setMessage(mensaje)
                .setCancelable(false)
                .setPositiveButton(R.string.entendido, (d, w) ->
                        NavHostFragment.findNavController(this).popBackStack())
                .show();
    }

    /**
     * Vuelve al pedido y verifica el pago: con el id que Wompi pasó al retorno o,
     * si el usuario salió antes (transaccionId null), buscándolo por la referencia.
     */
    private void finalizar(@Nullable String transaccionId) {
        if (terminado) {
            return;
        }
        terminado = true;
        Bundle args = new Bundle();
        args.putString(PedidoDetalleFragment.ARG_ID, pedidoId);
        args.putString(PedidoDetalleFragment.ARG_TRANSACCION, transaccionId);
        args.putBoolean(PedidoDetalleFragment.ARG_VERIFICAR, true);
        NavOptions op = new NavOptions.Builder().setPopUpTo(R.id.pedidoDetalleFragment, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.pedidoDetalleFragment, args, op);
    }

    private void confirmarSalida() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.salir_del_pago)
                .setMessage(R.string.salir_del_pago_detalle)
                .setNegativeButton(R.string.seguir_pagando, null)
                .setPositiveButton(R.string.salir, (d, w) -> finalizar(null))
                .show();
    }

    private class ClienteWompi extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String url = uri.toString();
            if (urlRetorno != null && url.startsWith(urlRetorno)) {
                // Wompi agrega ?id=<transacción>&env=test a la URL de retorno.
                finalizar(uri.getQueryParameter("id"));
                return true;
            }
            String esquema = uri.getScheme();
            if (esquema != null && !esquema.startsWith("http")) {
                // Métodos que abren otra app (p. ej. Nequi o el banco en PSE).
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignorada) {
                    // No hay app para ese enlace; el usuario sigue en el checkout.
                }
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (b != null) {
                b.cargando.setVisibility(View.GONE);
                b.web.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (b != null) {
            b.web.destroy();
        }
        b = null;
    }
}
