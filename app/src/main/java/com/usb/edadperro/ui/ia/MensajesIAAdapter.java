package com.usb.edadperro.ui.ia;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.MensajeIA;
import com.usb.edadperro.databinding.ItemBurbujaBinding;
import com.usb.edadperro.ui.comun.Burbujas;

import java.util.ArrayList;
import java.util.List;

/**
 * Burbujas del chat con el asistente de IA.
 */
public class MensajesIAAdapter extends RecyclerView.Adapter<MensajesIAAdapter.VH> {

    private final List<MensajeIA> datos = new ArrayList<>();

    public void setDatos(List<MensajeIA> nuevos) {
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
        MensajeIA m = datos.get(position);
        boolean mio = m.tipo == MensajeIA.USUARIO;
        Burbujas.configurar(h.b, mio, true);
        if (m.tipo == MensajeIA.ESCRIBIENDO) {
            h.b.tvTexto.setText(R.string.escribiendo);
            h.b.tvTexto.setAlpha(0.6f);
        } else {
            h.b.tvTexto.setAlpha(1f);
            h.b.tvTexto.setText(mio ? m.texto : Burbujas.markdown(m.texto));
        }
        if (m.urgencia && !mio) {
            h.b.burbuja.setBackgroundResource(R.drawable.fondo_chip_urgencia);
        }
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
