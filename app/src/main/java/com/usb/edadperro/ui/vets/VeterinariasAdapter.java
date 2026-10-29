package com.usb.edadperro.ui.vets;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Veterinaria;
import com.usb.edadperro.databinding.ItemVeterinariaBinding;
import com.usb.edadperro.util.Distancia;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de veterinarias con acciones de llamar, cómo llegar y ver la fuente.
 */
public class VeterinariasAdapter extends RecyclerView.Adapter<VeterinariasAdapter.VH> {

    public interface Acciones {
        void enfocar(Veterinaria v);

        void consultarDireccion(Veterinaria v, int posicion);
    }

    private final List<Veterinaria> datos = new ArrayList<>();
    private final Acciones acciones;

    public VeterinariasAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setDatos(List<Veterinaria> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemVeterinariaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Veterinaria v = datos.get(position);
        h.b.tvNombre.setText(v.nombre);
        h.b.tvDistancia.setText(Distancia.formatear(v.distanciaKm));
        h.b.tvUrgencias.setVisibility(v.urgencias ? View.VISIBLE : View.GONE);

        if (v.direccion != null) {
            h.b.tvDireccion.setText(v.direccion);
            h.b.tvDireccion.setOnClickListener(null);
        } else {
            // OSM no siempre tiene la dirección escrita: se consulta con Nominatim al tocar.
            h.b.tvDireccion.setText(R.string.consultar_direccion);
            h.b.tvDireccion.setOnClickListener(x -> acciones.consultarDireccion(v, h.getBindingAdapterPosition()));
        }

        if (v.telefono != null) {
            h.b.tvTelefono.setText(v.telefono);
            h.b.btnLlamar.setText(R.string.llamar);
            h.b.btnLlamar.setOnClickListener(x -> Ui.marcar(x.getContext(), primerTelefono(v.telefono)));
        } else {
            h.b.tvTelefono.setText(R.string.telefono_no_registrado);
            h.b.btnLlamar.setText(R.string.buscar_telefono);
            h.b.btnLlamar.setOnClickListener(x -> Ui.abrirUrl(x.getContext(),
                    "https://www.google.com/search?q=" + Uri.encode(v.nombre + " veterinaria teléfono")));
        }

        h.b.tvHorario.setVisibility(v.horario == null ? View.GONE : View.VISIBLE);
        h.b.tvHorario.setText(v.horario);

        h.b.btnComoLlegar.setOnClickListener(x -> Ui.comoLlegar(x.getContext(), v.lat, v.lon, v.nombre));
        h.b.btnVerOsm.setOnClickListener(x -> Ui.abrirUrl(x.getContext(), v.urlOsm()));
        h.itemView.setOnClickListener(x -> acciones.enfocar(v));
    }

    /** OSM puede traer varios teléfonos separados por ";". */
    private static String primerTelefono(String telefonos) {
        return telefonos.split(";")[0].trim();
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemVeterinariaBinding b;

        VH(ItemVeterinariaBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
