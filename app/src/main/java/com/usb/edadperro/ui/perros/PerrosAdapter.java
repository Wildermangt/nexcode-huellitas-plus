package com.usb.edadperro.ui.perros;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.databinding.ItemPerroBinding;
import com.usb.edadperro.util.FotoMascota;

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
        pintarFoto(h, p);
        h.itemView.setOnClickListener(v -> alTocar.abrir(p));
    }

    /** Muestra la foto 1:1 del perro, o el ícono de huella si no tiene. */
    private void pintarFoto(VH h, Perro p) {
        Bitmap foto = FotoMascota.aBitmap(p.fotoBase64);
        if (foto != null) {
            h.b.ivFoto.setPadding(0, 0, 0, 0);
            ImageViewCompat.setImageTintList(h.b.ivFoto, null);
            h.b.ivFoto.setImageBitmap(foto);
        } else {
            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                    h.itemView.getResources().getDisplayMetrics());
            h.b.ivFoto.setPadding(pad, pad, pad, pad);
            ImageViewCompat.setImageTintList(h.b.ivFoto,
                    ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), R.color.naranja)));
            h.b.ivFoto.setImageResource(R.drawable.ic_huella);
        }
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
