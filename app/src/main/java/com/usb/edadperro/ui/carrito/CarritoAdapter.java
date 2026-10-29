package com.usb.edadperro.ui.carrito;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.data.modelo.ItemCarrito;
import com.usb.edadperro.databinding.ItemCarritoBinding;
import com.usb.edadperro.util.CarritoCalculadora;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Líneas del carrito con botones para aumentar, disminuir y eliminar.
 * También se usa en modo solo lectura en el checkout.
 */
public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.VH> {

    public interface Acciones {
        void cambiarCantidad(ItemCarrito item, int nueva);

        void eliminar(ItemCarrito item);
    }

    private final List<ItemCarrito> datos = new ArrayList<>();
    private final Acciones acciones;

    /** @param acciones null para mostrar el carrito sin botones (solo lectura) */
    public CarritoAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setDatos(List<ItemCarrito> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemCarritoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ItemCarrito it = datos.get(position);
        h.b.tvNombre.setText(Formato.producto(it.marca, it.nombre));
        h.b.tvUnitario.setText(it.presentacion + " · " + Formato.pesos(it.precioUnitario) + " c/u");
        h.b.tvCantidad.setText(String.valueOf(it.cantidad));
        h.b.tvSubtotal.setText(Formato.pesos(it.subtotal()));
        Ui.imagenProducto(h.b.ivProducto, it.imagenUrl);

        boolean editable = acciones != null;
        h.b.btnMenos.setVisibility(editable ? View.VISIBLE : View.GONE);
        h.b.btnMas.setVisibility(editable ? View.VISIBLE : View.GONE);
        h.b.btnEliminar.setVisibility(editable ? View.VISIBLE : View.GONE);
        if (!editable) {
            h.b.tvCantidad.setText("x" + it.cantidad);
            return;
        }
        h.b.btnMenos.setOnClickListener(v -> acciones.cambiarCantidad(it, it.cantidad - 1));
        h.b.btnMas.setEnabled(it.cantidad < CarritoCalculadora.MAX_POR_PRODUCTO);
        h.b.btnMas.setOnClickListener(v -> acciones.cambiarCantidad(it, it.cantidad + 1));
        h.b.btnEliminar.setOnClickListener(v -> acciones.eliminar(it));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCarritoBinding b;

        VH(ItemCarritoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
