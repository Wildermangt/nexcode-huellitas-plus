package com.usb.edadperro.ui.pago;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Pedido;

/**
 * Colores, íconos y textos de cada estado de pago (aprobado, rechazado y pendiente).
 */
final class EstadoUi {

    final int color;
    final int fondo;
    final int icono;
    final int titulo;
    final int detalle;

    private EstadoUi(int color, int fondo, int icono, int titulo, int detalle) {
        this.color = color;
        this.fondo = fondo;
        this.icono = icono;
        this.titulo = titulo;
        this.detalle = detalle;
    }

    static EstadoUi de(String estadoPedido) {
        if (Pedido.PAGADO.equals(estadoPedido)) {
            return new EstadoUi(R.color.verde, R.color.verde_suave, R.drawable.ic_aprobado,
                    R.string.estado_aprobado, R.string.estado_aprobado_detalle);
        }
        if (Pedido.PAGO_RECHAZADO.equals(estadoPedido)) {
            return new EstadoUi(R.color.urgencia, R.color.urgencia_suave, R.drawable.ic_rechazado,
                    R.string.estado_rechazado, R.string.estado_rechazado_detalle);
        }
        if (Pedido.PAGO_EN_PROCESO.equals(estadoPedido)) {
            return new EstadoUi(R.color.naranja, R.color.naranja_suave, R.drawable.ic_pendiente,
                    R.string.estado_pendiente, R.string.estado_pendiente_detalle);
        }
        return new EstadoUi(R.color.azul, R.color.azul_suave, R.drawable.ic_pedidos,
                R.string.estado_sin_pagar, R.string.estado_sin_pagar_detalle);
    }
}
