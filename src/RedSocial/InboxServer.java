/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package RedSocial;

import java.net.*;
import java.io.*;

/**
 *
 * @author Fabio Sierra
 */

public class InboxServer {

    private static final int PUERTO = 9090;

    private boolean esSvidor = false;
    private ServerSocket serverSocket = null;
    private Socket socketConexion = null;
    private PrintWriter salida = null;

    private MensajeListener listener = null;
    private LeidoListener leidoListener = null;

    public interface MensajeListener {
        void onMensajeRecibido(String emisor, String receptor, String contenido, String tipo);
    }

    public interface LeidoListener {
        void onMensajesLeidos(String lector, String dueno);
    }

    public void setListener(MensajeListener listener) {
        this.listener = listener;
    }

    public void setLeidoListener(LeidoListener l) {
        this.leidoListener = l;
    }

    /**
     * Intenta arrancar como servidor.
     * Si el puerto ya está ocupado (otra instancia corriendo), arranca como cliente.
     */
    public void iniciar() {
        try {
            // Intentamos abrir el ServerSocket
            serverSocket = new ServerSocket(PUERTO);
            esSvidor = true;
            System.out.println("[Socket] Esta instancia es el SERVIDOR en puerto " + PUERTO);
            iniciarHiloServidor();
        } catch (IOException e) {
            // Puerto ocupado → ya hay un servidor, somos el cliente
            esSvidor = false;
            System.out.println("[Socket] Esta instancia es el CLIENTE, conectando...");
            iniciarHiloCliente();
        }
    }

    /**
     * Espera en un hilo aparte a que el cliente se conecte.
     * Una vez conectado, escucha mensajes entrantes.
     */
    private void iniciarHiloServidor() {
        Thread hiloServidor = new Thread(() -> {
            try {
                System.out.println("[Servidor] Esperando conexión del cliente...");
                socketConexion = serverSocket.accept(); // bloquea hasta que el cliente conecta
                System.out.println("[Servidor] Cliente conectado!");
                salida = new PrintWriter(socketConexion.getOutputStream(), true);
                escucharMensajes(socketConexion); // empieza a escuchar
            } catch (IOException e) {
                System.out.println("[Servidor] Error: " + e.getMessage());
            }
        });
        hiloServidor.setDaemon(true); // muere cuando la app cierra
        hiloServidor.start();
    }

    /**
     * Intenta conectarse al servidor con reintentos cada 2 segundos.
     * Una vez conectado, escucha mensajes entrantes.
     */
    private void iniciarHiloCliente() {
        Thread hiloCliente = new Thread(() -> {
            int intentos = 0;
            while (socketConexion == null && intentos < 10) {
                try {
                    socketConexion = new Socket("localhost", PUERTO);
                    salida = new PrintWriter(socketConexion.getOutputStream(), true);
                    System.out.println("[Cliente] Conectado al servidor!");
                    escucharMensajes(socketConexion);
                } catch (IOException e) {
                    intentos++;
                    System.out.println("[Cliente] Reintento " + intentos + "...");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
        });
        hiloCliente.setDaemon(true);
        hiloCliente.start();
    }

    private void escucharMensajes(Socket socket) {
        Thread hiloEscucha = new Thread(() -> {
            try {
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    System.out.println("[Socket] Recibido: " + linea);
                    parsearYNotificar(linea);
                }
            } catch (IOException e) {
                System.out.println("[Socket] Conexión cerrada: " + e.getMessage());
            }
        });
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    private void parsearYNotificar(String linea) {
        if (linea.startsWith("LEIDO|")) {
            String[] partes = linea.split("\\|", 3);
            if (partes.length < 3) return;
            String lector = partes[1];
            String dueno  = partes[2];
            if (leidoListener != null)
                javax.swing.SwingUtilities.invokeLater(() ->
                    leidoListener.onMensajesLeidos(lector, dueno));
            return;
        }
        if (!linea.startsWith("MENSAJE|")) return;
        String[] partes = linea.split("\\|", 5);
        if (partes.length < 5) return;
        String emisor    = partes[1];
        String receptor  = partes[2];
        String contenido = partes[3];
        String tipo      = partes[4];
        if (listener != null)
            javax.swing.SwingUtilities.invokeLater(() ->
                listener.onMensajeRecibido(emisor, receptor, contenido, tipo));
    }

    /**
     * Envía la notificación de mensaje nuevo a la otra instancia por Socket.
     * Se llama después de guardar el mensaje en el archivo .ins.
     */
    public void enviarNotificacion(String emisor, String receptor,
                                    String contenido, String tipo) {
        if (salida == null) return;
        salida.println("MENSAJE|" + emisor + "|" + receptor + "|" + contenido + "|" + tipo);
    }

    public void enviarLeido(String lector, String dueno) {
        if (salida == null) return;
        salida.println("LEIDO|" + lector + "|" + dueno);
    }

    public void cerrar() {
        try {
            if (socketConexion != null) socketConexion.close();
            if (serverSocket   != null) serverSocket.close();
        } catch (IOException e) {
            System.out.println("[Socket] Error al cerrar: " + e.getMessage());
        }
    }

    public boolean estaConectado() {
        return socketConexion != null && socketConexion.isConnected();
    }

    public boolean esServidor() {
        return esSvidor;
    }
}
