package com.usb.edadperro;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.alarmas.Recordatorios;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.data.repo.CarritoRepo;
import com.usb.edadperro.data.repo.CuidadoRepo;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.ActivityMainBinding;
import com.usb.edadperro.ui.auth.AuthActivity;
import com.usb.edadperro.util.Hilos;
import com.usb.edadperro.util.Red;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Actividad principal de HuellitasPlus.
 *
 * Aloja todos los módulos como fragmentos y permite navegar entre ellos con
 * la barra inferior (Inicio, Tienda, Asistente IA, Veterinarias y Perfil).
 * La barra superior tiene accesos fijos a urgencias veterinarias y al carrito.
 *
 * Autores: Jeferson Wilderman González Tenjo - José Leandro Ocampo Camacho
 * Asignatura: Optativa II - Desarrollo Móvil
 */
public class MainActivity extends AppCompatActivity {

    /** Argumento para abrir el mapa mostrando solo urgencias. */
    public static final String ARG_SOLO_URGENCIAS = "soloUrgencias";
    /** Extra del intento: abrir la lista de cuidados (al tocar una alarma). */
    public static final String ARG_ABRIR_CUIDADOS = "abrirCuidados";

    private ActivityMainBinding binding;
    private NavController nav;
    private BadgeDrawable badgeCarrito;
    private ListenerRegistration escuchaCarrito;
    private ListenerRegistration escuchaCuidados;
    private ConnectivityManager.NetworkCallback callbackRed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sin sesión no se puede usar la app: se vuelve a la bienvenida.
        if (!SesionRepo.haySesion()) {
            irABienvenida();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        nav = host.getNavController();
        AppBarConfiguration config = new AppBarConfiguration.Builder(new HashSet<>(Arrays.asList(
                R.id.inicioFragment, R.id.tiendaFragment, R.id.asistenteFragment,
                R.id.veterinariasFragment, R.id.perfilFragment))).build();
        NavigationUI.setupActionBarWithNavController(this, nav, config);
        NavigationUI.setupWithNavController(binding.navInferior, nav);
        // Cada pestaña abre siempre su pantalla principal. Por defecto Navigation restaura el
        // historial guardado de la pestaña, y si el carrito o urgencias (que se abren desde la
        // barra superior) quedaron encima, tocar la pestaña parecía no hacer nada.
        binding.navInferior.setOnItemSelectedListener(item -> irAPestana(item.getItemId()));
        binding.navInferior.setOnItemReselectedListener(item -> irAPestana(item.getItemId()));

        // En pantallas de escritura se oculta la barra inferior para dar espacio al teclado.
        nav.addOnDestinationChangedListener((c, destino, args) -> {
            int id = destino.getId();
            boolean ocultar = id == R.id.conversacionFragment || id == R.id.pagoWompiFragment
                    || id == R.id.checkoutFragment || id == R.id.productoFormFragment
                    || id == R.id.perroFormFragment || id == R.id.editarPerfilFragment
                    || id == R.id.cuidadoFormFragment;
            binding.navInferior.setVisibility(ocultar ? View.GONE : View.VISIBLE);
        });

        configurarBadgeCarrito();
        sincronizarAlarmas();
        SesionRepo.cargarPerfil();
        abrirCuidadosSiSePidio(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        abrirCuidadosSiSePidio(intent);
    }

    private void abrirCuidadosSiSePidio(Intent intent) {
        if (nav != null && intent != null && intent.getBooleanExtra(ARG_ABRIR_CUIDADOS, false)) {
            intent.removeExtra(ARG_ABRIR_CUIDADOS);
            nav.navigate(R.id.cuidadosFragment);
        }
    }

    /**
     * Mantiene las alarmas del teléfono iguales a los cuidados guardados en la cuenta
     * (también los creados o borrados desde otro dispositivo).
     */
    private void sincronizarAlarmas() {
        escuchaCuidados = CuidadoRepo.misCuidados().addSnapshotListener((snap, e) -> {
            if (snap == null) {
                return;
            }
            List<Cuidado> cuidados = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                Cuidado c = d.toObject(Cuidado.class);
                if (c != null) {
                    cuidados.add(c);
                }
            }
            Recordatorios.sincronizar(this, cuidados);
        });
    }

    @androidx.annotation.OptIn(markerClass = ExperimentalBadgeUtils.class)
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_superior, menu);
        // El badge se ancla al ícono del carrito cuando la barra ya está dibujada.
        binding.toolbar.post(() ->
                BadgeUtils.attachBadgeDrawable(badgeCarrito, binding.toolbar, R.id.accionCarrito));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return alElegirAccion(item) || super.onOptionsItemSelected(item);
    }

    private boolean alElegirAccion(MenuItem item) {
        if (item.getItemId() == R.id.accionUrgencias) {
            abrirUrgencias();
            return true;
        }
        if (item.getItemId() == R.id.accionCarrito) {
            nav.navigate(R.id.carritoFragment);
            return true;
        }
        return false;
    }

    /** Va a la pantalla principal de una pestaña, dejando el inicio como única pantalla debajo. */
    private boolean irAPestana(int destino) {
        if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == destino) {
            return true;
        }
        NavOptions op = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(nav.getGraph().getStartDestinationId(), false)
                .build();
        nav.navigate(destino, null, op);
        return true;
    }

    /** Acceso visible a veterinarias de urgencias (Puntos 4 y 5). */
    public void abrirUrgencias() {
        Bundle args = new Bundle();
        args.putBoolean(ARG_SOLO_URGENCIAS, true);
        nav.navigate(R.id.veterinariasFragment, args);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return nav.navigateUp() || super.onSupportNavigateUp();
    }

    /** Muestra en el ícono del carrito el número de unidades agregadas. */
    private void configurarBadgeCarrito() {
        badgeCarrito = BadgeDrawable.create(this);
        badgeCarrito.setBackgroundColor(getColor(R.color.naranja));
        badgeCarrito.setVisible(false);
        escuchaCarrito = CarritoRepo.items().addSnapshotListener((snap, e) -> {
            if (snap == null) {
                return;
            }
            int unidades = 0;
            for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                Long c = d.getLong("cantidad");
                unidades += c == null ? 0 : c.intValue();
            }
            badgeCarrito.setNumber(unidades);
            badgeCarrito.setVisible(unidades > 0);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) {
            return;
        }
        // Aviso de conexión (Punto 11.10).
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        mostrarAvisoRed(!Red.hayConexion(this));
        callbackRed = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Hilos.principal(() -> mostrarAvisoRed(false));
            }

            @Override
            public void onLost(@NonNull Network network) {
                // En este instante getActiveNetwork() aún puede devolver la red perdida;
                // si otra red pasa a ser la predeterminada llegará onAvailable.
                Hilos.principal(() -> mostrarAvisoRed(true));
            }
        };
        cm.registerDefaultNetworkCallback(callbackRed);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (callbackRed != null) {
            getSystemService(ConnectivityManager.class).unregisterNetworkCallback(callbackRed);
            callbackRed = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (escuchaCarrito != null) {
            escuchaCarrito.remove();
        }
        if (escuchaCuidados != null) {
            escuchaCuidados.remove();
        }
    }

    private void mostrarAvisoRed(boolean sinConexion) {
        binding.tvSinConexion.setVisibility(sinConexion ? View.VISIBLE : View.GONE);
    }

    /** Cierra la sesión y vuelve a la pantalla de bienvenida. */
    public void cerrarSesion() {
        if (escuchaCarrito != null) {
            escuchaCarrito.remove();
            escuchaCarrito = null;
        }
        if (escuchaCuidados != null) {
            escuchaCuidados.remove();
            escuchaCuidados = null;
        }
        // Las alarmas son de la cuenta: no deben sonar para otro usuario.
        Recordatorios.cancelarTodas(this);
        SesionRepo.cerrarSesion();
        irABienvenida();
    }

    private void irABienvenida() {
        startActivity(new Intent(this, AuthActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    /** uid actual, usado por los fragmentos. */
    public static String uid() {
        return Fb.uid();
    }
}
