package com.usb.edadperro.ui.comun;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.databinding.ItemFilaBinding;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador reutilizable para las listas sencillas de la app
 * (pedidos, conversaciones, usuarios y productos del administrador).
 */
public class FilaAdapter extends RecyclerView.Adapter<FilaAdapter.VH> {

    /** Datos de una fila. Los campos opcionales pueden quedar en null/0. */
    public static class Fila {
        public String titulo;
        public String subtitulo;
        public String derecha;
        public String etiqueta;
        public int colorEtiqueta;
        public int fondoEtiqueta;
        /** Ícono (drawable) o, si es 0, se muestra la inicial del título. */
        public int icono;
        /** URL de imagen (productos); tiene prioridad sobre el ícono. */
        public String imagenUrl;
        public Runnable alTocar;
        public Runnable alMantener;
    }

    private final List<Fila> datos = new ArrayList<>();

    public void setDatos(List<Fila> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemFilaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Fila f = datos.get(position);
        h.b.tvTitulo.setText(f.titulo);
        h.b.tvSubtitulo.setText(f.subtitulo);
        h.b.tvSubtitulo.setVisibility(f.subtitulo == null ? View.GONE : View.VISIBLE);
        h.b.tvDerecha.setText(f.derecha);
        h.b.tvDerecha.setVisibility(f.derecha == null ? View.GONE : View.VISIBLE);

        if (f.etiqueta != null) {
            h.b.tvEtiqueta.setVisibility(View.VISIBLE);
            h.b.tvEtiqueta.setText(f.etiqueta);
            h.b.tvEtiqueta.setTextColor(ContextCompat.getColor(h.itemView.getContext(), f.colorEtiqueta));
            h.b.tvEtiqueta.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(h.itemView.getContext(), f.fondoEtiqueta)));
        } else {
            h.b.tvEtiqueta.setVisibility(View.GONE);
        }

        if (f.imagenUrl != null) {
            h.b.ivIcono.setVisibility(View.VISIBLE);
            h.b.tvInicial.setVisibility(View.GONE);
            // app:tint es de AppCompat: se quita con ImageViewCompat para ver la foto en color.
            androidx.core.widget.ImageViewCompat.setImageTintList(h.b.ivIcono, null);
            h.b.ivIcono.setPadding(2, 2, 2, 2);
            Ui.imagenProducto(h.b.ivIcono, f.imagenUrl);
        } else if (f.icono != 0) {
            h.b.ivIcono.setVisibility(View.VISIBLE);
            h.b.tvInicial.setVisibility(View.GONE);
            androidx.core.widget.ImageViewCompat.setImageTintList(h.b.ivIcono, ColorStateList.valueOf(
                    ContextCompat.getColor(h.itemView.getContext(), com.usb.edadperro.R.color.verde)));
            h.b.ivIcono.setImageResource(f.icono);
        } else {
            h.b.ivIcono.setVisibility(View.GONE);
            h.b.tvInicial.setVisibility(View.VISIBLE);
            String t = f.titulo == null || f.titulo.isEmpty() ? "?" : f.titulo.substring(0, 1).toUpperCase();
            h.b.tvInicial.setText(t);
        }

        h.itemView.setOnClickListener(f.alTocar == null ? null : v -> f.alTocar.run());
        h.itemView.setOnLongClickListener(f.alMantener == null ? null : v -> {
            f.alMantener.run();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFilaBinding b;

        VH(ItemFilaBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
