package com.usb.edadperro.ui.tienda;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.databinding.ItemProductoBinding;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Grilla de productos del catálogo.
 */
public class ProductosAdapter extends RecyclerView.Adapter<ProductosAdapter.VH> {

    public interface AlTocar {
        void abrir(Producto p);
    }

    /** Por debajo de este número se avisa que quedan pocas unidades. */
    static final int POCAS_UNIDADES = 5;

    private final List<Producto> datos = new ArrayList<>();
    private final AlTocar alTocar;

    public ProductosAdapter(AlTocar alTocar) {
        this.alTocar = alTocar;
    }

    public void setDatos(List<Producto> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemProductoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Producto p = datos.get(position);
        h.b.tvMarca.setText(p.marca);
        h.b.tvNombre.setText(p.nombre);
        h.b.tvPresentacion.setText(p.presentacion);
        h.b.tvPrecio.setText(Formato.pesos(p.precio));
        h.b.tvCategoria.setText(p.categoriaNombre);
        mostrarStock(h.b.tvStock, p.stock);
        Ui.imagenProducto(h.b.ivProducto, p.imagenUrl);
        h.itemView.setOnClickListener(v -> alTocar.abrir(p));
    }

    /** Texto y color de la disponibilidad de inventario. */
    static void mostrarStock(android.widget.TextView tv, long stock) {
        int color;
        if (stock <= 0) {
            tv.setText(R.string.agotado);
            color = R.color.urgencia;
        } else if (stock <= POCAS_UNIDADES) {
            tv.setText(tv.getContext().getString(R.string.pocas_unidades, stock));
            color = R.color.naranja;
        } else {
            tv.setText(tv.getContext().getString(R.string.disponibles, stock));
            color = R.color.verde;
        }
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), color));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemProductoBinding b;

        VH(ItemProductoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
