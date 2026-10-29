package com.usb.edadperro.ui.perros;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.databinding.ItemPerroBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de perros registrados con su edad humana equivalente.
 */
public class PerrosAdapter extends RecyclerView.Adapter<PerrosAdapter.VH> {

    public interface AlTocar {
        void abrir(Perro p);
    }

    private final List<Perro> datos = new ArrayList<>();
    private final AlTocar alTocar;

    public PerrosAdapter(AlTocar alTocar) {
        this.alTocar = alTocar;
    }

    public void setDatos(List<Perro> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemPerroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Perro p = datos.get(position);
        h.b.tvNombre.setText(p.nombre);
        String raza = p.raza == null || p.raza.isEmpty() ? "Raza no indicada" : p.raza;
        h.b.tvDatos.setText(String.format(Locale.getDefault(), "%s · %s · %s",
                raza, TextosPerro.edad(p.edad), TextosPerro.tamano(p.tamanoEnum())));
        h.b.tvEdadHumana.setText(String.format(Locale.getDefault(), "%.0f años humanos · %s",
                p.edadHumana(), TextosPerro.etapa(p.etapa())));
        h.itemView.setOnClickListener(v -> alTocar.abrir(p));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemPerroBinding b;

        VH(ItemPerroBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
