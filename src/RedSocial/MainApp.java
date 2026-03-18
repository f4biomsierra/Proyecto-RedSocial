/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RedSocial;

import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MainApp extends JFrame {
    private final UserManager userManager = new UserManager();
    private final PublicacionManager pubManager = new PublicacionManager();
    private final FollowManager followManager = new FollowManager();
    private final InboxManager inboxManager = new InboxManager();
    private final StickerManager stickerManager = new StickerManager();
    private final ComentarioManager comentarioManager = new ComentarioManager();

    private final InboxServer mensajeServer = new InboxServer();

    private Runnable recargarConversacionActual = null;
    private String conversacionAbieraCon = null;

    private User usuarioActual = null;
    private JPanel headerHome = null;
    private String vistaAnterior = "FEED";
    private final java.util.Set<String> likesEnSesion = new java.util.HashSet<>();

    private static final Color BLANCO = Color.WHITE;
    private static final Color NEGRO = new Color(30, 30, 30);
    private static final Color GRIS_CLARO = new Color(250, 250, 250);
    private static final Color GRIS_BORDE = new Color(219, 219, 219);
    private static final Color AZUL_IG = new Color(0, 149, 246);
    private static final Color ROSA_IG = new Color(225, 48, 108);
    private static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_NORM = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font FONT_LOGO = new Font("Serif", Font.ITALIC, 26);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    public MainApp() {
        setTitle("Instagram");
        setSize(390, 844);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(construirPantallaLogin(),      "LOGIN");
        mainPanel.add(construirPantallaRegistro(),  "REGISTRO");
        mainPanel.add(construirPantallaHome(),      "HOME");
        mainPanel.add(construirPantallaReactivar(), "REACTIVAR");

        cardLayout.show(mainPanel, "LOGIN");

        mensajeServer.iniciar();

        mensajeServer.setListener((emisor, receptor, contenido, tipo) -> {
            if (usuarioActual == null) return;
            // Si el receptor soy yo y tengo el chat abierto con el emisor → recargar mensajes
            if (receptor.equalsIgnoreCase(usuarioActual.getUsername())) {
                if (emisor.equalsIgnoreCase(conversacionAbieraCon)
                        && recargarConversacionActual != null) {
                    SwingUtilities.invokeLater(() -> recargarConversacionActual.run());
                    // Marcar como leídos y notificar al emisor
                    try {
                        inboxManager.marcarLeidos(usuarioActual.getUsername(), emisor);
                        mensajeServer.enviarLeido(usuarioActual.getUsername(), emisor);
                    } catch (IOException ex) {}
                }
                // Reconstruir inbox para actualizar badges
                SwingUtilities.invokeLater(() -> {
                    homeCenter.remove(getComponentByName("INBOX"));
                    homeCenter.add(construirInbox(), "INBOX");
                });
            }
        });

        mensajeServer.setLeidoListener((lector, dueno) -> {
            if (usuarioActual == null) return;
            // El otro leyó mis mensajes → recargar para mostrar ✓✓ Visto
            if (dueno.equalsIgnoreCase(usuarioActual.getUsername())
                    && lector.equalsIgnoreCase(conversacionAbieraCon)
                    && recargarConversacionActual != null) {
                SwingUtilities.invokeLater(() -> recargarConversacionActual.run());
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                liberarSesion(usuarioActual != null ? usuarioActual.getUsername() : null);
                mensajeServer.cerrar();
            }
        });

        setVisible(true);
    }

    private JPanel construirPantallaLogin() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);

        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setBackground(BLANCO);
        caja.setBorder(new EmptyBorder(40, 40, 40, 40));
        caja.setPreferredSize(new Dimension(310, 500));

        JLabel logo = new JLabel("Instagram");
        logo.setFont(FONT_LOGO);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField txtUser = styledTextField("Nombre de usuario");
        JPasswordField txtPass = new JPasswordField();
        styleTextField(txtPass, "Contraseña");

        JButton btnLogin = igButton("Iniciar sesión", AZUL_IG, BLANCO);
        btnLogin.addActionListener(e -> {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            try {
                User u = userManager.buscarporUsername(user);
                if (u == null) {
                    showError("Usuario no encontrado.");
                } else if (!u.isEstadoCuenta()) {
                    showError("Cuenta desactivada.");
                } else if (!u.getPassword().equals(pass)) {
                    showError("Contraseña incorrecta.");
                } else {
                    if (!adquirirSesion(u.getUsername())) {
                        showError("Este usuario ya tiene sesión activa en otra ventana.");
                        return;
                    }
                    usuarioActual = u;
                    txtUser.setText("");
                    txtPass.setText("");
                    mostrarHome();
                }
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(310, 1));

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkPanel.setBackground(BLANCO);
        JLabel lblTxt = new JLabel("¿No tienes cuenta?");
        lblTxt.setFont(FONT_NORM);
        JButton btnIrReg = new JButton("Regístrate");
        btnIrReg.setFont(FONT_BOLD);
        btnIrReg.setForeground(AZUL_IG);
        btnIrReg.setBorderPainted(false);
        btnIrReg.setContentAreaFilled(false);
        btnIrReg.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIrReg.addActionListener(e -> cardLayout.show(mainPanel, "REGISTRO"));
        linkPanel.add(lblTxt);
        linkPanel.add(btnIrReg);

        JButton btnReactivar = new JButton("Reactivar cuenta desactivada");
        btnReactivar.setFont(FONT_SMALL);
        btnReactivar.setForeground(GRIS_BORDE);
        btnReactivar.setBorderPainted(false);
        btnReactivar.setContentAreaFilled(false);
        btnReactivar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReactivar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReactivar.addActionListener(e -> cardLayout.show(mainPanel, "REACTIVAR"));

        caja.add(logo);
        caja.add(Box.createVerticalStrut(30));
        caja.add(txtUser);
        caja.add(Box.createVerticalStrut(10));
        caja.add(txtPass);
        caja.add(Box.createVerticalStrut(15));
        caja.add(btnLogin);
        caja.add(Box.createVerticalStrut(20));
        caja.add(sep);
        caja.add(Box.createVerticalStrut(20));
        caja.add(linkPanel);
        caja.add(Box.createVerticalStrut(10));
        caja.add(btnReactivar);

        panel.add(caja);
        return panel;
    }

    private JPanel construirPantallaRegistro() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);

        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setBackground(BLANCO);
        caja.setBorder(new EmptyBorder(20, 30, 20, 30));
        caja.setPreferredSize(new Dimension(330, 700));

        JLabel logo = new JLabel("Instagram");
        logo.setFont(FONT_LOGO);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Crea tu cuenta");
        sub.setFont(FONT_NORM);
        sub.setForeground(Color.GRAY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField txtNombre   = styledTextField("Nombre completo");
        JTextField txtUsername = styledTextField("Usuario");
        JPasswordField txtPass = new JPasswordField();
        styleTextField(txtPass, "Contraseña");
        JTextField txtEdad     = styledTextField("Edad");

        JPanel generoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        generoPanel.setBackground(BLANCO);
        generoPanel.setMaximumSize(new Dimension(330, 35));
        JLabel lblGen = new JLabel("Género:  ");
        lblGen.setFont(FONT_NORM);
        ButtonGroup bgGen = new ButtonGroup();
        JRadioButton rbM = new JRadioButton("Masculino");
        JRadioButton rbF = new JRadioButton("Femenino");
        rbM.setFont(FONT_NORM); rbM.setBackground(BLANCO);
        rbF.setFont(FONT_NORM); rbF.setBackground(BLANCO);
        rbM.setSelected(true);
        bgGen.add(rbM); bgGen.add(rbF);
        generoPanel.add(lblGen); generoPanel.add(rbM); generoPanel.add(rbF);

        JPanel tipoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tipoPanel.setBackground(BLANCO);
        tipoPanel.setMaximumSize(new Dimension(330, 35));
        JLabel lblTipo = new JLabel("Cuenta:  ");
        lblTipo.setFont(FONT_NORM);
        ButtonGroup bgTipo = new ButtonGroup();
        JRadioButton rbPub  = new JRadioButton("Pública");
        JRadioButton rbPriv = new JRadioButton("Privada");
        rbPub.setFont(FONT_NORM);  rbPub.setBackground(BLANCO);
        rbPriv.setFont(FONT_NORM); rbPriv.setBackground(BLANCO);
        rbPub.setSelected(true);
        bgTipo.add(rbPub); bgTipo.add(rbPriv);
        tipoPanel.add(lblTipo); tipoPanel.add(rbPub); tipoPanel.add(rbPriv);

        final File[] archivoFoto = { null };
        JPanel fotoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fotoPanel.setBackground(BLANCO);
        fotoPanel.setMaximumSize(new Dimension(330, 35));
        JLabel lblFotoNombre = new JLabel("Sin foto elegida");
        lblFotoNombre.setFont(FONT_SMALL);
        lblFotoNombre.setForeground(Color.GRAY);
        JButton btnElegirFoto = igButton("📷 Foto de perfil", GRIS_BORDE, NEGRO);
        btnElegirFoto.setMaximumSize(new Dimension(150, 32));
        btnElegirFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Elegir foto de perfil");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archivoFoto[0] = chooser.getSelectedFile();
                lblFotoNombre.setText(archivoFoto[0].getName());
                lblFotoNombre.setForeground(NEGRO);
            }
        });
        fotoPanel.add(btnElegirFoto);
        fotoPanel.add(Box.createHorizontalStrut(8));
        fotoPanel.add(lblFotoNombre);

        JButton btnReg = igButton("Registrarse", ROSA_IG, BLANCO);
        btnReg.addActionListener(e -> {
            String nombre   = txtNombre.getText().trim();
            String username = txtUsername.getText().trim();
            String pass     = new String(txtPass.getPassword()).trim();
            String edadStr  = txtEdad.getText().trim();

            UserManager.Genero genero = rbM.isSelected()
                ? UserManager.Genero.M : UserManager.Genero.F;
            UserManager.TipoCuenta tipo = rbPub.isSelected()
                ? UserManager.TipoCuenta.PUBLICO : UserManager.TipoCuenta.PRIVADO;

            if (nombre.isEmpty() || username.isEmpty() || pass.isEmpty() || edadStr.isEmpty()) {
                showError("Completa todos los campos."); return;
            }
            int edad;
            try { edad = Integer.parseInt(edadStr); } catch (NumberFormatException ex) {
                showError("Edad inválida."); return;
            }
            try {
                boolean ok = userManager.crearUser(
                    username, nombre, genero, pass, edad, archivoFoto[0], tipo);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "¡Cuenta creada! Ya puedes iniciar sesión.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOGIN");
                } else {
                    showError("El nombre de usuario ya existe.");
                }
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        JButton btnVolver = new JButton("← Ya tengo cuenta");
        btnVolver.setFont(FONT_SMALL); btnVolver.setForeground(AZUL_IG);
        btnVolver.setBorderPainted(false); btnVolver.setContentAreaFilled(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnVolver.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        caja.add(logo);
        caja.add(Box.createVerticalStrut(5));
        caja.add(sub);
        caja.add(Box.createVerticalStrut(20));
        caja.add(txtNombre);
        caja.add(Box.createVerticalStrut(8));
        caja.add(txtUsername);
        caja.add(Box.createVerticalStrut(8));
        caja.add(txtPass);
        caja.add(Box.createVerticalStrut(8));
        caja.add(txtEdad);
        caja.add(Box.createVerticalStrut(10));
        caja.add(generoPanel);
        caja.add(tipoPanel);
        caja.add(Box.createVerticalStrut(6));
        caja.add(fotoPanel);
        caja.add(Box.createVerticalStrut(15));
        caja.add(btnReg);
        caja.add(Box.createVerticalStrut(15));
        caja.add(btnVolver);

        panel.add(caja);
        return panel;
    }

    private JPanel homeFeed;
    private JPanel homeBuscar;
    private JPanel homeInbox;
    private JPanel homePerfil;
    private JLabel lblNavBadge;

    private final CardLayout homeCard   = new CardLayout();
    private final JPanel     homeCenter = new JPanel(homeCard);

    private JPanel construirPantallaHome() {
        JPanel panel = new JPanel(new BorderLayout());

        headerHome = new JPanel(new BorderLayout(0, 0));
        headerHome.setBackground(BLANCO);
        headerHome.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        headerHome.setPreferredSize(new Dimension(390, 48));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        izq.setBackground(BLANCO);
        izq.add(avatarCircular(usuarioActual != null ? usuarioActual.getFotoPerfil() : null, 32));
        headerHome.add(izq, BorderLayout.WEST);

        JLabel logoHeader = new JLabel("Instagram", SwingConstants.CENTER);
        logoHeader.setFont(new Font("Serif", Font.ITALIC, 20));
        headerHome.add(logoHeader, BorderLayout.CENTER);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 10));
        der.setBackground(BLANCO);

        ImageIcon icoNotif = cargarIcono("icon_notif.png", 18);
        JButton btnNotif = icoNotif != null ? new JButton(icoNotif) : new JButton("🔔");
        if (icoNotif == null) btnNotif.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnNotif.setBorderPainted(false); btnNotif.setContentAreaFilled(false);
        btnNotif.setFocusPainted(false); btnNotif.setMargin(new Insets(0,4,0,4));
        btnNotif.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNotif.addActionListener(e -> mostrarVista("NOTIF"));

        ImageIcon icoInbox = cargarIcono("icon_inbox.png", 18);
        JButton btnInboxHeader = icoInbox != null ? new JButton(icoInbox) : new JButton("✈");
        if (icoInbox == null) btnInboxHeader.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnInboxHeader.setBorderPainted(false); btnInboxHeader.setContentAreaFilled(false);
        btnInboxHeader.setFocusPainted(false); btnInboxHeader.setMargin(new Insets(0,4,0,4));
        btnInboxHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInboxHeader.addActionListener(e -> mostrarVista("INBOX"));

        der.add(btnNotif);
        der.add(btnInboxHeader);
        headerHome.add(der, BorderLayout.EAST);

        panel.add(headerHome, BorderLayout.NORTH);
        homeCenter.setBackground(BLANCO);
        panel.add(homeCenter, BorderLayout.CENTER);
        panel.add(construirNavBar(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirNavBar() {
        JPanel nav = new JPanel(new GridLayout(1, 4));
        nav.setBackground(BLANCO);
        nav.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        nav.setPreferredSize(new Dimension(390, 50));

        String[] archivos = {"icon_home.png", "icon_search.png", "icon_add.png", null};
        String[] fallback  = {"⌂", "🔍", "＋", null};
        String[] vistas    = {"FEED", "BUSCAR", "POST", "PERFIL"};

        for (int i = 0; i < 4; i++) {
            final String vista = vistas[i];
            JPanel celda = new JPanel(new GridBagLayout());
            celda.setBackground(BLANCO);

            JButton btn;
            if (i == 3) {

                JLabel avatarNav = avatarCircular(
                    usuarioActual != null ? usuarioActual.getFotoPerfil() : null, 26);
                btn = new JButton();
                btn.setPreferredSize(new Dimension(26, 26));
                btn.setLayout(new BorderLayout());
                btn.add(avatarNav, BorderLayout.CENTER);
            } else {
                ImageIcon ico = cargarIcono(archivos[i], 20);
                if (ico != null) {
                    btn = new JButton(ico);
                    btn.setPreferredSize(new Dimension(26, 26));
                } else {
                    btn = new JButton(fallback[i]);
                    btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    btn.setForeground(NEGRO);
                }
            }
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(0,0,0,0));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> mostrarVista(vista));
            celda.add(btn);
            nav.add(celda);
        }
        return nav;
    }

    private void mostrarHome() {
        inicializarCuentasPredeterminadas();

        mainPanel.remove(getComponentByName("HOME"));
        JPanel nuevoHome = construirPantallaHome();
        mainPanel.add(nuevoHome, "HOME");

        homeCenter.removeAll();
        homeCenter.add(construirFeed(),            "FEED");
        homeCenter.add(construirBuscar(),           "BUSCAR");
        homeCenter.add(construirPost(),             "POST");
        homeCenter.add(construirInbox(),            "INBOX");
        homeCenter.add(construirNotificaciones(),   "NOTIF");
        homeCenter.add(construirConfiguracion(),    "CONFIG");
        homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
        homeCard.show(homeCenter, "FEED");
        cardLayout.show(mainPanel, "HOME");
    }

    private void mostrarVista(String vista) {
        if (vista.equals("POST")) {

            homeCenter.remove(getComponentByName("POST"));
            homeCenter.add(construirPost(), "POST");
            homeCard.show(homeCenter, "POST");
            return;
        }
        vistaAnterior = vista;
        if (vista.equals("INBOX")) {
            homeCenter.remove(getComponentByName("INBOX"));
            homeCenter.add(construirInbox(), "INBOX");
        }
        if (vista.equals("NOTIF")) {
            homeCenter.remove(getComponentByName("NOTIF"));
            homeCenter.add(construirNotificaciones(), "NOTIF");
        }
        if (vista.equals("CONFIG")) {
            homeCenter.remove(getComponentByName("CONFIG"));
            homeCenter.add(construirConfiguracion(), "CONFIG");
        }
        if (vista.equals("FEED")) {
            homeCenter.remove(getComponentByName("FEED"));
            homeCenter.add(construirFeed(), "FEED");
        }
        if (vista.equals("PERFIL")) {
            homeCenter.remove(getComponentByName("PERFIL"));
            homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
        }
        homeCard.show(homeCenter, vista);
    }

    private Component getComponentByName(String name) {
        for (Component c : homeCenter.getComponents()) {

        }
        return new JPanel();
    }

    private JPanel construirFeed() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        JPanel feedPanel = new JPanel();
        feedPanel.setLayout(new BoxLayout(feedPanel, BoxLayout.Y_AXIS));
        feedPanel.setBackground(BLANCO);

        try {
            List<String> following = followManager.obtenerFollowing(usuarioActual.getUsername());
            List<Publicacion> pubs = pubManager.obtenerFeed(
                usuarioActual.getUsername(), following, userManager);

            String[] predeterminadas = {"popeyes", "realmadrid", "unitec"};
            for (String cuenta : predeterminadas) {
                if (!following.contains(cuenta) &&
                    !cuenta.equalsIgnoreCase(usuarioActual.getUsername())) {
                    List<Publicacion> extra = pubManager.obtenerPublicaciones(cuenta);
                    for (Publicacion p : extra) pubs.add(p);
                }
            }

            for (int i = 0; i < pubs.size() - 1; i++)
                for (int j = i + 1; j < pubs.size(); j++)
                    if (pubs.get(i).getFecha() < pubs.get(j).getFecha()) {
                        Publicacion tmp = pubs.get(i);
                        pubs.set(i, pubs.get(j));
                        pubs.set(j, tmp);
                    }

            if (pubs.isEmpty()) {
                JLabel vacio = new JLabel("Aún no hay publicaciones. ¡Sigue a alguien!");
                vacio.setFont(FONT_NORM);
                vacio.setForeground(Color.GRAY);
                vacio.setAlignmentX(Component.CENTER_ALIGNMENT);
                feedPanel.add(Box.createVerticalStrut(40));
                feedPanel.add(vacio);
            } else {
                for (Publicacion p : pubs) {
                    feedPanel.add(construirTarjetaPublicacion(p));
                    feedPanel.add(Box.createVerticalStrut(8));
                }
            }
        } catch (IOException e) { showError(e.getMessage()); }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BLANCO);
        centerWrapper.add(feedPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(centerWrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        SwingUtilities.invokeLater(() ->
            scroll.getVerticalScrollBar().setValue(0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel construirTarjetaPublicacion(Publicacion pubParam) {
        final Publicacion p = pubParam;
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BLANCO);
        card.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.setMaximumSize(new Dimension(32767, 999));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        cardHeader.setBackground(BLANCO);

        try {
            User autorUser = userManager.buscarporUsername(p.getUserAutor());
            File fotoAutor = (autorUser != null) ? autorUser.getFotoPerfil() : null;
            cardHeader.add(avatarCircular(fotoAutor, 36));
        } catch (IOException ex) { cardHeader.add(avatarCircular(null, 36)); }
        JLabel userLbl = new JLabel(p.getUserAutor());
        userLbl.setFont(FONT_BOLD);
        userLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userLbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirPerfil(p.getUserAutor()); }
        });
        cardHeader.add(userLbl);

        card.add(cardHeader);

        if (!p.getRutaImagen().isEmpty()) {
            File imgFile = new File(p.getRutaImagen());
            if (imgFile.exists()) {

                ImageIcon rawIcon = new ImageIcon(p.getRutaImagen());
                int origW = rawIcon.getIconWidth();
                int origH = rawIcon.getIconHeight();
                int targetW, targetH;
                if (origW <= 0 || origH <= 0) { targetW = 370; targetH = 370; }
                else {
                    double ratio = (double) origH / origW;
                    if (ratio > 1.15) {

                        targetW = 370;
                        targetH = Math.min((int)(targetW * ratio), 463);
                    } else if (ratio < 0.85) {

                        targetW = 370; targetH = (int)(targetW * ratio);
                    } else {

                        targetW = 370; targetH = 370;
                    }
                }
                Image scaled = rawIcon.getImage()
                    .getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                JLabel imgLabel = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
                imgLabel.setMaximumSize(new Dimension(390, targetH));
                imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(imgLabel);
            } else if (!p.getTipoMultimedia().equals("NINGUNA")) {
                card.add(imagePlaceholder(p.getTipoMultimedia()));
            }
        }

        JTextArea txtContenido = new JTextArea(p.getContenido());
        txtContenido.setFont(FONT_NORM);
        txtContenido.setWrapStyleWord(true);
        txtContenido.setLineWrap(true);
        txtContenido.setEditable(false);
        txtContenido.setBackground(BLANCO);
        txtContenido.setBorder(new EmptyBorder(4, 12, 4, 12));
        txtContenido.setMaximumSize(new Dimension(390, 999));
        card.add(txtContenido);

        JPanel footerCard = new JPanel();
        footerCard.setLayout(new BoxLayout(footerCard, BoxLayout.Y_AXIS));
        footerCard.setBackground(BLANCO);
        footerCard.setBorder(new EmptyBorder(2, 8, 6, 8));

        JPanel botonesRow = new JPanel(new BorderLayout());
        botonesRow.setBackground(BLANCO);

        JPanel izqBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        izqBtns.setBackground(BLANCO);

        String likeKey = p.getUserAutor() + "|" + p.getFecha();
        boolean yaLikeado = likesEnSesion.contains(likeKey);

        final JLabel[] lblLikesRef = {null};

        ImageIcon icoLikeVacio  = cargarIcono("icon_like.png",        20);
        ImageIcon icoLikeLleno  = cargarIcono("icon_like_filled.png", 20);
        JButton btnLike = new JButton(yaLikeado
            ? (icoLikeLleno  != null ? icoLikeLleno  : null)
            : (icoLikeVacio  != null ? icoLikeVacio  : null));
        if (btnLike.getIcon() == null) {
            btnLike.setText(yaLikeado ? "♥" : "♡");
            btnLike.setFont(new Font("SansSerif", Font.PLAIN, 22));
        }
        btnLike.setForeground(yaLikeado ? ROSA_IG : NEGRO);
        btnLike.setBorderPainted(false); btnLike.setContentAreaFilled(false);
        btnLike.setFocusPainted(false);
        btnLike.setPreferredSize(new Dimension(28, 28));
        btnLike.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLike.addActionListener(e -> {
            try {
                if (likesEnSesion.contains(likeKey)) {
                    pubManager.quitarLike(p.getUserAutor(), p.getFecha());
                    p.setLikes(Math.max(0, p.getLikes() - 1));
                    likesEnSesion.remove(likeKey);
                    ImageIcon icoV = cargarIcono("icon_like.png", 20);
                    if (icoV != null) { btnLike.setIcon(icoV); btnLike.setText(""); }
                    else { btnLike.setIcon(null); btnLike.setText("♡"); btnLike.setFont(new Font("SansSerif", Font.PLAIN, 18)); }
                    btnLike.setPreferredSize(new Dimension(28, 28));
                    btnLike.setForeground(NEGRO);
                } else {
                    pubManager.darLike(p.getUserAutor(), p.getFecha());
                    p.setLikes(p.getLikes() + 1);
                    likesEnSesion.add(likeKey);
                    ImageIcon icoL = cargarIcono("icon_like_filled.png", 20);
                    if (icoL != null) { btnLike.setIcon(icoL); btnLike.setText(""); }
                    else { btnLike.setIcon(null); btnLike.setText("♥"); btnLike.setFont(new Font("SansSerif", Font.PLAIN, 18)); }
                    btnLike.setPreferredSize(new Dimension(28, 28));
                    btnLike.setForeground(ROSA_IG);
                }

                lblLikesRef[0].setText(p.getLikes() > 0 ? String.valueOf(p.getLikes()) : "");
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        ImageIcon icoComment = cargarIcono("icon_comment.png", 20);
        JButton btnComment = icoComment != null
            ? new JButton(icoComment) : new JButton("💬");
        if (icoComment == null) btnComment.setFont(new Font("SansSerif", Font.PLAIN, 18));
        btnComment.setForeground(NEGRO);
        btnComment.setBorderPainted(false); btnComment.setContentAreaFilled(false);
        btnComment.setFocusPainted(false);
        btnComment.setPreferredSize(new Dimension(28, 28));
        btnComment.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnComment.addActionListener(e -> abrirComentarios(p));

        ImageIcon icoShare = cargarIcono("icon_share.png", 20);
        JButton btnShare = icoShare != null
            ? new JButton(icoShare) : new JButton("✈");
        if (icoShare == null) btnShare.setFont(new Font("SansSerif", Font.PLAIN, 18));
        btnShare.setForeground(NEGRO);
        btnShare.setBorderPainted(false); btnShare.setContentAreaFilled(false);
        btnShare.setFocusPainted(false);
        btnShare.setPreferredSize(new Dimension(28, 28));
        btnShare.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnShare.addActionListener(e -> {

            String dest = JOptionPane.showInputDialog(this, "Enviar a usuario:");
            if (dest != null && !dest.trim().isEmpty())
                abrirConversacion(dest.trim());
        });

        JLabel lblLikeCount = new JLabel(p.getLikes() > 0 ? String.valueOf(p.getLikes()) : "");
        lblLikeCount.setFont(FONT_SMALL);
        lblLikeCount.setForeground(NEGRO);

        int numComments = 0;
        try { numComments = comentarioManager.obtenerComentarios(
                p.getUserAutor(), p.getFecha()).size(); } catch (IOException ex) {}
        JLabel lblCommCount = new JLabel(numComments > 0 ? String.valueOf(numComments) : "");
        lblCommCount.setFont(FONT_SMALL);
        lblCommCount.setForeground(NEGRO);

        lblLikesRef[0] = lblLikeCount;

        izqBtns.add(btnLike);
        izqBtns.add(lblLikeCount);
        izqBtns.add(Box.createHorizontalStrut(6));
        izqBtns.add(btnComment);
        izqBtns.add(lblCommCount);
        izqBtns.add(Box.createHorizontalStrut(6));
        izqBtns.add(btnShare);
        botonesRow.add(izqBtns, BorderLayout.WEST);
        footerCard.add(botonesRow);

        JPanel fechaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fechaRow.setBackground(BLANCO);
        JLabel lblFecha = new JLabel(p.getFechaStr());
        lblFecha.setFont(FONT_SMALL);
        lblFecha.setForeground(Color.GRAY);
        fechaRow.add(lblFecha);
        footerCard.add(fechaRow);
        card.add(footerCard);

        card.putClientProperty("lblCommCount_" + p.getFecha(), lblCommCount);

        btnComment.addActionListener(ev2 -> SwingUtilities.invokeLater(() -> {
            try {
                int n = comentarioManager.obtenerComentarios(
                    p.getUserAutor(), p.getFecha()).size();
                lblCommCount.setText(n > 0 ? String.valueOf(n) : "");
            } catch (IOException ex2) {}
        }));

        return card;
    }

    private void abrirComentarios(Publicacion p) {

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 44));
        JButton btnV = new JButton("←  Volver");
        btnV.setFont(FONT_BOLD); btnV.setForeground(AZUL_IG);
        btnV.setBorderPainted(false); btnV.setContentAreaFilled(false);
        btnV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnV.addActionListener(e -> { homeCenter.remove(wrapper); homeCard.show(homeCenter, vistaAnterior); });
        JLabel lblTit = new JLabel("Comentarios", SwingConstants.CENTER);
        lblTit.setFont(FONT_BOLD);
        hdr.add(btnV, BorderLayout.WEST);
        hdr.add(lblTit, BorderLayout.CENTER);
        wrapper.add(hdr, BorderLayout.NORTH);

        JPanel commPanel = new JPanel();
        commPanel.setLayout(new BoxLayout(commPanel, BoxLayout.Y_AXIS));
        commPanel.setBackground(BLANCO);
        commPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

        Runnable cargarComentarios = () -> {
            while (commPanel.getComponentCount() > 0) commPanel.remove(0);
            try {
                for (Comentario c : comentarioManager.obtenerComentarios(
                        p.getUserAutor(), p.getFecha())) {
                    commPanel.add(filaComentario(c, p));
                }
            } catch (IOException ex) {}
            commPanel.revalidate(); commPanel.repaint();
        };
        cargarComentarios.run();

        JScrollPane scrollComm = new JScrollPane(commPanel);
        scrollComm.setBorder(null);
        scrollComm.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollComm.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        wrapper.add(scrollComm, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
        inputPanel.setBackground(BLANCO);
        inputPanel.add(avatarCircular(usuarioActual.getFotoPerfil(), 32), BorderLayout.WEST);

        JTextField txtComentario = new JTextField();
        txtComentario.setFont(FONT_NORM);
        txtComentario.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(GRIS_BORDE, 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        txtComentario.setToolTipText("Usa @usuario o #hashtag");

        JButton btnEnviarComm = new JButton("Publicar");
        btnEnviarComm.setFont(FONT_BOLD);
        btnEnviarComm.setForeground(AZUL_IG);
        btnEnviarComm.setBorderPainted(false);
        btnEnviarComm.setContentAreaFilled(false);
        btnEnviarComm.setFocusPainted(false);
        btnEnviarComm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ActionListener enviarComm = ev -> {
            String texto = txtComentario.getText().trim();
            if (texto.isEmpty()) return;
            try {

                comentarioManager.agregarComentario(
                    p.getUserAutor(), p.getFecha(),
                    usuarioActual.getUsername(), texto);
                txtComentario.setText("");
                cargarComentarios.run();

                int nComm = comentarioManager.obtenerComentarios(
                    p.getUserAutor(), p.getFecha()).size();

                for (Component comp : homeCenter.getComponents()) {
                    if (comp instanceof JPanel) {
                        actualizarCommCount((JPanel)comp, p.getFecha(), nComm);
                    }
                }
                SwingUtilities.invokeLater(() ->
                    scrollComm.getVerticalScrollBar().setValue(
                        scrollComm.getVerticalScrollBar().getMaximum()));
            } catch (IOException ex) { showError(ex.getMessage()); }
        };
        btnEnviarComm.addActionListener(enviarComm);
        txtComentario.addActionListener(enviarComm);

        inputPanel.add(txtComentario, BorderLayout.CENTER);
        inputPanel.add(btnEnviarComm, BorderLayout.EAST);

        JPanel sugerComm = new JPanel();
        sugerComm.setLayout(new BoxLayout(sugerComm, BoxLayout.Y_AXIS));
        sugerComm.setBackground(BLANCO);
        sugerComm.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        sugerComm.setVisible(false);

        txtComentario.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void actualizar() {
                sugerComm.removeAll();
                String texto = txtComentario.getText();

                int caret = txtComentario.getCaretPosition();
                if (caret > texto.length()) caret = texto.length();
                String antes = texto.substring(0, caret);

                int inicio = antes.lastIndexOf(' ') + 1;
                String palabraActual = antes.substring(inicio);

                if (palabraActual.startsWith("@") && palabraActual.length() > 1) {

                    String query = palabraActual.substring(1);
                    try {
                        List<User> lista = userManager.busquedaParcial(query);
                        int max = Math.min(lista.size(), 4);
                        for (int i = 0; i < max; i++) {
                            User u = lista.get(i);
                            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                            fila.setBackground(BLANCO);
                            fila.setMaximumSize(new Dimension(390, 40));
                            fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            try { fila.add(avatarCircular(u.getFotoPerfil(), 28)); }
                            catch (Exception ex) { fila.add(avatarCircular(null, 28)); }
                            JLabel lbl = new JLabel("@" + u.getUsername()
                                + "  " + u.getNombreCompleto());
                            lbl.setFont(FONT_NORM);
                            fila.add(lbl);
                            fila.addMouseListener(new MouseAdapter() {
                                public void mouseClicked(MouseEvent e) {

                                    String actual = txtComentario.getText();
                                    int car = txtComentario.getCaretPosition();
                                    if (car > actual.length()) car = actual.length();
                                    int ini = actual.lastIndexOf(' ', car - 1) + 1;
                                    String nuevo = actual.substring(0, ini)
                                        + "@" + u.getUsername() + " "
                                        + actual.substring(car);
                                    txtComentario.setText(nuevo);
                                    txtComentario.setCaretPosition(
                                        ini + u.getUsername().length() + 2);
                                    sugerComm.setVisible(false);
                                    sugerComm.revalidate(); sugerComm.repaint();
                                }
                            });
                            sugerComm.add(fila);
                            if (i < max - 1) sugerComm.add(new JSeparator());
                        }
                        sugerComm.setVisible(!lista.isEmpty());
                    } catch (IOException ex) {}

                } else if (palabraActual.startsWith("#") && palabraActual.length() > 1) {

                    String tag = palabraActual;
                    try {
                        List<User> todos = userManager.obtenerUsuarios();
                        List<Publicacion> pubs = pubManager.buscarPorHashtag(tag, todos);

                        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();
                        for (Publicacion pub : pubs)
                            for (String h : pub.getHastags())
                                if (h.startsWith(tag.toLowerCase())) tags.add(h);
                        int i = 0;
                        for (String h : tags) {
                            if (i >= 4) break;
                            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
                            fila.setBackground(BLANCO);
                            fila.setMaximumSize(new Dimension(390, 36));
                            fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            JLabel lbl = new JLabel(h);
                            lbl.setFont(FONT_BOLD);
                            lbl.setForeground(ROSA_IG);
                            fila.add(lbl);
                            final String tagFinal = h;
                            fila.addMouseListener(new MouseAdapter() {
                                public void mouseClicked(MouseEvent e) {
                                    String actual = txtComentario.getText();
                                    int car = txtComentario.getCaretPosition();
                                    if (car > actual.length()) car = actual.length();
                                    int ini = actual.lastIndexOf(' ', car - 1) + 1;
                                    String nuevo = actual.substring(0, ini)
                                        + tagFinal + " "
                                        + actual.substring(car);
                                    txtComentario.setText(nuevo);
                                    txtComentario.setCaretPosition(
                                        ini + tagFinal.length() + 1);
                                    sugerComm.setVisible(false);
                                    sugerComm.revalidate(); sugerComm.repaint();
                                }
                            });
                            sugerComm.add(fila);
                            if (i < Math.min(tags.size(), 4) - 1)
                                sugerComm.add(new JSeparator());
                            i++;
                        }
                        sugerComm.setVisible(!tags.isEmpty());
                    } catch (IOException ex) {}
                } else {
                    sugerComm.setVisible(false);
                }
                sugerComm.revalidate(); sugerComm.repaint();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { actualizar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { actualizar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizar(); }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BLANCO);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        bottom.add(sugerComm, BorderLayout.NORTH);
        bottom.add(inputPanel, BorderLayout.CENTER);
        wrapper.add(bottom, BorderLayout.SOUTH);

        homeCenter.add(wrapper, "COMENTARIOS");
        homeCard.show(homeCenter, "COMENTARIOS");
    }

    private JPanel filaComentario(Comentario c, Publicacion p) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setBackground(BLANCO);
        fila.setBorder(new EmptyBorder(2, 14, 2, 14));

        JPanel avWrap = new JPanel(new BorderLayout());
        avWrap.setBackground(BLANCO);
        avWrap.setBorder(new EmptyBorder(2, 0, 0, 0));
        try {
            User u = userManager.buscarporUsername(c.getAutor());
            avWrap.add(avatarCircular(u != null ? u.getFotoPerfil() : null, 32), BorderLayout.NORTH);
        } catch (IOException ex) {
            avWrap.add(avatarCircular(null, 32), BorderLayout.NORTH);
        }
        fila.add(avWrap, BorderLayout.WEST);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(BLANCO);
        textCol.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel lblUser = new JLabel(c.getAutor());
        lblUser.setFont(FONT_BOLD);

        JLabel lblText = new JLabel("<html><div style='width:260px'>"
            + c.getContenido().replace("<","&lt;").replace(">","&gt;")
            + "</div></html>");
        lblText.setFont(FONT_NORM);
        lblText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblText.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                for (String palabra : c.getContenido().split("\\s+")) {
                    if (palabra.startsWith("@") && palabra.length() > 1) {
                        abrirPerfil(palabra.substring(1)); return;
                    }
                    if (palabra.startsWith("#") && palabra.length() > 1) {
                        mostrarVista("BUSCAR"); return;
                    }
                }
            }
        });

        textCol.add(lblUser);
        textCol.add(lblText);
        fila.add(textCol, BorderLayout.CENTER);
        return fila;
    }

    private String resaltarTexto(String texto) {
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.split("(?<=\\s)|(?=\\s)")) {
            if (palabra.startsWith("@") && palabra.length() > 1)
                sb.append("<font color='#0095F6'>").append(palabra).append("</font>");
            else if (palabra.startsWith("#") && palabra.length() > 1)
                sb.append("<font color='#E1306C'>").append(palabra).append("</font>");
            else
                sb.append(palabra);
        }
        return sb.toString();
    }

    private JPanel construirBuscar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        JPanel barraBusqueda = new JPanel(new BorderLayout(8, 0));
        barraBusqueda.setBackground(BLANCO);
        barraBusqueda.setBorder(new EmptyBorder(10, 12, 10, 12));

        JTextField txtBuscar = new JTextField();
        txtBuscar.setFont(FONT_NORM);
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(GRIS_BORDE, 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        txtBuscar.setBackground(GRIS_CLARO);

        JButton btnBuscar = igButton("Buscar", AZUL_IG, BLANCO);
        btnBuscar.setPreferredSize(new Dimension(80, 32));

        barraBusqueda.add(txtBuscar, BorderLayout.CENTER);
        barraBusqueda.add(btnBuscar, BorderLayout.EAST);

        JPanel resultados = new JPanel();
        resultados.setLayout(new BoxLayout(resultados, BoxLayout.Y_AXIS));
        resultados.setBackground(BLANCO);
        JScrollPane scroll = new JScrollPane(resultados);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        txtBuscar.setToolTipText("@ para usuarios, # para hashtags");

        Runnable buscar = () -> {
            resultados.removeAll();
            String q = txtBuscar.getText().trim();
            if (q.isEmpty()) { resultados.revalidate(); resultados.repaint(); return; }
            try {
                if (q.startsWith("#")) {

                    List<User> todos = userManager.obtenerUsuarios();
                    List<Publicacion> pubs = pubManager.buscarPorHashtag(q, todos);
                    if (pubs.isEmpty()) {
                        resultados.add(grayLabel("Sin publicaciones con " + q));
                    } else {
                        for (Publicacion p : pubs) {
                            resultados.add(construirTarjetaPublicacion(p));
                            resultados.add(Box.createVerticalStrut(6));
                        }
                    }
                } else {

                    String query = q.startsWith("@") ? q.substring(1) : q;
                    List<User> lista = userManager.busquedaParcial(query);
                    if (lista.isEmpty()) {
                        resultados.add(grayLabel("Sin resultados."));
                    } else {
                        for (User u : lista) resultados.add(filaPerfil(u));
                    }
                }
            } catch (IOException e) { showError(e.getMessage()); }
            resultados.revalidate();
            resultados.repaint();
        };

        btnBuscar.addActionListener(e -> buscar.run());
        txtBuscar.addActionListener(e -> buscar.run());

        JPanel sugerenciasPanel = new JPanel();
        sugerenciasPanel.setLayout(new BoxLayout(sugerenciasPanel, BoxLayout.Y_AXIS));
        sugerenciasPanel.setBackground(BLANCO);
        sugerenciasPanel.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        sugerenciasPanel.setVisible(false);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void actualizar() {
                String q = txtBuscar.getText().trim();
                sugerenciasPanel.removeAll();
                if (q.isEmpty()) {
                    sugerenciasPanel.setVisible(false);
                    sugerenciasPanel.revalidate(); sugerenciasPanel.repaint(); return;
                }
                try {
                    if (q.startsWith("#") && q.length() > 1) {

                        List<User> todos = userManager.obtenerUsuarios();
                        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();
                        for (User u : todos) {
                            for (Publicacion pub : pubManager.obtenerPublicaciones(u.getUsername())) {
                                for (String h : pub.getHastags()) {
                                    if (h.toLowerCase().startsWith(q.toLowerCase())) tags.add(h);
                                }
                            }
                        }
                        int i = 0;
                        for (String tag : tags) {
                            if (i++ >= 5) break;
                            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
                            fila.setBackground(BLANCO);
                            fila.setMaximumSize(new Dimension(390, 38));
                            fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            JLabel lbl = new JLabel(tag);
                            lbl.setFont(FONT_BOLD);
                            lbl.setForeground(ROSA_IG);
                            fila.add(lbl);
                            final String tagFinal = tag;
                            fila.addMouseListener(new MouseAdapter() {
                                public void mouseClicked(MouseEvent e) {
                                    txtBuscar.setText(tagFinal);
                                    sugerenciasPanel.setVisible(false);
                                    buscar.run();
                                }
                            });
                            sugerenciasPanel.add(fila);
                            if (i < Math.min(tags.size(), 5)) sugerenciasPanel.add(new JSeparator());
                        }
                        sugerenciasPanel.setVisible(!tags.isEmpty());
                    } else if (!q.startsWith("#")) {

                        String query = q.startsWith("@") ? q.substring(1) : q;
                        List<User> lista = userManager.busquedaParcial(query);
                        int max = Math.min(lista.size(), 4);
                        for (int i = 0; i < max; i++) {
                            User u = lista.get(i);
                            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
                            fila.setBackground(BLANCO);
                            fila.setMaximumSize(new Dimension(390, 40));
                            fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            try { fila.add(avatarCircular(userManager.buscarporUsername(
                                u.getUsername()).getFotoPerfil(), 28)); }
                            catch (Exception ex) { fila.add(avatarCircular(null, 28)); }
                            JLabel lbl = new JLabel("@" + u.getUsername()
                                + "  " + u.getNombreCompleto());
                            lbl.setFont(FONT_NORM);
                            fila.add(lbl);
                            fila.addMouseListener(new MouseAdapter() {
                                public void mouseClicked(MouseEvent e) {
                                    sugerenciasPanel.setVisible(false);
                                    txtBuscar.setText("");
                                    abrirPerfil(u.getUsername());
                                }
                            });
                            sugerenciasPanel.add(fila);
                            if (i < max - 1) sugerenciasPanel.add(new JSeparator());
                        }
                        sugerenciasPanel.setVisible(!lista.isEmpty());
                    } else {
                        sugerenciasPanel.setVisible(false);
                    }
                } catch (IOException ex) {}
                sugerenciasPanel.revalidate(); sugerenciasPanel.repaint();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { actualizar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { actualizar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizar(); }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BLANCO);
        top.add(barraBusqueda, BorderLayout.NORTH);
        top.add(sugerenciasPanel, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel filaPerfil(User u) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        fila.setBackground(BLANCO);
        fila.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        fila.setMaximumSize(new Dimension(390, 60));
        fila.add(avatarCircular(u.getFotoPerfil(), 40));
        fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fila.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirPerfil(u.getUsername()); }
        });
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(BLANCO);
        JLabel lblUser = new JLabel("@" + u.getUsername());
        lblUser.setFont(FONT_BOLD);
        JLabel lblNom  = new JLabel(u.getNombreCompleto() + " · " + u.getTipoCuenta().name());
        lblNom.setFont(FONT_SMALL);
        lblNom.setForeground(Color.GRAY);
        info.add(lblUser); info.add(lblNom);
        fila.add(info);
        fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fila.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirPerfil(u.getUsername()); }
        });
        return fila;
    }

    private JPanel construirInbox() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        JPanel headerInbox = new JPanel(new BorderLayout());
        headerInbox.setBackground(BLANCO);
        headerInbox.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel lblTitulo = new JLabel("Mensajes");
        lblTitulo.setFont(FONT_BOLD);
        JButton btnNuevo = igButton("+ Nuevo", AZUL_IG, BLANCO);
        btnNuevo.addActionListener(e -> dialogNuevaMensaje());
        headerInbox.add(lblTitulo, BorderLayout.WEST);
        headerInbox.add(btnNuevo, BorderLayout.EAST);

        JPanel listaPanel = new JPanel();
        listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
        listaPanel.setBackground(BLANCO);

        try {
            List<String> convs = inboxManager.obtenerConversaciones(usuarioActual.getUsername());
            if (convs.isEmpty()) {
                listaPanel.add(Box.createVerticalStrut(30));
                listaPanel.add(grayLabel("Sin conversaciones."));
            } else {
                for (String otro : convs) {
                    listaPanel.add(filaConversacion(otro));
                }
            }
        } catch (IOException e) { showError(e.getMessage()); }

        JScrollPane scroll = new JScrollPane(listaPanel);
        scroll.setBorder(null);

        panel.add(headerInbox, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel filaConversacion(String otroUsername) {
        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.setBackground(BLANCO);
        fila.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        fila.setMaximumSize(new Dimension(390, 65));
        fila.setBorder(new EmptyBorder(8, 12, 8, 12));

        try {
            User otro = userManager.buscarporUsername(otroUsername);
            fila.add(avatarCircular(otro != null ? otro.getFotoPerfil() : null, 46), BorderLayout.WEST);
        } catch (IOException ex) { fila.add(avatarCircular(null, 46), BorderLayout.WEST); }

        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(BLANCO);

        JLabel lblUser = new JLabel("@" + otroUsername);
        lblUser.setFont(FONT_BOLD);
        info.add(lblUser, BorderLayout.NORTH);

        // Badge de no leídos — punto azul como Instagram
        JLabel badge = new JLabel();
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(AZUL_IG);
        info.add(badge, BorderLayout.CENTER);

        // Helper para contar y actualizar el badge instantáneamente
        Runnable actualizarBadge = () -> {
            try {
                List<Inbox> todos = inboxManager.obtenerTodosMensajes(usuarioActual.getUsername());
                int count = 0;
                for (Inbox msg : todos) {
                    if (msg.getEmisor().equalsIgnoreCase(otroUsername) &&
                        msg.getReceptor().equalsIgnoreCase(usuarioActual.getUsername()) &&
                        !msg.isLeido()) count++;
                }
                final int c = count;
                SwingUtilities.invokeLater(() -> {
                    if (c > 0) {
                        badge.setText("● " + c + " mensaje(s) nuevo(s)");
                        lblUser.setFont(new Font("SansSerif", Font.BOLD, 13));
                    } else {
                        badge.setText("");
                        lblUser.setFont(FONT_BOLD);
                    }
                });
            } catch (IOException ex) {}
        };
        actualizarBadge.run();

        fila.add(info, BorderLayout.CENTER);
        fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fila.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                abrirConversacion(otroUsername);
                // Marcar como leídos y actualizar badge inmediatamente
                try { inboxManager.marcarLeidos(usuarioActual.getUsername(), otroUsername); }
                catch (IOException ex) {}
                actualizarBadge.run();
            }
        });

        return fila;
    }

    private JPanel construirPerfil(String username, boolean esMiPerfil) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        try {
            User u = userManager.buscarporUsername(username);
            if (u == null) { panel.add(grayLabel("Usuario no encontrado.")); return panel; }

            List<String> followers = followManager.obtenerFollowers(username);
            List<String> following = followManager.obtenerFollowing(username);
            List<Publicacion> pubs = obtenerPublicacionesVisibles(u);

            JPanel headerPerfil = new JPanel();
            headerPerfil.setLayout(new BoxLayout(headerPerfil, BoxLayout.Y_AXIS));
            headerPerfil.setBackground(BLANCO);
            headerPerfil.setBorder(new EmptyBorder(12, 14, 6, 14));

            JPanel filaUser = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            filaUser.setBackground(BLANCO);
            String lockIcon = u.getTipoCuenta() == UserManager.TipoCuenta.PRIVADO ? "🔒 " : "";
            JLabel lblUsername = new JLabel(lockIcon + u.getUsername());
            lblUsername.setFont(new Font("SansSerif", Font.BOLD, 16));
            filaUser.add(lblUsername);
            headerPerfil.add(filaUser);
            headerPerfil.add(Box.createVerticalStrut(14));

            JPanel filaTop = new JPanel(new BorderLayout(0, 0));
            filaTop.setBackground(BLANCO);
            filaTop.setMaximumSize(new Dimension(390, 90));

            JPanel avatarWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            avatarWrap.setBackground(BLANCO);
            avatarWrap.add(avatarCircular(u.getFotoPerfil(), 80));
            filaTop.add(avatarWrap, BorderLayout.WEST);

            JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
            stats.setBackground(BLANCO);
            stats.add(statBox(String.valueOf(pubs.size()), "Publicaciones"));

            JPanel boxFollowers = statBox(String.valueOf(followers.size()), "Seguidores");
            boxFollowers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boxFollowers.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    mostrarListaUsuarios("Seguidores de @" + username, followers, panel);
                }
            });
            stats.add(boxFollowers);

            JPanel boxFollowing = statBox(String.valueOf(following.size()), "Seguidos");
            boxFollowing.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boxFollowing.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    mostrarListaUsuarios("Seguidos por @" + username, following, panel);
                }
            });
            stats.add(boxFollowing);
            JPanel statsWrap = new JPanel(new BorderLayout());
            statsWrap.setBackground(BLANCO);
            statsWrap.setBorder(new EmptyBorder(10, 10, 0, 0));
            statsWrap.add(stats, BorderLayout.CENTER);
            filaTop.add(statsWrap, BorderLayout.CENTER);
            headerPerfil.add(filaTop);
            headerPerfil.add(Box.createVerticalStrut(10));

            JLabel lblNombre = new JLabel(u.getNombreCompleto());
            lblNombre.setFont(FONT_BOLD);
            headerPerfil.add(lblNombre);
            headerPerfil.add(Box.createVerticalStrut(10));

            JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            botonesPanel.setBackground(BLANCO);

            if (esMiPerfil) {
                JPanel btnGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
                btnGrid.setBackground(BLANCO);
                btnGrid.setMaximumSize(new Dimension(362, 36));
                btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

                JButton btnEditar = perfilBtn("Editar perfil");
                btnEditar.addActionListener(e -> mostrarVista("CONFIG"));

                JButton btnCompartir = perfilBtn("Compartir perfil");
                btnCompartir.addActionListener(e ->
                    JOptionPane.showMessageDialog(this,
                        "Comparte tu perfil: @" + u.getUsername(),
                        "Compartir", JOptionPane.INFORMATION_MESSAGE));

                btnGrid.add(btnEditar);
                btnGrid.add(btnCompartir);
                botonesPanel.add(btnGrid);
            } else {
                boolean yaSigo = followManager.yaSigo(usuarioActual.getUsername(), username);

                JPanel btnGrid2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
                btnGrid2.setBackground(BLANCO);
                btnGrid2.setMaximumSize(new Dimension(362, 36));
                btnGrid2.setAlignmentX(Component.LEFT_ALIGNMENT);

                JButton btnFollow;
                if (yaSigo) {
                    btnFollow = perfilBtn("Siguiendo ▾");
                } else {
                    btnFollow = new JButton("Seguir") {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                                RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(AZUL_IG);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                            g2.dispose();
                            super.paintComponent(g);
                        }
                    };
                    btnFollow.setFont(new Font("SansSerif", Font.BOLD, 12));
                    btnFollow.setBackground(AZUL_IG);
                    btnFollow.setForeground(BLANCO);
                    btnFollow.setOpaque(false);
                    btnFollow.setContentAreaFilled(false);
                    btnFollow.setBorderPainted(false);
                    btnFollow.setFocusPainted(false);
                    btnFollow.setPreferredSize(new Dimension(158, 30));
                    btnFollow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                btnFollow.addActionListener(e -> {
                    try {
                        if (followManager.yaSigo(usuarioActual.getUsername(), username)) {
                            followManager.dejarDeSeguir(usuarioActual.getUsername(), username);
                        } else {
                            followManager.seguir(usuarioActual.getUsername(), username);
                        }
                        abrirPerfil(username);
                    } catch (IOException ex) { showError(ex.getMessage()); }
                });

                if (u.getTipoCuenta() == UserManager.TipoCuenta.PUBLICO || yaSigo) {
                    JButton btnMensaje = perfilBtn("Mensaje");
                    btnMensaje.setPreferredSize(new Dimension(158, 30));
                    btnMensaje.addActionListener(e -> abrirConversacion(username));
                    btnGrid2.add(btnFollow);
                    btnGrid2.add(btnMensaje);
                } else {

                    btnFollow.setPreferredSize(new Dimension(330, 34));
                    btnGrid2.add(btnFollow);
                }
                botonesPanel.add(btnGrid2);
            }

            headerPerfil.add(botonesPanel);

            int CELDA = 122;
            JPanel gridPanel = new JPanel(new GridLayout(0, 3, 1, 1));
            gridPanel.setBackground(new Color(230, 230, 230));
            gridPanel.setBorder(null);

            boolean noSigo = !esMiPerfil
                && u.getTipoCuenta() == UserManager.TipoCuenta.PRIVADO
                && !followManager.yaSigo(usuarioActual.getUsername(), username);

            if (pubs.isEmpty()) {
                gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.Y_AXIS));
                gridPanel.setBackground(BLANCO);
                if (noSigo) {

                    JPanel lockMsg = new JPanel();
                    lockMsg.setLayout(new BoxLayout(lockMsg, BoxLayout.Y_AXIS));
                    lockMsg.setBackground(BLANCO);
                    lockMsg.setBorder(new EmptyBorder(30, 20, 20, 20));
                    lockMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
                    JLabel lockIco = new JLabel("🔒", SwingConstants.CENTER);
                    lockIco.setFont(new Font("SansSerif", Font.PLAIN, 40));
                    lockIco.setAlignmentX(Component.CENTER_ALIGNMENT);
                    JLabel lockTit = new JLabel("Esta cuenta es privada", SwingConstants.CENTER);
                    lockTit.setFont(FONT_BOLD);
                    lockTit.setAlignmentX(Component.CENTER_ALIGNMENT);
                    JLabel lockSub = new JLabel("Sigue esta cuenta para ver sus fotos.", SwingConstants.CENTER);
                    lockSub.setFont(FONT_SMALL);
                    lockSub.setForeground(Color.GRAY);
                    lockSub.setAlignmentX(Component.CENTER_ALIGNMENT);
                    lockMsg.add(lockIco);
                    lockMsg.add(Box.createVerticalStrut(10));
                    lockMsg.add(lockTit);
                    lockMsg.add(Box.createVerticalStrut(4));
                    lockMsg.add(lockSub);
                    gridPanel.add(lockMsg);
                } else {
                    gridPanel.add(grayLabel(esMiPerfil ? "Aún no has publicado nada." : "Sin publicaciones."));
                }
            } else {
                for (Publicacion p : pubs) {
                    JPanel miniatura = new JPanel(new BorderLayout());
                    miniatura.setBackground(Color.LIGHT_GRAY);
                    miniatura.setPreferredSize(new Dimension(CELDA, CELDA));
                    miniatura.setMinimumSize(new Dimension(CELDA, CELDA));
                    miniatura.setMaximumSize(new Dimension(CELDA, CELDA));

                    if (!p.getRutaImagen().isEmpty() && new File(p.getRutaImagen()).exists()) {

                        ImageIcon raw = new ImageIcon(p.getRutaImagen());
                        Image scaled = raw.getImage()
                            .getScaledInstance(CELDA, CELDA, Image.SCALE_SMOOTH);
                        JLabel imgLbl = new JLabel(new ImageIcon(scaled));
                        imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
                        miniatura.add(imgLbl, BorderLayout.CENTER);
                    } else {
                        JLabel icono = new JLabel("□", SwingConstants.CENTER);
                        icono.setFont(new Font("SansSerif", Font.PLAIN, 36));
                        icono.setForeground(Color.LIGHT_GRAY);
                        miniatura.add(icono, BorderLayout.CENTER);
                    }

                    miniatura.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    Publicacion pFinal = p;
                    miniatura.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) { verPublicacion(pFinal); }
                    });
                    gridPanel.add(miniatura);
                }
            }

            JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            tabBar.setBackground(BLANCO);
            tabBar.setBorder(new EmptyBorder(4, 0, 0, 0));
            JLabel lblGrid = new JLabel("⊞  Publicaciones");
            lblGrid.setFont(FONT_SMALL);
            lblGrid.setForeground(NEGRO);
            tabBar.add(lblGrid);
            headerPerfil.add(tabBar);

            JPanel gridWrapper = new JPanel(new BorderLayout());
            gridWrapper.setBackground(BLANCO);
            gridWrapper.add(gridPanel, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(gridWrapper);
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
            panel.add(headerPerfil, BorderLayout.NORTH);
            panel.add(scroll, BorderLayout.CENTER);

        } catch (IOException e) { showError(e.getMessage()); }
        return panel;
    }

    private JPanel construirPost() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLANCO);
        header.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        header.setPreferredSize(new Dimension(390, 44));
        JLabel lblTitulo = new JLabel("  Nueva publicación");
        lblTitulo.setFont(FONT_BOLD);
        header.add(lblTitulo, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        form.setBackground(BLANCO);

        JTextArea txtContenido = new JTextArea(5, 20);
        txtContenido.setFont(FONT_NORM);
        txtContenido.setLineWrap(true);
        txtContenido.setWrapStyleWord(true);
        txtContenido.setBorder(new LineBorder(GRIS_BORDE, 1));
        JScrollPane scrollTxt = new JScrollPane(txtContenido);
        scrollTxt.setMaximumSize(new Dimension(350, 130));
        scrollTxt.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCounter = new JLabel("0 / 220");
        lblCounter.setFont(FONT_SMALL);
        lblCounter.setForeground(Color.GRAY);
        lblCounter.setAlignmentX(Component.RIGHT_ALIGNMENT);

        txtContenido.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                int len = txtContenido.getText().length();
                lblCounter.setText(len + " / 220");
                lblCounter.setForeground(len > 220 ? Color.RED : Color.GRAY);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        final File[] archivoImagen = { null };
        JPanel fotoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fotoPanel.setBackground(BLANCO);
        fotoPanel.setMaximumSize(new Dimension(350, 36));
        fotoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblFotoNombre = new JLabel("Sin imagen elegida");
        lblFotoNombre.setFont(FONT_SMALL);
        lblFotoNombre.setForeground(Color.GRAY);

        JButton btnElegirFoto = igButton("🖼 Agregar foto", GRIS_BORDE, NEGRO);
        btnElegirFoto.setMaximumSize(new Dimension(160, 32));
        btnElegirFoto.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Elegir imagen");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archivoImagen[0] = chooser.getSelectedFile();
                lblFotoNombre.setText(archivoImagen[0].getName());
                lblFotoNombre.setForeground(NEGRO);
            }
        });
        fotoPanel.add(btnElegirFoto);
        fotoPanel.add(Box.createHorizontalStrut(8));
        fotoPanel.add(lblFotoNombre);

        JButton btnPublicar = igButton("Publicar", AZUL_IG, BLANCO);
        btnPublicar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPublicar.addActionListener(e -> {
            String contenido = txtContenido.getText().trim();
            if (contenido.isEmpty()) { showError("El contenido no puede estar vacío."); return; }
            if (contenido.length() > 220) { showError("Máximo 220 caracteres."); return; }
            try {
                String rutaImagen = archivoImagen[0] != null
                    ? archivoImagen[0].getAbsolutePath() : "";

                String tipo = "NINGUNA";
                if (archivoImagen[0] != null) tipo = "CUADRADA";
                pubManager.agregarPublicacion(
                    usuarioActual.getUsername(), contenido, rutaImagen, tipo);

                txtContenido.setText("");
                archivoImagen[0] = null;
                lblFotoNombre.setText("Sin imagen elegida");
                lblFotoNombre.setForeground(Color.GRAY);
                mostrarVista("FEED");
                JOptionPane.showMessageDialog(MainApp.this, "¡Publicación creada!",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        form.add(new JLabel("Contenido (usa #hashtags y @menciones):"));
        form.add(Box.createVerticalStrut(6));
        form.add(scrollTxt);
        form.add(lblCounter);
        form.add(Box.createVerticalStrut(14));
        form.add(fotoPanel);
        form.add(Box.createVerticalStrut(20));
        form.add(btnPublicar);

        panel.add(new JScrollPane(form), BorderLayout.CENTER);
        return panel;
    }

    private void dialogNuevaMensaje() {
        JDialog dialog = new JDialog(this, "Nuevo mensaje", true);
        dialog.setSize(340, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        form.setBackground(BLANCO);
        JTextField txtDest = styledTextField("Usuario destinatario");
        JButton btnAbrir = igButton("Abrir conversación", AZUL_IG, BLANCO);
        btnAbrir.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAbrir.addActionListener(e -> {
            String dest = txtDest.getText().trim();
            if (dest.isEmpty()) return;
            dialog.dispose();
            abrirConversacion(dest);
        });
        form.add(new JLabel("¿Con quién quieres hablar?"));
        form.add(Box.createVerticalStrut(10));
        form.add(txtDest);
        form.add(Box.createVerticalStrut(14));
        form.add(btnAbrir);
        dialog.add(form);
        dialog.setVisible(true);
    }

    private void abrirConversacion(String otroUsername) {
        try {
            User otro = userManager.buscarporUsername(otroUsername);
            if (otro == null) { showError("Usuario no encontrado."); return; }

            if (!otro.isEstadoCuenta()) {
                showError("Este usuario ha desactivado su cuenta y no puede recibir mensajes."); return;
            }
            boolean puedeMensajear;
            if (otro.getTipoCuenta() == UserManager.TipoCuenta.PUBLICO) {
                puedeMensajear = true;
            } else {
                boolean yoSigo = followManager.yaSigo(usuarioActual.getUsername(), otroUsername);
                puedeMensajear = yoSigo;
            }
            if (!puedeMensajear) {
                showError("Este perfil es privado. Solo puedes enviarle mensajes si lo sigues."); return;
            }
            inboxManager.marcarLeidos(usuarioActual.getUsername(), otroUsername);
            mensajeServer.enviarLeido(usuarioActual.getUsername(), otroUsername);

        } catch (IOException e) { showError(e.getMessage()); return; }
        homeCenter.remove(getComponentByName("INBOX"));
        homeCenter.add(construirInbox(), "INBOX");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout(0, 0));
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 56));

        JButton btnVolver = new JButton("←");
        btnVolver.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnVolver.setForeground(NEGRO);
        btnVolver.setBorderPainted(false); btnVolver.setContentAreaFilled(false);
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            conversacionAbieraCon      = null;
            recargarConversacionActual = null;
            homeCenter.remove(wrapper);
            homeCenter.revalidate();
            homeCard.show(homeCenter, "INBOX");
        });

        JPanel centro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        centro.setBackground(BLANCO);
        try {
            User otroUser = userManager.buscarporUsername(otroUsername);
            centro.add(avatarCircular(otroUser != null ? otroUser.getFotoPerfil() : null, 36));
        } catch (IOException ex) { centro.add(avatarCircular(null, 36)); }
        JPanel nombreCol = new JPanel();
        nombreCol.setLayout(new BoxLayout(nombreCol, BoxLayout.Y_AXIS));
        nombreCol.setBackground(BLANCO);
        JLabel lblNombreConv = new JLabel(otroUsername);
        lblNombreConv.setFont(FONT_BOLD);
        nombreCol.add(lblNombreConv);
        centro.add(nombreCol);

        hdr.add(btnVolver, BorderLayout.WEST);
        hdr.add(centro,    BorderLayout.CENTER);
        wrapper.add(hdr, BorderLayout.NORTH);

        JPanel mensajesPanel = new JPanel();
        mensajesPanel.setLayout(new BoxLayout(mensajesPanel, BoxLayout.Y_AXIS));
        mensajesPanel.setBackground(BLANCO);

        Runnable cargarMensajes = () -> {
            mensajesPanel.removeAll();
            try {
                List<Inbox> conv = inboxManager.obtenerConversacion(
                    usuarioActual.getUsername(), otroUsername);
                if (conv.isEmpty()) {
                    mensajesPanel.add(Box.createVerticalStrut(20));
                    mensajesPanel.add(grayLabel("Sin mensajes aún."));
                } else {
                    for (Inbox m : conv) {
                        boolean soyYo = m.getEmisor().equalsIgnoreCase(usuarioActual.getUsername());
                        mensajesPanel.add(burbujaMensaje(m, soyYo));
                    }
                }
            } catch (IOException e) { showError(e.getMessage()); }
            mensajesPanel.revalidate();
            mensajesPanel.repaint();
        };

        conversacionAbieraCon      = otroUsername;
        recargarConversacionActual = cargarMensajes;
        cargarMensajes.run();

        JScrollPane scrollMensajes = new JScrollPane(mensajesPanel);
        scrollMensajes.setBorder(null);
        scrollMensajes.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMensajes.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollMensajes.getVerticalScrollBar().setUnitIncrement(16);

        mensajesPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() ->
                    scrollMensajes.getVerticalScrollBar().setValue(
                        scrollMensajes.getVerticalScrollBar().getMaximum()));
            }
        });
        wrapper.add(scrollMensajes, BorderLayout.CENTER);

        JPanel stickerPanel = new JPanel(new BorderLayout());
        stickerPanel.setBackground(BLANCO);
        stickerPanel.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        stickerPanel.setPreferredSize(new Dimension(390, 220));
        stickerPanel.setVisible(false);

        JPanel stickerGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        stickerGrid.setBackground(BLANCO);
        stickerGrid.setBorder(new EmptyBorder(8, 8, 8, 8));

        Runnable cargarStickers = () -> {
            stickerGrid.removeAll();
            try {
                List<String> stickers = stickerManager.obtenerTodos(usuarioActual.getUsername());
                for (String nombre : stickers) {
                    JButton btnS = new JButton();
                    btnS.setBorderPainted(false);
                    btnS.setContentAreaFilled(false);
                    btnS.setFocusPainted(false);
                    btnS.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    btnS.setPreferredSize(new Dimension(72, 72));

                    File imgS = null;
                    for (String ext : new String[]{".png",".jpg",".jpeg",".gif"}) {
                        File f = new File(ASSETS + nombre + ext);
                        if (f.exists()) { imgS = f; break; }
                    }
                    if (imgS != null) {
                        ImageIcon raw = new ImageIcon(imgS.getAbsolutePath());
                        Image scaled = raw.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                        btnS.setIcon(new ImageIcon(scaled));
                    } else {
                        btnS.setText(nombre);
                        btnS.setFont(FONT_SMALL);
                    }
                    final String sNombre = nombre;
                    btnS.addActionListener(ev -> {
                        try {
                            inboxManager.enviarMensaje(usuarioActual.getUsername(), otroUsername, sNombre, "STICKER");
                            mensajeServer.enviarNotificacion(usuarioActual.getUsername(), otroUsername, sNombre, "STICKER");
                            stickerPanel.setVisible(false);
                            cargarMensajes.run();
                            SwingUtilities.invokeLater(() ->
                                scrollMensajes.getVerticalScrollBar().setValue(
                                    scrollMensajes.getVerticalScrollBar().getMaximum()));
                        } catch (IOException ex) { showError(ex.getMessage()); }
                    });
                    stickerGrid.add(btnS);
                }
            } catch (IOException ex) {}
            stickerGrid.revalidate(); stickerGrid.repaint();
        };
        cargarStickers.run();

        JScrollPane scrollStickers = new JScrollPane(stickerGrid);
        scrollStickers.setBorder(null);
        scrollStickers.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollStickers.getVerticalScrollBar().setPreferredSize(new Dimension(0,0));
        stickerPanel.add(scrollStickers, BorderLayout.CENTER);

        JPanel inputBar = new JPanel(new BorderLayout(6, 0));
        inputBar.setBackground(BLANCO);
        inputBar.setBorder(new EmptyBorder(8, 10, 10, 10));

        JButton btnFoto = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AZUL_IG);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnFoto.setPreferredSize(new Dimension(34, 34));
        btnFoto.setBorderPainted(false); btnFoto.setContentAreaFilled(false);
        btnFoto.setFocusPainted(false); btnFoto.setOpaque(false);
        ImageIcon icoSend = cargarIcono("icon_chat_send.png", 18);
        if (icoSend != null) { btnFoto.setIcon(icoSend); }
        else { btnFoto.setText("✈"); btnFoto.setFont(new Font("SansSerif", Font.BOLD, 14)); }
        btnFoto.setForeground(BLANCO);
        btnFoto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFoto.addActionListener(e -> {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Importar sticker");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes", "png", "jpg", "jpeg", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File archivo = chooser.getSelectedFile();
                String nombreArchivo = archivo.getName();
                int dot = nombreArchivo.lastIndexOf('.');
                String nombre = dot >= 0 ? nombreArchivo.substring(0, dot) : nombreArchivo;
                try {

                    File destAssets = new File(ASSETS + nombreArchivo);
                    if (!destAssets.exists()) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo);
                             java.io.FileOutputStream fos = new java.io.FileOutputStream(destAssets)) {
                            byte[] buf = new byte[4096];
                            int n;
                            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
                        }
                    }
                    stickerManager.importarSticker(usuarioActual.getUsername(), nombre);
                    JOptionPane.showMessageDialog(this,
                        "Sticker \"" + nombre + "\" importado.", "Listo",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { showError(ex.getMessage()); }
            }
        });
        inputBar.add(btnFoto, BorderLayout.WEST);

        JTextField txtMsg = new JTextField();
        txtMsg.setFont(FONT_NORM);
        txtMsg.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220,220,220), 1, true),
            new EmptyBorder(8, 14, 8, 14)));
        txtMsg.setBackground(new Color(247, 247, 247));

        JPanel derInput = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        derInput.setBackground(BLANCO);

        JButton btnImagen = crearBtnChat("icon_chat_image.png", "🖼", 22);
        btnImagen.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Seleccionar imagen");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes", "png", "jpg", "jpeg", "gif"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File imgFile = chooser.getSelectedFile();
                try {
                    File destino = new File(ASSETS + imgFile.getName());
                    if (!destino.exists()) {
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(imgFile);
                             java.io.FileOutputStream fos = new java.io.FileOutputStream(destino)) {
                            byte[] buf = new byte[4096];
                            int n;
                            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
                        }
                    }
                    String nombreImg = imgFile.getName();
                    inboxManager.enviarMensaje(usuarioActual.getUsername(), otroUsername,
                        nombreImg, "IMAGEN");
                    mensajeServer.enviarNotificacion(usuarioActual.getUsername(), otroUsername,
                        nombreImg, "IMAGEN");
                    cargarMensajes.run();
                    SwingUtilities.invokeLater(() ->
                        scrollMensajes.getVerticalScrollBar().setValue(
                            scrollMensajes.getVerticalScrollBar().getMaximum()));
                } catch (Exception ex) { showError(ex.getMessage()); }
            }
        });

        JButton btnStickerToggle = crearBtnChat("icon_chat_sticker.png", "😊", 22);
        btnStickerToggle.addActionListener(e -> {
            if (!stickerPanel.isVisible()) {
                cargarStickers.run();
            }
            stickerPanel.setVisible(!stickerPanel.isVisible());
        });

        JButton btnMas = crearBtnChat("icon_chat_camera.png", "📷", 22);

        JButton btnEnviar = new JButton("Enviar");
        btnEnviar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnEnviar.setForeground(AZUL_IG);
        btnEnviar.setBorderPainted(false); btnEnviar.setContentAreaFilled(false);
        btnEnviar.setFocusPainted(false);
        btnEnviar.setVisible(false);
        btnEnviar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        derInput.add(btnImagen);
        derInput.add(btnStickerToggle);
        derInput.add(btnMas);
        derInput.add(btnEnviar);

        inputBar.add(txtMsg, BorderLayout.CENTER);
        inputBar.add(derInput, BorderLayout.EAST);

        ActionListener enviarTexto = e -> {
            String contenido = txtMsg.getText().trim();
            if (contenido.isEmpty()) return;
            try {
                inboxManager.enviarMensaje(usuarioActual.getUsername(), otroUsername, contenido, "TEXTO");
                mensajeServer.enviarNotificacion(usuarioActual.getUsername(), otroUsername, contenido, "TEXTO");
                txtMsg.setText("");
                stickerPanel.setVisible(false);
                cargarMensajes.run();
                SwingUtilities.invokeLater(() ->
                    scrollMensajes.getVerticalScrollBar().setValue(
                        scrollMensajes.getVerticalScrollBar().getMaximum()));
            } catch (IOException ex) { showError(ex.getMessage()); }
        };
        txtMsg.addActionListener(enviarTexto);
        btnEnviar.addActionListener(enviarTexto);

        txtMsg.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                boolean hayTexto = !txtMsg.getText().trim().isEmpty();
                btnImagen.setVisible(!hayTexto);
                btnStickerToggle.setVisible(!hayTexto);
                btnMas.setVisible(!hayTexto);
                btnEnviar.setVisible(hayTexto);
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        JButton btnEliminar = new JButton("Eliminar conversación");
        btnEliminar.setFont(FONT_SMALL);
        btnEliminar.setForeground(Color.RED);
        btnEliminar.setBorderPainted(false);
        btnEliminar.setContentAreaFilled(false);
        btnEliminar.addActionListener(e -> {
            int conf = JOptionPane.showConfirmDialog(this,
                "¿Eliminar conversación?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                try {
                    inboxManager.eliminarConversacion(usuarioActual.getUsername(), otroUsername);
                    conversacionAbieraCon      = null;
                    recargarConversacionActual = null;
                    homeCenter.remove(wrapper);
                    mostrarVista("INBOX");
                } catch (IOException ex) { showError(ex.getMessage()); }
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BLANCO);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        bottom.add(stickerPanel, BorderLayout.NORTH);
        bottom.add(inputBar, BorderLayout.CENTER);
        bottom.add(btnEliminar, BorderLayout.SOUTH);
        wrapper.add(bottom, BorderLayout.SOUTH);

        String convKey = "CONV_" + otroUsername.toLowerCase();

        for (Component comp : homeCenter.getComponents()) {
            if (comp instanceof JPanel) {
                Object name = ((JPanel)comp).getClientProperty("convKey");
                if (convKey.equals(name)) { homeCenter.remove(comp); break; }
            }
        }
        wrapper.putClientProperty("convKey", convKey);
        homeCenter.add(wrapper, convKey);
        homeCard.show(homeCenter, convKey);
    }

    private JPanel burbujaMensaje(Inbox m, boolean soyYo) {
        JPanel fila = new JPanel(new BorderLayout(0, 1));
        fila.setBackground(BLANCO);
        fila.setBorder(new EmptyBorder(2, 10, 2, 10));

        JPanel content = new JPanel(new FlowLayout(soyYo ? FlowLayout.RIGHT : FlowLayout.LEFT, 8, 3));
        content.setBackground(BLANCO);

        if (!soyYo) {
            try {
                User emisorUser = userManager.buscarporUsername(m.getEmisor());
                content.add(avatarCircular(
                    emisorUser != null ? emisorUser.getFotoPerfil() : null, 26));
            } catch (IOException ex) {
                content.add(avatarCircular(null, 26));
            }
        }

        if (m.getTipo().equals("IMAGEN")) {
            File imgFile = new File(ASSETS + m.getContenido());
            if (imgFile.exists()) {
                ImageIcon raw = new ImageIcon(imgFile.getAbsolutePath());
                int w = raw.getIconWidth(), h = raw.getIconHeight();
                int maxW = 220;
                int targetW = Math.min(w, maxW);
                int targetH = w > 0 ? (int)((double)h / w * targetW) : targetW;
                Image scaled = raw.getImage().getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                JLabel imgLbl = new JLabel(new ImageIcon(scaled));
                imgLbl.setBorder(new EmptyBorder(4, 4, 4, 4));
                content.add(imgLbl);
            } else {
                JLabel lbl = new JLabel("🖼 " + m.getContenido());
                lbl.setFont(FONT_NORM);
                content.add(lbl);
            }
        } else if (m.getTipo().equals("STICKER")) {
            String nombre = m.getContenido();
            File imgSticker = null;
            for (String ext : new String[]{".png", ".jpg", ".jpeg", ".gif"}) {
                File f = new File(ASSETS + nombre + ext);
                if (f.exists()) { imgSticker = f; break; }
            }
            if (imgSticker != null) {
                ImageIcon raw = new ImageIcon(imgSticker.getAbsolutePath());
                Image scaled = raw.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                content.add(new JLabel(new ImageIcon(scaled)));
            } else {
                JLabel lbl = new JLabel("🎭 " + nombre);
                lbl.setFont(FONT_NORM);
                content.add(lbl);
            }
        } else {
            final Color bgColor = soyYo ? AZUL_IG : new Color(240, 240, 240);
            final Color fgColor = soyYo ? BLANCO : NEGRO;
            final String texto  = m.getContenido();

            JPanel burbuja = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                    g2.dispose();
                }
            };
            burbuja.setOpaque(false);
            burbuja.setBorder(new EmptyBorder(8, 14, 8, 14));

            JLabel lblMsg = new JLabel("<html><div style='max-width:200px;'>"
                + texto + "</div></html>");
            lblMsg.setFont(FONT_NORM);
            lblMsg.setForeground(fgColor);
            burbuja.add(lblMsg);

            JPanel wrap = new JPanel(new FlowLayout(soyYo ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
            wrap.setBackground(BLANCO);
            wrap.setMaximumSize(new Dimension(290, 999));
            wrap.add(burbuja);
            content.add(wrap);
        }
        fila.add(content, BorderLayout.CENTER);

        // Indicador de leído debajo del mensaje (solo en mensajes propios)
        if (soyYo) {
            JPanel estadoRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            estadoRow.setBackground(BLANCO);
            JLabel lblEstado = new JLabel(m.isLeido() ? "✓✓ Visto" : "✓ Enviado");
            lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 9));
            lblEstado.setForeground(m.isLeido() ? AZUL_IG : Color.GRAY);
            estadoRow.add(lblEstado);
            fila.add(estadoRow, m.getTipo().equals("IMAGEN") || m.getTipo().equals("STICKER")
                ? BorderLayout.SOUTH : BorderLayout.SOUTH);
        }

        return fila;
    }

    private void abrirPerfil(String username) {
        boolean esMio = username.equalsIgnoreCase(usuarioActual.getUsername());
        if (esMio) {
            homeCenter.remove(getComponentByName("PERFIL"));
            homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
            homeCard.show(homeCenter, "PERFIL");
            return;
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        JPanel headerAjeno = new JPanel(new BorderLayout());
        headerAjeno.setBackground(BLANCO);
        headerAjeno.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        headerAjeno.setPreferredSize(new Dimension(390, 44));

        JButton btnVolver = new JButton("←  Volver");
        btnVolver.setFont(FONT_BOLD);
        btnVolver.setForeground(AZUL_IG);
        btnVolver.setBorderPainted(false);
        btnVolver.setContentAreaFilled(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            homeCenter.remove(wrapper);
            homeCard.show(homeCenter, vistaAnterior);
        });

        JLabel lblUsername = new JLabel("@" + username, SwingConstants.CENTER);
        lblUsername.setFont(FONT_BOLD);

        headerAjeno.add(btnVolver,    BorderLayout.WEST);
        headerAjeno.add(lblUsername,  BorderLayout.CENTER);

        wrapper.add(headerAjeno,                      BorderLayout.NORTH);
        wrapper.add(construirPerfil(username, false),  BorderLayout.CENTER);

        homeCenter.add(wrapper, "PERFIL_AJENO");
        homeCard.show(homeCenter, "PERFIL_AJENO");
    }

    private void verPublicacion(Publicacion p) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 44));
        JButton btnV = new JButton("←  Volver");
        btnV.setFont(FONT_BOLD); btnV.setForeground(AZUL_IG);
        btnV.setBorderPainted(false); btnV.setContentAreaFilled(false);
        btnV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnV.addActionListener(e -> { homeCenter.remove(wrapper); homeCard.show(homeCenter, vistaAnterior); });
        JLabel lblTit = new JLabel("Publicación", SwingConstants.CENTER);
        lblTit.setFont(FONT_BOLD);
        hdr.add(btnV, BorderLayout.WEST);
        hdr.add(lblTit, BorderLayout.CENTER);
        wrapper.add(hdr, BorderLayout.NORTH);

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setBackground(BLANCO);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0; gbc2.gridy = 0;
        gbc2.weightx = 1.0; gbc2.weighty = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.NORTH;
        contenido.add(construirTarjetaPublicacion(p), gbc2);

        JScrollPane scrollVer = new JScrollPane(contenido);
        scrollVer.setBorder(null);
        scrollVer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollVer.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollVer.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scrollVer, BorderLayout.CENTER);

        homeCenter.add(wrapper, "VER_PUB");
        homeCard.show(homeCenter, "VER_PUB");
    }

    private JPanel construirPantallaReactivar() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);

        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setBackground(BLANCO);
        caja.setBorder(new EmptyBorder(40, 40, 40, 40));
        caja.setPreferredSize(new Dimension(310, 420));

        JLabel logo = new JLabel("Instagram");
        logo.setFont(FONT_LOGO);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Reactivar cuenta");
        lblSub.setFont(FONT_BOLD);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSub.setForeground(Color.GRAY);

        JTextField txtUser = styledTextField("Nombre de usuario");
        JPasswordField txtPass = new JPasswordField();
        styleTextField(txtPass, "Contraseña");

        JLabel lblError = new JLabel(" ");
        lblError.setForeground(Color.RED);
        lblError.setFont(FONT_SMALL);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnReact = igButton("Reactivar cuenta", AZUL_IG, BLANCO);
        btnReact.addActionListener(e -> {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            try {
                User u = userManager.buscarporUsername(user);
                if (u == null) { lblError.setText("Usuario no encontrado."); return; }
                if (!u.getPassword().equals(pass)) { lblError.setText("Contraseña incorrecta."); return; }
                u.setEstadoCuenta(true);
                userManager.actualizarUsuario(u);
                lblError.setForeground(new Color(0,150,0));
                lblError.setText("Cuenta reactivada. Ya puedes iniciar sesión.");
                txtUser.setText(""); txtPass.setText("");
            } catch (IOException ex) { lblError.setText(ex.getMessage()); }
        });

        JButton btnVolver = new JButton("← Volver al inicio de sesión");
        btnVolver.setFont(FONT_SMALL);
        btnVolver.setForeground(AZUL_IG);
        btnVolver.setBorderPainted(false); btnVolver.setContentAreaFilled(false);
        btnVolver.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        caja.add(logo);
        caja.add(Box.createVerticalStrut(8));
        caja.add(lblSub);
        caja.add(Box.createVerticalStrut(24));
        caja.add(txtUser);
        caja.add(Box.createVerticalStrut(10));
        caja.add(txtPass);
        caja.add(Box.createVerticalStrut(6));
        caja.add(lblError);
        caja.add(Box.createVerticalStrut(10));
        caja.add(btnReact);
        caja.add(Box.createVerticalStrut(16));
        caja.add(btnVolver);

        panel.add(caja);
        return panel;
    }

    private List<Publicacion> obtenerPublicacionesVisibles(User objetivo) throws IOException {
        if (objetivo.getTipoCuenta() == UserManager.TipoCuenta.PUBLICO) {
            return pubManager.obtenerPublicaciones(objetivo.getUsername());
        }
        if (usuarioActual != null) {
            if (objetivo.getUsername().equalsIgnoreCase(usuarioActual.getUsername())) {
                return pubManager.obtenerPublicaciones(objetivo.getUsername());
            }
            boolean yoSigo = followManager.yaSigo(usuarioActual.getUsername(), objetivo.getUsername());
            if (yoSigo) {
                return pubManager.obtenerPublicaciones(objetivo.getUsername());
            }
        }
        return new ArrayList<>();
    }

    private JTextField styledTextField(String placeholder) {
        JTextField field = new JTextField();
        styleTextField(field, placeholder);
        return field;
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setFont(FONT_NORM);
        field.setMaximumSize(new Dimension(330, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(GRIS_BORDE, 1, true),
            new EmptyBorder(8, 10, 8, 10)));
        field.setBackground(GRIS_CLARO);
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText(""); field.setForeground(NEGRO);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder); field.setForeground(Color.GRAY);
                }
            }
        });
    }

    private JButton igButton(String texto, Color bg, Color fg) {
        JButton btn = new JButton(texto);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(330, 38));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    private JLabel avatarLabel(String icon) {
        JLabel lbl = new JLabel(icon);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 28));
        lbl.setForeground(GRIS_BORDE);
        return lbl;
    }

    
    private ImageIcon cargarIcono(String nombreArchivo, int size) {
        File f = new File(ASSETS + nombreArchivo);
        if (!f.exists()) return null;
        ImageIcon raw = new ImageIcon(f.getAbsolutePath());
        Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    
    private JLabel avatarCircular(File fotoFile, int size) {
        java.awt.image.BufferedImage circle =
            new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circle.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (fotoFile != null && fotoFile.exists()) {

            ImageIcon icon = new ImageIcon(fotoFile.getAbsolutePath());
            icon.getImage();
            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);

            java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
            mt.addImage(img, 0);
            try { mt.waitForAll(); } catch (InterruptedException ignored) {}
            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(img, 0, 0, null);
        } else {

            g2.setColor(GRIS_CLARO);
            g2.fillOval(0, 0, size, size);
            g2.setColor(GRIS_BORDE);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(1, 1, size - 2, size - 2);
            g2.setColor(GRIS_BORDE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            String txt = "○";
            g2.drawString(txt, (size - fm.stringWidth(txt)) / 2,
                (size + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
        JLabel lbl = new JLabel(new ImageIcon(circle));
        lbl.setPreferredSize(new Dimension(size, size));
        return lbl;
    }

    
    private void mostrarListaUsuarios(String titulo, List<String> usernames, JPanel perfilPanel) {
        JPanel sub = new JPanel(new BorderLayout());
        sub.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 48));
        JButton btnV = new JButton("←");
        btnV.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnV.setForeground(NEGRO);
        btnV.setBorderPainted(false); btnV.setContentAreaFilled(false);
        btnV.setFocusPainted(false);
        btnV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnV.addActionListener(e -> { homeCenter.remove(sub); homeCenter.revalidate(); homeCard.show(homeCenter, vistaAnterior.equals("FEED") ? "PERFIL" : vistaAnterior); });
        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(FONT_BOLD);
        hdr.add(btnV, BorderLayout.WEST);
        hdr.add(lblTit, BorderLayout.CENTER);
        sub.add(hdr, BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(BLANCO);

        if (usernames.isEmpty()) {
            lista.add(Box.createVerticalStrut(30));
            lista.add(grayLabel("No hay usuarios."));
        } else {
            for (String uname : usernames) {
                try {
                    User u = userManager.buscarporUsername(uname);
                    if (u == null) continue;

                    JPanel fila = new JPanel(new BorderLayout(10, 0));
                    fila.setBackground(BLANCO);
                    fila.setBorder(new EmptyBorder(8, 14, 8, 14));
                    fila.setMaximumSize(new Dimension(390, 62));
                    fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    fila.add(avatarCircular(u.getFotoPerfil(), 44), BorderLayout.WEST);

                    JPanel info = new JPanel();
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                    info.setBackground(BLANCO);
                    JLabel lblUser = new JLabel(u.getUsername());
                    lblUser.setFont(FONT_BOLD);
                    JLabel lblNom = new JLabel(u.getNombreCompleto());
                    lblNom.setFont(FONT_SMALL);
                    lblNom.setForeground(Color.GRAY);
                    info.add(lblUser);
                    info.add(lblNom);
                    fila.add(info, BorderLayout.CENTER);

                    boolean yaSigo = followManager.yaSigo(usuarioActual.getUsername(), uname);
                    if (!uname.equalsIgnoreCase(usuarioActual.getUsername())) {
                        JButton btnF = perfilBtn(yaSigo ? "Siguiendo" : "Seguir");
                        if (!yaSigo) {
                            btnF.setBackground(AZUL_IG);
                            btnF.setForeground(BLANCO);
                        }
                        btnF.setPreferredSize(new Dimension(90, 28));
                        btnF.addActionListener(ev -> {
                            try {
                                if (followManager.yaSigo(usuarioActual.getUsername(), uname))
                                    followManager.dejarDeSeguir(usuarioActual.getUsername(), uname);
                                else
                                    followManager.seguir(usuarioActual.getUsername(), uname);
                                homeCenter.remove(sub);
                                homeCenter.revalidate();
                                homeCard.show(homeCenter, vistaAnterior);
                            } catch (IOException ex) { showError(ex.getMessage()); }
                        });
                        fila.add(btnF, BorderLayout.EAST);
                    }

                    fila.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.getSource() == fila) abrirPerfil(uname);
                        }
                    });
                    lista.add(fila);
                    lista.add(new JSeparator());
                } catch (IOException ex) {}
            }
        }

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        sub.add(scroll, BorderLayout.CENTER);

        homeCenter.add(sub, "LISTA_USUARIOS");
        homeCard.show(homeCenter, "LISTA_USUARIOS");
    }

    private JPanel statBox(String numero, String titulo) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(BLANCO);
        JLabel num = new JLabel(numero, SwingConstants.CENTER);
        num.setFont(FONT_BOLD);
        num.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tit = new JLabel(titulo, SwingConstants.CENTER);
        tit.setFont(FONT_SMALL);
        tit.setForeground(Color.GRAY);
        tit.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(num); box.add(tit);
        return box;
    }

    private JPanel imagePlaceholder(String tipo) {
        int alto = tipo.equals("VERTICAL") ? 280 : tipo.equals("HORIZONTAL") ? 200 : 240;
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(GRIS_CLARO);
        p.setMaximumSize(new Dimension(390, alto));
        p.setPreferredSize(new Dimension(390, alto));
        JLabel lbl = new JLabel("🖼  " + tipo, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    private JLabel grayLabel(String texto) {
        JLabel lbl = new JLabel("  " + texto);
        lbl.setFont(FONT_NORM);
        lbl.setForeground(Color.GRAY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    
    private boolean adquirirSesion(String username) {
        File lock = new File("INSTA_RAIZ/" + username.toLowerCase() + ".lock");
        try {
            if (lock.exists()) return false;
            lock.getParentFile().mkdirs();
            lock.createNewFile();
            lock.deleteOnExit();
            return true;
        } catch (IOException e) { return false; }
    }

    
    private void liberarSesion(String username) {
        if (username == null) return;
        new File("INSTA_RAIZ/" + username.toLowerCase() + ".lock").delete();
    }

    
    private JButton chatIconBtn(String emoji, int fontSize) {
        JButton btn = new JButton(emoji);
        btn.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    
    private JButton crearBtnChat(String archivo, String fallbackEmoji, int size) {
        ImageIcon ico = cargarIcono(archivo, size);
        JButton btn = ico != null ? new JButton(ico) : chatIconBtn(fallbackEmoji, size);
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    
    private void actualizarCommCount(JPanel panel, long fecha, int count) {
        for (Component c : panel.getComponents()) {
            Object prop = null;
            if (c instanceof JPanel) {
                prop = ((JPanel)c).getClientProperty("lblCommCount_" + fecha);
                if (prop instanceof JLabel)
                    ((JLabel)prop).setText(count > 0 ? String.valueOf(count) : "");
                actualizarCommCount((JPanel)c, fecha, count);
            }
        }
    }

    private JPanel construirConfiguracion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 48));
        JButton btnV = new JButton("←");
        btnV.setFont(new Font("SansSerif", Font.BOLD, 18));
        btnV.setForeground(NEGRO);
        btnV.setBorderPainted(false); btnV.setContentAreaFilled(false);
        btnV.setFocusPainted(false);
        btnV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnV.addActionListener(e -> mostrarVista("PERFIL"));
        JLabel lblTit = new JLabel("Configuración y actividad", SwingConstants.CENTER);
        lblTit.setFont(FONT_BOLD);
        hdr.add(btnV,   BorderLayout.WEST);
        hdr.add(lblTit, BorderLayout.CENTER);
        panel.add(hdr, BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(BLANCO);
        lista.setBorder(new EmptyBorder(8, 0, 8, 0));

        lista.add(filaConfig("👤", "Estado de la cuenta",
            "Activa o desactiva tu cuenta",
            () -> abrirSubConfig("Estado de la cuenta", new String[]{
                "Desactivar cuenta", "Mantener activa"},
                new Runnable[]{
                    () -> {
                        try {
                            usuarioActual.setEstadoCuenta(false);
                            userManager.actualizarUsuario(usuarioActual);
                            liberarSesion(usuarioActual.getUsername());
                            usuarioActual = null; likesEnSesion.clear();
                            cardLayout.show(mainPanel, "LOGIN");
                        } catch (IOException ex) { showError(ex.getMessage()); }
                    },
                    () -> mostrarVista("CONFIG")
                })));
        lista.add(new JSeparator());

        boolean esPrivado = usuarioActual.getTipoCuenta() == UserManager.TipoCuenta.PRIVADO;
        lista.add(filaConfig("🔒", "Privacidad de la cuenta",
            esPrivado ? "Cuenta privada — toca para hacer pública" : "Cuenta pública — toca para hacer privada",
            () -> abrirSubConfig("Privacidad de la cuenta", new String[]{
                esPrivado ? "Hacer pública" : "Hacer privada", "Cancelar"},
                new Runnable[]{
                    () -> {
                        try {
                            UserManager.TipoCuenta nuevo = esPrivado
                                ? UserManager.TipoCuenta.PUBLICO
                                : UserManager.TipoCuenta.PRIVADO;
                            usuarioActual.setTipoCuenta(nuevo);
                            userManager.actualizarUsuario(usuarioActual);
                            mostrarVista("CONFIG");
                        } catch (IOException ex) { showError(ex.getMessage()); }
                    },
                    () -> mostrarVista("CONFIG")
                })));
        lista.add(new JSeparator());

        lista.add(filaConfig("ℹ", "Información",
            "Versión 1.0 — Proyecto II UNITEC", () -> {}));
        lista.add(new JSeparator());

        JPanel seccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        seccion.setBackground(BLANCO);
        JLabel lblSec = new JLabel("Inicio de sesión");
        lblSec.setFont(FONT_SMALL);
        lblSec.setForeground(Color.GRAY);
        seccion.add(lblSec);
        lista.add(seccion);

        JButton btnCerrar = new JButton("Cerrar sesión");
        btnCerrar.setFont(FONT_NORM);
        btnCerrar.setForeground(AZUL_IG);
        btnCerrar.setBorderPainted(false); btnCerrar.setContentAreaFilled(false);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setHorizontalAlignment(SwingConstants.LEFT);
        btnCerrar.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnCerrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCerrar.addActionListener(e -> {
            liberarSesion(usuarioActual != null ? usuarioActual.getUsername() : null);
            usuarioActual = null; likesEnSesion.clear();
            cardLayout.show(mainPanel, "LOGIN");
        });
        lista.add(btnCerrar);

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    
    private void abrirSubConfig(String titulo, String[] opciones, Runnable[] acciones) {
        JPanel sub = new JPanel(new BorderLayout());
        sub.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 48));
        JButton btnV = new JButton("←");
        btnV.setFont(new Font("SansSerif", Font.BOLD, 18)); btnV.setForeground(NEGRO);
        btnV.setBorderPainted(false); btnV.setContentAreaFilled(false); btnV.setFocusPainted(false);
        btnV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnV.addActionListener(e -> { homeCenter.remove(sub); mostrarVista("CONFIG"); });
        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(FONT_BOLD);
        hdr.add(btnV, BorderLayout.WEST);
        hdr.add(lblTit, BorderLayout.CENTER);
        sub.add(hdr, BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(BLANCO);
        lista.setBorder(new EmptyBorder(12, 0, 0, 0));

        for (int i = 0; i < opciones.length; i++) {
            final Runnable accion = acciones[i];
            final boolean esRojo = opciones[i].toLowerCase().contains("desactivar")
                                || opciones[i].toLowerCase().contains("cerrar");
            JButton btn = new JButton(opciones[i]);
            btn.setFont(FONT_NORM);
            btn.setForeground(esRojo ? Color.RED : AZUL_IG);
            btn.setBorderPainted(false); btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(14, 20, 14, 20));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> accion.run());
            lista.add(btn);
            lista.add(new JSeparator());
        }

        sub.add(lista, BorderLayout.CENTER);
        homeCenter.add(sub, "SUBCONFIG");
        homeCard.show(homeCenter, "SUBCONFIG");
    }

    private JPanel filaConfig(String icono, String titulo, String subtitulo, Runnable accion) {
        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.setBackground(BLANCO);
        fila.setBorder(new EmptyBorder(12, 16, 12, 16));
        fila.setMaximumSize(new Dimension(390, 70));
        fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblIco = new JLabel(icono);
        lblIco.setFont(new Font("SansSerif", Font.PLAIN, 20));
        fila.add(lblIco, BorderLayout.WEST);

        JPanel texto = new JPanel();
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.setBackground(BLANCO);
        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(FONT_NORM);
        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(Color.GRAY);
        texto.add(lblTit);
        texto.add(lblSub);
        fila.add(texto, BorderLayout.CENTER);

        JLabel flecha = new JLabel("›");
        flecha.setFont(new Font("SansSerif", Font.PLAIN, 20));
        flecha.setForeground(Color.GRAY);
        fila.add(flecha, BorderLayout.EAST);

        if (accion != null) fila.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { accion.run(); }
        });
        return fila;
    }

    
    private JButton perfilBtn(String texto) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(219, 219, 219));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(new Color(239, 239, 239));
        btn.setForeground(NEGRO);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(158, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel construirNotificaciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 48));
        JLabel lblTit = new JLabel("  Notificaciones");
        lblTit.setFont(new Font("SansSerif", Font.BOLD, 16));
        hdr.add(lblTit, BorderLayout.WEST);
        panel.add(hdr, BorderLayout.NORTH);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(BLANCO);
        lista.setBorder(new EmptyBorder(8, 0, 8, 0));

        try {
            List<User> todos = userManager.obtenerUsuarios();
            boolean hayNotif = false;

            List<Inbox> todosMsgs = inboxManager.obtenerTodosMensajes(usuarioActual.getUsername());
            for (User u : todos) {
                if (u.getUsername().equalsIgnoreCase(usuarioActual.getUsername())) continue;
                int noLeidos = 0;
                for (Inbox msg : todosMsgs) {
                    if (msg.getEmisor().equalsIgnoreCase(u.getUsername()) &&
                        msg.getReceptor().equalsIgnoreCase(usuarioActual.getUsername()) &&
                        !msg.isLeido()) noLeidos++;
                }
                if (noLeidos > 0) {
                    final String emisor = u.getUsername();
                    lista.add(filaNotificacion(u,
                        "<b>" + u.getUsername() + "</b> te envió " + noLeidos + " mensaje(s) nuevo(s).",
                        "Responder"));
                    lista.add(new JSeparator());
                    hayNotif = true;
                }
            }

            for (User u : todos) {
                if (u.getUsername().equalsIgnoreCase(usuarioActual.getUsername())) continue;
                if (followManager.yaSigo(u.getUsername(), usuarioActual.getUsername())) {
                    lista.add(filaNotificacion(u,
                        "<b>" + u.getUsername() + "</b> comenzó a seguirte.", "Mensaje"));
                    lista.add(new JSeparator());
                    hayNotif = true;
                }
            }
            if (!hayNotif) {
                lista.add(Box.createVerticalStrut(40));
                lista.add(grayLabel("No tienes notificaciones nuevas."));
            }
        } catch (IOException e) { showError(e.getMessage()); }

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel filaNotificacion(User u, String texto, String accion) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setBackground(BLANCO);
        fila.setBorder(new EmptyBorder(6, 12, 6, 12));
        fila.setMaximumSize(new Dimension(390, 52));

        fila.add(avatarCircular(u.getFotoPerfil(), 36), BorderLayout.WEST);

        JLabel lblTexto = new JLabel("<html><div style='width:190px;'>" + texto + "</div></html>");
        lblTexto.setFont(FONT_SMALL);
        lblTexto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblTexto.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirPerfil(u.getUsername()); }
        });
        fila.add(lblTexto, BorderLayout.CENTER);

        if (accion != null) {
            JButton btnAcc = new JButton(accion);
            btnAcc.setFont(new Font("SansSerif", Font.PLAIN, 10));
            btnAcc.setBackground(new Color(239,239,239));
            btnAcc.setForeground(NEGRO);
            btnAcc.setOpaque(true);
            btnAcc.setBorderPainted(true);
            btnAcc.setBorder(new LineBorder(GRIS_BORDE, 1, true));
            btnAcc.setFocusPainted(false);
            btnAcc.setPreferredSize(new Dimension(70, 24));
            btnAcc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnAcc.addActionListener(e -> abrirConversacion(u.getUsername()));

            JPanel btnWrap = new JPanel(new GridBagLayout());
            btnWrap.setBackground(BLANCO);
            btnWrap.setBorder(new EmptyBorder(0, 0, 0, 30));
            btnWrap.add(btnAcc);
            fila.add(btnWrap, BorderLayout.EAST);
        }
        return fila;
    }

    private static final String ASSETS = "assets/";

    private static final String FOTO_POPEYES    = ASSETS + "popeyes_perfil.jpg";
    private static final String FOTO_REALMADRID = ASSETS + "realmadrid_perfil.jpg";
    private static final String FOTO_UNITEC     = ASSETS + "unitec_perfil.jpg";

    private static final String POP_IMG1 = ASSETS + "popeyes_post1.jpg";
    private static final String POP_IMG2 = ASSETS + "popeyes_post2.jpg";
    private static final String POP_IMG3 = ASSETS + "popeyes_post3.jpg";

    private static final String RM_IMG1  = ASSETS + "realmadrid_post1.jpg";
    private static final String RM_IMG2  = ASSETS + "realmadrid_post2.jpg";
    private static final String RM_IMG3  = ASSETS + "realmadrid_post3.jpg";

    private static final String UNI_IMG1 = ASSETS + "unitec_post1.jpg";
    private static final String UNI_IMG2 = ASSETS + "unitec_post2.jpg";
    private static final String UNI_IMG3 = ASSETS + "unitec_post3.jpg";

    private void inicializarCuentasPredeterminadas() {

        crearCuentaSiNoExiste("popeyes", "Popeyes Honduras", "popeyes123", FOTO_POPEYES,
            new String[]{
                "🍗 ¡Bienvenidos al sabor! Pollo frito crujiente como ninguno. #Popeyes #PolloFrito",
                "🔥 Nuestra receta secreta lleva más de 50 años. ¡Ven y pruébala! #Louisiana #FastFood",
                "🌶️ Nuevo Chicken Sandwich picante disponible ya en todas las sucursales. #Nuevo #HotChicken"
            },
            new String[]{ POP_IMG1, POP_IMG2, POP_IMG3 });

        crearCuentaSiNoExiste("realmadrid", "Real Madrid CF", "hala_madrid", FOTO_REALMADRID,
            new String[]{
                "⚽ ¡Hala Madrid! Los mejores del mundo siguen escribiendo historia. #RealMadrid #UCL",
                "🏆 14 Champions League y contando. El club más grande del mundo. #HalaMadrid",
                "🌟 Entrenamiento completado. Listos para el próximo partido. #RMCity #Madridistas"
            },
            new String[]{ RM_IMG1, RM_IMG2, RM_IMG3 });

        crearCuentaSiNoExiste("unitec", "Universidad UNITEC", "unitec123", FOTO_UNITEC,
            new String[]{
                "🎓 Formando los profesionales del futuro desde 1992. ¡Inscripciones abiertas! #UNITEC",
                "💡 Innovación y tecnología al servicio de la educación hondureña. #Honduras #Educacion",
                "📚 ¡Felicitaciones a todos nuestros graduados! El esfuerzo siempre da frutos. #Graduacion"
            },
            new String[]{ UNI_IMG1, UNI_IMG2, UNI_IMG3 });

        seguirSiNoSiguen("popeyes",    "realmadrid");
        seguirSiNoSiguen("popeyes",    "unitec");
        seguirSiNoSiguen("realmadrid", "popeyes");
        seguirSiNoSiguen("realmadrid", "unitec");
        seguirSiNoSiguen("unitec",     "popeyes");
        seguirSiNoSiguen("unitec",     "realmadrid");
    }

    private void seguirSiNoSiguen(String quien, String aQuien) {
        try {
            if (!followManager.yaSigo(quien, aQuien))
                followManager.seguir(quien, aQuien);
        } catch (IOException e) {
            System.out.println("Error al seguir entre cuentas predeterminadas: " + e.getMessage());
        }
    }

    private void crearCuentaSiNoExiste(String username, String nombre, String password,
                                        String fotoPerfil, String[] posts, String[] imagenes) {
        try {
            if (userManager.buscarporUsername(username) != null) return;

            File fotoFile = new File(fotoPerfil);
            userManager.crearUser(username, nombre,
                UserManager.Genero.M, password, 0,
                fotoFile.exists() ? fotoFile : null,
                UserManager.TipoCuenta.PUBLICO);

            for (int i = 0; i < posts.length; i++) {
                String ruta = (i < imagenes.length) ? imagenes[i] : "";
                File imgFile = new File(ruta);
                String tipo  = imgFile.exists() ? "CUADRADA" : "NINGUNA";
                pubManager.agregarPublicacion(username, posts[i],
                    imgFile.exists() ? ruta : "", tipo);
            }
        } catch (IOException e) {
            System.out.println("Error creando cuenta predeterminada: " + e.getMessage());
        }
    }

    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;
                int width = 0, height = 0, rowHeight = 0, rowWidth = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component c = target.getComponent(i);
                    if (c.isVisible()) {
                        Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            width = Math.max(width, rowWidth);
                            height += rowHeight + vgap;
                            rowWidth = 0; rowHeight = 0;
                        }
                        rowWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return new Dimension(width, height);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp());
    }
}