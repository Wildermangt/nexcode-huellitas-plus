package com.usb.edadperro.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Conversacion;
import com.usb.edadperro.data.modelo.Mensaje;
import com.usb.edadperro.data.repo.ChatRepo;
import com.usb.edadperro.databinding.FragmentConversacionBinding;
import com.usb.edadperro.databinding.ItemBurbujaBinding;
import com.usb.edadperro.ui.comun.Burbujas;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Conversación en tiempo real con otro usuario: enviar y recibir mensajes,
 * con fecha, hora y remitente de cada uno.
 */
public class ConversacionFragment extends Fragment {

    public static final String ARG_UID = "otroUid";
    public static final String ARG_NOMBRE = "otroNombre";

    private static final int MENU_BLOQUEAR = 1;
    private static final int MENU_REPORTAR = 2;

    private FragmentConversacionBinding b;
    private ListenerRegistration escucha;
    private ListenerRegistration escuchaBloqueo;
    private boolean bloqueado = false;
    /** false cuando las reglas rechazaron la escucha (la conversación aún no existía). */
    private boolean escuchaActiva = false;
    private String otroUid;
    private String otroNombre;
    private final MensajesAdapter adapter = new MensajesAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentConversacionBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        otroUid = requireArguments().getString(ARG_UID);
        otroNombre = requireArguments().getString(ARG_NOMBRE, "Usuario");
        if (getActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(otroNombre);
        }

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        b.rvMensajes.setLayoutManager(lm);
        b.rvMensajes.setAdapter(adapter);
        b.btnEnviar.setOnClickListener(v -> enviar());
        b.btnDesbloquear.setOnClickListener(v -> ChatRepo.desbloquear(otroUid));
        escuchar();

        // Estado de bloqueo en tiempo real: oculta la barra de escritura si bloqueé al usuario.
        escuchaBloqueo = ChatRepo.bloqueo(otroUid).addSnapshotListener((d, e) -> {
            if (b == null || d == null) {
                return;
            }
            bloqueado = d.exists();
            b.barraBloqueo.setVisibility(bloqueado ? View.VISIBLE : View.GONE);
            b.barraEnvio.setVisibility(bloqueado ? View.GONE : View.VISIBLE);
            requireActivity().invalidateOptionsMenu();
        });

        // Menú de privacidad: bloquear / desbloquear y reportar.
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                menu.add(Menu.NONE, MENU_BLOQUEAR, Menu.NONE,
                        bloqueado ? R.string.desbloquear_usuario : R.string.bloquear_usuario);
                menu.add(Menu.NONE, MENU_REPORTAR, Menu.NONE, R.string.reportar_usuario);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == MENU_BLOQUEAR) {
                    if (bloqueado) {
                        ChatRepo.desbloquear(otroUid);
                    } else {
                        confirmarBloqueo();
                    }
                    return true;
                }
                if (item.getItemId() == MENU_REPORTAR) {
                    reportar();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void confirmarBloqueo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.bloquear_pregunta, otroNombre))
                .setMessage(R.string.bloquear_detalle)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.bloquear, (d, w) -> ChatRepo.bloquear(otroUid, otroNombre))
                .show();
    }

    /** Reporte con motivo para que el administrador lo revise. */
    private void reportar() {
        String[] motivos = getResources().getStringArray(R.array.motivos_reporte);
        final int[] elegido = {0};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.reportar_a, otroNombre))
                .setSingleChoiceItems(motivos, 0, (d, i) -> elegido[0] = i)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.reportar, (d, w) -> ChatRepo
                        .reportar(otroUid, otroNombre, motivos[elegido[0]])
                        .addOnSuccessListener(v -> {
                            if (b == null) {
                                return;
                            }
                            Snackbar.make(b.getRoot(), R.string.reporte_enviado, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.bloquear, x -> ChatRepo.bloquear(otroUid, otroNombre))
                                    .show();
                        })
                        .addOnFailureListener(e -> {
                            if (b != null) {
                                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    /** Escucha en tiempo real los mensajes de la conversación. */
    private void escuchar() {
        if (escucha != null) {
            escucha.remove();
        }
        String convId = Conversacion.idPara(Fb.uid(), otroUid);
        escuchaActiva = true;
        // MetadataChanges.INCLUDE: muestra al instante los mensajes propios aún no confirmados.
        escucha = ChatRepo.mensajes(convId).addSnapshotListener(MetadataChanges.INCLUDE, (snap, e) -> {
            if (b == null) {
                return;
            }
            if (e != null) {
                // Si la conversación aún no existe, las reglas no permiten leerla: es normal.
                // Se descarta lo que hubiera en caché, porque ya no se puede confirmar.
                escuchaActiva = false;
                adapter.setDatos(new ArrayList<>());
                b.tvVacio.setVisibility(View.VISIBLE);
                return;
            }
            List<Mensaje> lista = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Mensaje m = d.toObject(Mensaje.class);
                if (m != null) {
                    lista.add(m);
                }
            }
            adapter.setDatos(lista);
            b.tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            b.rvMensajes.scrollToPosition(Math.max(0, lista.size() - 1));
        });
    }

    private void enviar() {
        String texto = Ui.texto(b.etMensaje);
        if (texto.isEmpty()) {
            return;
        }
        if (texto.length() > ChatRepo.MAX_MENSAJE) {
            texto = texto.substring(0, ChatRepo.MAX_MENSAJE);
        }
        b.etMensaje.setText("");
        ChatRepo.enviar(otroUid, otroNombre, texto)
                .addOnSuccessListener(v -> {
                    // Si la escucha falló porque la conversación no existía, ahora ya existe: se reinicia.
                    if (b != null && !escuchaActiva) {
                        escuchar();
                    }
                })
                .addOnFailureListener(e -> {
                    if (b == null) {
                        return;
                    }
                    // Las reglas rechazan el mensaje si el otro usuario me bloqueó.
                    boolean bloqueoAjeno = e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.PERMISSION_DENIED;
                    Snackbar.make(b.getRoot(), bloqueoAjeno ? getString(R.string.no_puedes_escribir)
                            : Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                });
    }

    /** Burbujas de la conversación, con remitente, fecha y hora. */
    private static class MensajesAdapter extends RecyclerView.Adapter<MensajesAdapter.VH> {
        private final List<Mensaje> datos = new ArrayList<>();
        private final String yo = Fb.uid();

        void setDatos(List<Mensaje> nuevos) {
            datos.clear();
            datos.addAll(nuevos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemBurbujaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Mensaje m = datos.get(position);
            boolean mio = yo.equals(m.remitenteId);
            Burbujas.configurar(h.b, mio, false);
            h.b.tvTexto.setText(m.texto);
            h.b.tvRemitente.setVisibility(View.VISIBLE);
            h.b.tvRemitente.setText(mio ? h.itemView.getContext().getString(R.string.tu) : m.remitenteNombre);
            h.b.tvHora.setVisibility(View.VISIBLE);
            // Mientras el servidor no confirma, la fecha es nula: se muestra "Enviando…".
            h.b.tvHora.setText(m.fecha == null
                    ? h.itemView.getContext().getString(R.string.enviando)
                    : fechaHora(m.fecha));
        }

        private static String fechaHora(Date d) {
            return Formato.fechaHora(d);
        }

        @Override
        public int getItemCount() {
            return datos.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemBurbujaBinding b;

            VH(ItemBurbujaBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (escucha != null) {
            escucha.remove();
        }
        if (escuchaBloqueo != null) {
            escuchaBloqueo.remove();
        }
        b = null;
    }
}
