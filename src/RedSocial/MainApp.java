/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RedSocial;

import RedSocial.UserManager.Genero;
import RedSocial.UserManager.TipoCuenta;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MainApp extends JFrame {

    // ── Managers ──────────────────────────────────────────────────────────────
    private final UserManager        userManager        = new UserManager();
    private final PublicacionManager pubManager         = new PublicacionManager();
    private final FollowManager      followManager      = new FollowManager();
    private final InboxManager       inboxManager       = new InboxManager();
    private final StickerManager     stickerManager     = new StickerManager();
    private final ComentarioManager  comentarioManager  = new ComentarioManager();

    // ── Socket: mensajería en tiempo real ─────────────────────────────────────
    private final InboxServer mensajeServer = new InboxServer();

    private Runnable recargarConversacionActual = null;
    private String   conversacionAbieraCon      = null;

    // ── Estado ────────────────────────────────────────────────────────────────
    private User usuarioActual = null;
    private String vistaAnterior = "FEED"; // vista activa antes de abrir perfil ajeno
    // Guarda "usernameAutor|fechaHora" de publicaciones ya likeadas en esta sesión
    private final java.util.Set<String> likesEnSesion = new java.util.HashSet<>();

    // ── Colores Instagram ─────────────────────────────────────────────────────
    private static final Color BLANCO     = Color.WHITE;
    private static final Color NEGRO      = new Color(30, 30, 30);
    private static final Color GRIS_CLARO = new Color(250, 250, 250);
    private static final Color GRIS_BORDE = new Color(219, 219, 219);
    private static final Color AZUL_IG    = new Color(0, 149, 246);
    private static final Color ROSA_IG    = new Color(225, 48, 108);
    private static final Font  FONT_BOLD  = new Font("SansSerif", Font.BOLD,   13);
    private static final Font  FONT_NORM  = new Font("SansSerif", Font.PLAIN,  12);
    private static final Font  FONT_SMALL = new Font("SansSerif", Font.PLAIN,  10);
    private static final Font  FONT_LOGO  = new Font("Serif",     Font.ITALIC, 26);

    // ── Panel principal (CardLayout) ──────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     mainPanel  = new JPanel(cardLayout);

    public MainApp() {
        setTitle("Instagram");
        setSize(390, 844);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(construirPantallaLogin(),    "LOGIN");
        mainPanel.add(construirPantallaRegistro(), "REGISTRO");
        mainPanel.add(construirPantallaHome(),     "HOME");

        cardLayout.show(mainPanel, "LOGIN");

        // ── Arrancar el Socket ─────────────────────────────────────────────────
        mensajeServer.iniciar();

        mensajeServer.setListener((emisor, receptor, contenido, tipo) -> {
            if (usuarioActual != null &&
                receptor.equalsIgnoreCase(usuarioActual.getUsername()) &&
                emisor.equalsIgnoreCase(conversacionAbieraCon) &&
                recargarConversacionActual != null) {
                recargarConversacionActual.run();
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                mensajeServer.cerrar();
            }
        });

        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PANTALLA: LOGIN
    // ══════════════════════════════════════════════════════════════════════════

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
        btnReactivar.addActionListener(e -> dialogReactivar());

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

    // ══════════════════════════════════════════════════════════════════════════
    // PANTALLA: REGISTRO
    // ══════════════════════════════════════════════════════════════════════════

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

        // Género
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

        // Tipo de cuenta
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

        // Foto de perfil
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
            // Enums viven dentro de UserManager
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

    // ══════════════════════════════════════════════════════════════════════════
    // PANTALLA: HOME
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel homeFeed;
    private JPanel homeBuscar;
    private JPanel homeInbox;
    private JPanel homePerfil;
    private JLabel lblNavBadge;

    private final CardLayout homeCard   = new CardLayout();
    private final JPanel     homeCenter = new JPanel(homeCard);

    private JPanel construirPantallaHome() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLANCO);
        header.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        header.setPreferredSize(new Dimension(390, 50));
        JLabel logoHeader = new JLabel("  Instagram");
        logoHeader.setFont(new Font("Serif", Font.ITALIC, 22));
        header.add(logoHeader, BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        homeCenter.setBackground(BLANCO);
        panel.add(homeCenter, BorderLayout.CENTER);
        panel.add(construirNavBar(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel construirNavBar() {
        JPanel nav = new JPanel(new GridLayout(1, 5));
        nav.setBackground(BLANCO);
        nav.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        nav.setPreferredSize(new Dimension(390, 55));

        String[] iconos = {"⌂", "🔍", "＋", "✉", "👤"};
        String[] vistas  = {"FEED", "BUSCAR", "POST", "INBOX", "PERFIL"};

        for (int i = 0; i < 5; i++) {
            final String vista = vistas[i];
            JButton btn = new JButton(iconos[i]);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 20));
            btn.setForeground(NEGRO);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> mostrarVista(vista));
            if (i == 3) lblNavBadge = new JLabel();
            nav.add(btn);
        }
        return nav;
    }

    private void mostrarHome() {
        inicializarCuentasPredeterminadas();
        homeCenter.removeAll();
        homeCenter.add(construirFeed(),   "FEED");
        homeCenter.add(construirBuscar(), "BUSCAR");
        homeCenter.add(construirPost(),   "POST");
        homeCenter.add(construirInbox(),  "INBOX");
        homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
        homeCard.show(homeCenter, "FEED");
        cardLayout.show(mainPanel, "HOME");
    }

    private void mostrarVista(String vista) {
        if (vista.equals("POST")) {
            // Post es sub-vista, la reconstruimos cada vez que se abre
            homeCenter.remove(getComponentByName("POST"));
            homeCenter.add(construirPost(), "POST");
            homeCard.show(homeCenter, "POST");
            return;
        }
        vistaAnterior = vista; // guardamos para poder volver desde perfil ajeno
        if (vista.equals("INBOX")) {
            homeCenter.remove(getComponentByName("INBOX"));
            homeCenter.add(construirInbox(), "INBOX");
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
            // CardLayout no expone nombres directamente
        }
        return new JPanel();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUB-VISTA: FEED
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel construirFeed() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(GRIS_CLARO);

        JPanel feedPanel = new JPanel();
        feedPanel.setLayout(new BoxLayout(feedPanel, BoxLayout.Y_AXIS));
        feedPanel.setBackground(GRIS_CLARO);

        try {
            List<String> following = followManager.obtenerFollowing(usuarioActual.getUsername());
            List<Publicacion> pubs = pubManager.obtenerFeed(
                usuarioActual.getUsername(), following, userManager);

            // Siempre agregar publicaciones de cuentas predeterminadas
            // (solo si el usuario no las sigue ya, para no duplicar)
            String[] predeterminadas = {"popeyes", "realmadrid", "unitec"};
            for (String cuenta : predeterminadas) {
                if (!following.contains(cuenta) &&
                    !cuenta.equalsIgnoreCase(usuarioActual.getUsername())) {
                    List<Publicacion> extra = pubManager.obtenerPublicaciones(cuenta);
                    for (Publicacion p : extra) pubs.add(p);
                }
            }
            // Reordenar todo por fecha descendente
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

        // Usar BorderLayout para que las tarjetas ocupen todo el ancho
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(GRIS_CLARO);
        centerWrapper.add(feedPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(centerWrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        // Scroll siempre empieza desde arriba
        SwingUtilities.invokeLater(() ->
            scroll.getVerticalScrollBar().setValue(0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel construirTarjetaPublicacion(Publicacion p) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BLANCO);
        card.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        card.setMaximumSize(new Dimension(32767, 999));

        JPanel cardHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        cardHeader.setBackground(BLANCO);
        // Avatar circular pequeño con foto real si existe
        try {
            User autorUser = userManager.buscarporUsername(p.getUserAutor());
            File fotoAutor = (autorUser != null) ? autorUser.getFotoPerfil() : null;
            cardHeader.add(avatarCircular(fotoAutor, 36));
        } catch (IOException ex) { cardHeader.add(avatarCircular(null, 36)); }
        JLabel userLbl = new JLabel(p.getUserAutor()); // sin @
        userLbl.setFont(FONT_BOLD);
        userLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userLbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirPerfil(p.getUserAutor()); }
        });
        cardHeader.add(userLbl);

        card.add(cardHeader);
        // Mostrar imagen centrada sin scrollbar horizontal
        if (!p.getRutaImagen().isEmpty()) {
            File imgFile = new File(p.getRutaImagen());
            if (imgFile.exists()) {
                // Escalar a 390px de ancho manteniendo proporción
                ImageIcon rawIcon = new ImageIcon(p.getRutaImagen());
                int origW = rawIcon.getIconWidth();
                int origH = rawIcon.getIconHeight();
                int targetW = 370;
                int targetH = (origW > 0) ? (int)(((double)origH / origW) * targetW) : 240;
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

        JPanel botonesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botonesRow.setBackground(BLANCO);

        // Botón de like — persiste entre cambios de vista usando likesEnSesion
        String likeKey = p.getUserAutor() + "|" + p.getFecha();
        boolean yaLikeado = likesEnSesion.contains(likeKey);
        JButton btnLike = new JButton(yaLikeado
            ? "♥  " + p.getLikes() + " Me gusta"
            : "♡  " + p.getLikes() + " Me gusta");
        btnLike.setFont(FONT_BOLD);
        btnLike.setForeground(yaLikeado ? ROSA_IG : NEGRO);
        btnLike.setBorderPainted(false);
        btnLike.setContentAreaFilled(false);
        btnLike.setFocusPainted(false);
        btnLike.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLike.addActionListener(e -> {
            try {
                if (likesEnSesion.contains(likeKey)) {
                    // Quitar like
                    pubManager.quitarLike(p.getUserAutor(), p.getFecha());
                    p.setLikes(Math.max(0, p.getLikes() - 1));
                    likesEnSesion.remove(likeKey);
                    btnLike.setText("♡  " + p.getLikes() + " Me gusta");
                    btnLike.setForeground(NEGRO);
                } else {
                    // Dar like
                    pubManager.darLike(p.getUserAutor(), p.getFecha());
                    p.setLikes(p.getLikes() + 1);
                    likesEnSesion.add(likeKey);
                    btnLike.setText("♥  " + p.getLikes() + " Me gusta");
                    btnLike.setForeground(ROSA_IG);
                }
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        JLabel lblFecha = new JLabel(p.getFechaStr());
        lblFecha.setFont(FONT_SMALL);
        lblFecha.setForeground(Color.GRAY);

        JButton btnComment = new JButton("💬 Comentar");
        btnComment.setFont(FONT_NORM);
        btnComment.setForeground(NEGRO);
        btnComment.setBorderPainted(false);
        btnComment.setContentAreaFilled(false);
        btnComment.setFocusPainted(false);
        btnComment.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnComment.addActionListener(e -> abrirComentarios(p));

        botonesRow.add(btnLike);
        botonesRow.add(btnComment);
        footerCard.add(botonesRow);
        // Fecha en su propia línea, siempre visible
        JPanel fechaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fechaRow.setBackground(BLANCO);
        fechaRow.add(lblFecha);
        footerCard.add(fechaRow);
        card.add(footerCard);

        return card;
    }

    private void abrirComentarios(Publicacion p) {
        // Mostrar comentarios en la misma ventana (como sub-vista)
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        // Header con botón volver
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

        // Panel de comentarios con scroll
        JPanel commPanel = new JPanel();
        commPanel.setLayout(new BoxLayout(commPanel, BoxLayout.Y_AXIS));
        commPanel.setBackground(BLANCO);
        commPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Publicación original arriba
        try {
            User autor = userManager.buscarporUsername(p.getUserAutor());
            JPanel pubOrig = new JPanel(new BorderLayout(8, 0));
            pubOrig.setBackground(BLANCO);
            pubOrig.setBorder(new EmptyBorder(6, 8, 6, 8));
            pubOrig.add(avatarCircular(autor != null ? autor.getFotoPerfil() : null, 36),
                BorderLayout.WEST);
            // JTextArea para que el texto haga wrap correctamente
            JTextArea txtOrig = new JTextArea();
            txtOrig.setText(p.getUserAutor() + "  " + p.getContenido());
            txtOrig.setFont(FONT_NORM);
            txtOrig.setLineWrap(true);
            txtOrig.setWrapStyleWord(true);
            txtOrig.setEditable(false);
            txtOrig.setBackground(BLANCO);
            txtOrig.setBorder(null);
            pubOrig.add(txtOrig, BorderLayout.CENTER);
            commPanel.add(pubOrig);
        } catch (IOException ex) {}
        commPanel.add(new JSeparator());
        commPanel.add(Box.createVerticalStrut(8));

        // Cargar comentarios persistidos
        Runnable cargarComentarios = () -> {
            // quitar todo excepto los primeros 3 componentes (pub original + separador + strut)
            while (commPanel.getComponentCount() > 3) commPanel.remove(3);
            try {
                for (Comentario c : comentarioManager.obtenerComentarios(
                        p.getUserAutor(), p.getFecha())) {
                    commPanel.add(filaComentario(c, p));
                    commPanel.add(Box.createVerticalStrut(4));
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

        // Campo para escribir comentario
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
                // Guardar en disco
                comentarioManager.agregarComentario(
                    p.getUserAutor(), p.getFecha(),
                    usuarioActual.getUsername(), texto);
                txtComentario.setText("");
                cargarComentarios.run();
                SwingUtilities.invokeLater(() ->
                    scrollComm.getVerticalScrollBar().setValue(
                        scrollComm.getVerticalScrollBar().getMaximum()));
            } catch (IOException ex) { showError(ex.getMessage()); }
        };
        btnEnviarComm.addActionListener(enviarComm);
        txtComentario.addActionListener(enviarComm);

        inputPanel.add(txtComentario, BorderLayout.CENTER);
        inputPanel.add(btnEnviarComm, BorderLayout.EAST);

        // Panel de sugerencias @usuario y #hashtag
        JPanel sugerComm = new JPanel();
        sugerComm.setLayout(new BoxLayout(sugerComm, BoxLayout.Y_AXIS));
        sugerComm.setBackground(BLANCO);
        sugerComm.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
        sugerComm.setVisible(false);

        txtComentario.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void actualizar() {
                sugerComm.removeAll();
                String texto = txtComentario.getText();
                // Detectar la palabra que se está escribiendo actualmente
                int caret = txtComentario.getCaretPosition();
                if (caret > texto.length()) caret = texto.length();
                String antes = texto.substring(0, caret);
                // Buscar el inicio de la palabra actual
                int inicio = antes.lastIndexOf(' ') + 1;
                String palabraActual = antes.substring(inicio);

                if (palabraActual.startsWith("@") && palabraActual.length() > 1) {
                    // Sugerencias de usuarios
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
                                    // Reemplazar la palabra actual por el usuario elegido
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
                    // Sugerencias de hashtags — buscar publicaciones con ese tag
                    String tag = palabraActual;
                    try {
                        List<User> todos = userManager.obtenerUsuarios();
                        List<Publicacion> pubs = pubManager.buscarPorHashtag(tag, todos);
                        // Extraer hashtags únicos que coincidan
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
        fila.setBorder(new EmptyBorder(4, 8, 4, 8));
        // Avatar alineado arriba con el texto
        JPanel avatarWrapper = new JPanel(new BorderLayout());
        avatarWrapper.setBackground(BLANCO);
        try {
            User u = userManager.buscarporUsername(c.getAutor());
            avatarWrapper.add(avatarCircular(u != null ? u.getFotoPerfil() : null, 32),
                BorderLayout.NORTH);
        } catch (IOException ex) {
            avatarWrapper.add(avatarCircular(null, 32), BorderLayout.NORTH);
        }
        fila.add(avatarWrapper, BorderLayout.WEST);
        JTextArea txtComm = new JTextArea();
        txtComm.setText(c.getAutor() + "  " + c.getContenido());
        txtComm.setFont(FONT_NORM);
        txtComm.setLineWrap(true);
        txtComm.setWrapStyleWord(true);
        txtComm.setEditable(false);
        txtComm.setBackground(BLANCO);
        txtComm.setBorder(null);
        txtComm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtComm.addMouseListener(new MouseAdapter() {
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
        fila.add(txtComm, BorderLayout.CENTER);
        return fila;
    }

    // Resalta @menciones en azul y #hashtags en rosa dentro de HTML
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

    // ══════════════════════════════════════════════════════════════════════════
    // SUB-VISTA: BUSCAR
    // ══════════════════════════════════════════════════════════════════════════

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

        // Placeholder dinámico según el modo
        txtBuscar.setToolTipText("@ para usuarios, # para hashtags");

        Runnable buscar = () -> {
            resultados.removeAll();
            String q = txtBuscar.getText().trim();
            if (q.isEmpty()) { resultados.revalidate(); resultados.repaint(); return; }
            try {
                if (q.startsWith("#")) {
                    // Buscar por hashtag
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
                    // Buscar usuarios (con o sin @)
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

        // Sugerencias en tiempo real
        JPanel sugerenciasPanel = new JPanel();
        sugerenciasPanel.setLayout(new BoxLayout(sugerenciasPanel, BoxLayout.Y_AXIS));
        sugerenciasPanel.setBackground(BLANCO);
        sugerenciasPanel.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        sugerenciasPanel.setVisible(false);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void actualizar() {
                String q = txtBuscar.getText().trim();
                sugerenciasPanel.removeAll();
                // Solo sugerir usuarios (no hashtags)
                if (q.isEmpty() || q.startsWith("#")) {
                    sugerenciasPanel.setVisible(false);
                    sugerenciasPanel.revalidate(); sugerenciasPanel.repaint(); return;
                }
                try {
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
                                txtBuscar.setText("@" + u.getUsername());
                                sugerenciasPanel.setVisible(false);
                                buscar.run();
                            }
                        });
                        sugerenciasPanel.add(fila);
                        if (i < max - 1) sugerenciasPanel.add(new JSeparator());
                    }
                    sugerenciasPanel.setVisible(!lista.isEmpty());
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
        fila.add(avatarLabel("○"));
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

    // ══════════════════════════════════════════════════════════════════════════
    // SUB-VISTA: INBOX
    // ══════════════════════════════════════════════════════════════════════════

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
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        fila.setBackground(BLANCO);
        fila.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        fila.setMaximumSize(new Dimension(390, 65));
        fila.add(avatarLabel("○"));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(BLANCO);
        JLabel lblUser = new JLabel("@" + otroUsername);
        lblUser.setFont(FONT_BOLD);
        info.add(lblUser);

        try {
            // Contamos no leídos usando el método recursivo del InboxManager
            List<Inbox> todos = inboxManager.obtenerTodosMensajes(usuarioActual.getUsername());
            int noLeidos = inboxManager.contarNoLeidosRecursivo(todos, usuarioActual.getUsername(), 0);
            // Filtramos solo los de este remitente para mostrar el badge correcto
            int noLeidosEste = 0;
            for (Inbox inbox : todos) {
                if (inbox.getEmisor().equalsIgnoreCase(otroUsername) &&
                    inbox.getReceptor().equalsIgnoreCase(usuarioActual.getUsername()) &&
                    !inbox.isLeido()) {
                    noLeidosEste++;
                }
            }
            if (noLeidosEste > 0) {
                JLabel badge = new JLabel(" " + noLeidosEste + " nuevo(s)");
                badge.setFont(FONT_SMALL);
                badge.setForeground(AZUL_IG);
                info.add(badge);
            }
        } catch (IOException e) {}

        fila.add(info);
        fila.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fila.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { abrirConversacion(otroUsername); }
        });
        return fila;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUB-VISTA: PERFIL
    // ══════════════════════════════════════════════════════════════════════════

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
            headerPerfil.setBorder(new EmptyBorder(16, 14, 6, 14));

            // ── Username arriba centrado ──────────────────────────────────────
            JLabel lblUsername = new JLabel(u.getUsername(), SwingConstants.CENTER);
            lblUsername.setFont(FONT_BOLD);
            lblUsername.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPerfil.add(lblUsername);
            headerPerfil.add(Box.createVerticalStrut(12));

            // ── Fila: avatar + stats ──────────────────────────────────────────
            JPanel filaTop = new JPanel(new BorderLayout(16, 0));
            filaTop.setBackground(BLANCO);
            filaTop.setMaximumSize(new Dimension(390, 100));

            JLabel avatarGrande = avatarCircular(u.getFotoPerfil(), 86);
            avatarGrande.setBorder(new EmptyBorder(0, 0, 0, 8));
            filaTop.add(avatarGrande, BorderLayout.WEST);

            JPanel stats = new JPanel(new GridLayout(1, 3, 0, 0));
            stats.setBackground(BLANCO);
            stats.add(statBox(String.valueOf(pubs.size()), "Posts"));
            stats.add(statBox(String.valueOf(followers.size()), "Seguidores"));
            stats.add(statBox(String.valueOf(following.size()), "Siguiendo"));
            filaTop.add(stats, BorderLayout.CENTER);
            headerPerfil.add(filaTop);
            headerPerfil.add(Box.createVerticalStrut(10));

            // ── Nombre completo + tipo de cuenta ─────────────────────────────
            JLabel lblNombre = new JLabel(u.getNombreCompleto());
            lblNombre.setFont(FONT_BOLD);
            JLabel lblTipo = new JLabel(u.getTipoCuenta() == UserManager.TipoCuenta.PRIVADO
                ? "🔒 Cuenta privada" : "🌐 Cuenta pública");
            lblTipo.setFont(FONT_SMALL);
            lblTipo.setForeground(Color.GRAY);
            headerPerfil.add(lblNombre);
            headerPerfil.add(lblTipo);
            headerPerfil.add(Box.createVerticalStrut(10));

            JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            botonesPanel.setBackground(BLANCO);

            if (esMiPerfil) {
                // Botones estilo Instagram en fila completa
                JPanel btnGrid = new JPanel(new GridLayout(1, 2, 6, 0));
                btnGrid.setBackground(BLANCO);
                btnGrid.setMaximumSize(new Dimension(362, 36));
                btnGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

                JButton btnLogout = new JButton("Cerrar sesión");
                btnLogout.setFont(FONT_BOLD);
                btnLogout.setBackground(GRIS_CLARO);
                btnLogout.setForeground(NEGRO);
                btnLogout.setOpaque(true);
                btnLogout.setBorderPainted(true);
                btnLogout.setBorder(new LineBorder(GRIS_BORDE, 1, true));
                btnLogout.setFocusPainted(false);
                btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnLogout.addActionListener(e -> {
                    usuarioActual = null;
                    likesEnSesion.clear();
                    cardLayout.show(mainPanel, "LOGIN");
                });

                JButton btnDesactivar = new JButton("Desactivar cuenta");
                btnDesactivar.setFont(FONT_BOLD);
                btnDesactivar.setBackground(GRIS_CLARO);
                btnDesactivar.setForeground(Color.RED);
                btnDesactivar.setOpaque(true);
                btnDesactivar.setBorderPainted(true);
                btnDesactivar.setBorder(new LineBorder(GRIS_BORDE, 1, true));
                btnDesactivar.setFocusPainted(false);
                btnDesactivar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnDesactivar.addActionListener(e -> {
                    int conf = JOptionPane.showConfirmDialog(this,
                        "¿Desactivar tu cuenta?", "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (conf == JOptionPane.YES_OPTION) {
                        try {
                            usuarioActual.setEstadoCuenta(false);
                            userManager.actualizarUsuario(usuarioActual);
                            usuarioActual = null;
                            cardLayout.show(mainPanel, "LOGIN");
                        } catch (IOException ex) { showError(ex.getMessage()); }
                    }
                });
                btnGrid.add(btnLogout);
                btnGrid.add(btnDesactivar);
                botonesPanel.add(btnGrid);
            } else {
                boolean yaSigo = followManager.yaSigo(usuarioActual.getUsername(), username);

                JPanel btnGrid2 = new JPanel(new GridLayout(1, 2, 6, 0));
                btnGrid2.setBackground(BLANCO);
                btnGrid2.setMaximumSize(new Dimension(362, 36));
                btnGrid2.setAlignmentX(Component.LEFT_ALIGNMENT);

                JButton btnFollow = new JButton(yaSigo ? "Siguiendo" : "Seguir");
                btnFollow.setFont(FONT_BOLD);
                btnFollow.setBackground(yaSigo ? GRIS_CLARO : AZUL_IG);
                btnFollow.setForeground(yaSigo ? NEGRO : BLANCO);
                btnFollow.setOpaque(true);
                btnFollow.setBorderPainted(true);
                btnFollow.setBorder(new LineBorder(yaSigo ? GRIS_BORDE : AZUL_IG, 1, true));
                btnFollow.setFocusPainted(false);
                btnFollow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

                JButton btnMensaje = new JButton("Mensaje");
                btnMensaje.setFont(FONT_BOLD);
                btnMensaje.setBackground(GRIS_CLARO);
                btnMensaje.setForeground(NEGRO);
                btnMensaje.setOpaque(true);
                btnMensaje.setBorderPainted(true);
                btnMensaje.setBorder(new LineBorder(GRIS_BORDE, 1, true));
                btnMensaje.setFocusPainted(false);
                btnMensaje.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnMensaje.addActionListener(e -> abrirConversacion(username));

                btnGrid2.add(btnFollow);
                btnGrid2.add(btnMensaje);
                botonesPanel.add(btnGrid2);
            }

            headerPerfil.add(botonesPanel);

            // Grid de publicaciones — 3 columnas, celdas cuadradas 128x128
            int CELDA = 128;
            JPanel gridPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 2));
            gridPanel.setBackground(BLANCO);
            gridPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

            if (pubs.isEmpty()) {
                gridPanel.setLayout(new FlowLayout());
                gridPanel.setBackground(BLANCO);
                gridPanel.add(grayLabel(esMiPerfil ? "Aún no has publicado nada."
                    : (u.getTipoCuenta() == UserManager.TipoCuenta.PRIVADO
                        ? "Perfil privado." : "Sin publicaciones.")));
            } else {
                for (Publicacion p : pubs) {
                    JPanel miniatura = new JPanel(new BorderLayout());
                    miniatura.setBackground(GRIS_CLARO);
                    miniatura.setPreferredSize(new Dimension(CELDA, CELDA));
                    miniatura.setMaximumSize(new Dimension(CELDA, CELDA));

                    if (!p.getRutaImagen().isEmpty() && new File(p.getRutaImagen()).exists()) {
                        // Recorte cuadrado centrado de la imagen
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

            // Separador con icono de grid (estilo Instagram)
            JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            tabBar.setBackground(BLANCO);
            tabBar.setBorder(new MatteBorder(1, 0, 0, 0, GRIS_BORDE));
            JLabel lblGrid = new JLabel("⊞  Publicaciones");
            lblGrid.setFont(FONT_SMALL);
            lblGrid.setForeground(NEGRO);
            tabBar.add(lblGrid);
            headerPerfil.add(tabBar);

            JScrollPane scroll = new JScrollPane(gridPanel);
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
            panel.add(headerPerfil, BorderLayout.NORTH);
            panel.add(scroll, BorderLayout.CENTER);

        } catch (IOException e) { showError(e.getMessage()); }
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════════
    // SUB-VISTA: POST
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel construirPost() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        // Header con título
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLANCO);
        header.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        header.setPreferredSize(new Dimension(390, 44));
        JLabel lblTitulo = new JLabel("  Nueva publicación");
        lblTitulo.setFont(FONT_BOLD);
        header.add(lblTitulo, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);

        // Formulario
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        form.setBackground(BLANCO);

        // Área de texto
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

        // Selector de imagen con JFileChooser
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

        // Botón publicar
        JButton btnPublicar = igButton("Publicar", AZUL_IG, BLANCO);
        btnPublicar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPublicar.addActionListener(e -> {
            String contenido = txtContenido.getText().trim();
            if (contenido.isEmpty()) { showError("El contenido no puede estar vacío."); return; }
            if (contenido.length() > 220) { showError("Máximo 220 caracteres."); return; }
            try {
                String rutaImagen = archivoImagen[0] != null
                    ? archivoImagen[0].getAbsolutePath() : "";
                // Detectar tipo de multimedia automáticamente según extensión
                String tipo = "NINGUNA";
                if (archivoImagen[0] != null) tipo = "CUADRADA";
                pubManager.agregarPublicacion(
                    usuarioActual.getUsername(), contenido, rutaImagen, tipo);
                // Limpiar formulario
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

    // ══════════════════════════════════════════════════════════════════════════
    // DIÁLOGOS

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

            boolean puedeMensajear;
            if (otro.getTipoCuenta() == UserManager.TipoCuenta.PUBLICO) {
                puedeMensajear = true;
            } else {
                boolean yoSigo   = followManager.yaSigo(usuarioActual.getUsername(), otroUsername);
                boolean elMeSigue = followManager.yaSigo(otroUsername, usuarioActual.getUsername());
                puedeMensajear = yoSigo && elMeSigue;
            }
            if (!puedeMensajear) {
                showError("No puedes enviar mensajes a este perfil privado."); return;
            }
            inboxManager.marcarLeidos(usuarioActual.getUsername(), otroUsername);

        } catch (IOException e) { showError(e.getMessage()); return; }

        // Sub-vista de conversación (sin popup)
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        // Header con ← Volver y nombre del otro usuario
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLANCO);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        hdr.setPreferredSize(new Dimension(390, 44));
        JButton btnVolver = new JButton("←  Volver");
        btnVolver.setFont(FONT_BOLD); btnVolver.setForeground(AZUL_IG);
        btnVolver.setBorderPainted(false); btnVolver.setContentAreaFilled(false);
        btnVolver.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVolver.addActionListener(e -> {
            conversacionAbieraCon      = null;
            recargarConversacionActual = null;
            homeCenter.remove(wrapper);
            homeCard.show(homeCenter, "INBOX");
        });
        // Avatar del otro usuario en el header
        try {
            User otroUser = userManager.buscarporUsername(otroUsername);
            hdr.add(avatarCircular(otroUser != null ? otroUser.getFotoPerfil() : null, 30),
                BorderLayout.EAST);
        } catch (IOException ex) {}
        JLabel lblNombreConv = new JLabel(otroUsername, SwingConstants.CENTER);
        lblNombreConv.setFont(FONT_BOLD);
        hdr.add(btnVolver,    BorderLayout.WEST);
        hdr.add(lblNombreConv, BorderLayout.CENTER);
        wrapper.add(hdr, BorderLayout.NORTH);

        // Panel de mensajes
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
                        mensajesPanel.add(Box.createVerticalStrut(4));
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
        SwingUtilities.invokeLater(() ->
            scrollMensajes.getVerticalScrollBar().setValue(
                scrollMensajes.getVerticalScrollBar().getMaximum()));
        wrapper.add(scrollMensajes, BorderLayout.CENTER);

        // Input
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.setBorder(new EmptyBorder(6, 10, 10, 10));
        inputPanel.setBackground(BLANCO);

        JTextField txtMsg = new JTextField();
        txtMsg.setFont(FONT_NORM);
        txtMsg.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(GRIS_BORDE, 1, true),
            new EmptyBorder(6, 10, 6, 10)));

        JButton btnEnviar  = igButton("Enviar", AZUL_IG, BLANCO);
        JButton btnSticker = igButton("🎭",     GRIS_BORDE, NEGRO);

        ActionListener enviarTexto = e -> {
            String contenido = txtMsg.getText().trim();
            if (contenido.isEmpty()) return;
            try {
                inboxManager.enviarMensaje(usuarioActual.getUsername(), otroUsername, contenido, "TEXTO");
                mensajeServer.enviarNotificacion(usuarioActual.getUsername(), otroUsername, contenido, "TEXTO");
                txtMsg.setText("");
                cargarMensajes.run();
                SwingUtilities.invokeLater(() ->
                    scrollMensajes.getVerticalScrollBar().setValue(
                        scrollMensajes.getVerticalScrollBar().getMaximum()));
            } catch (IOException ex) { showError(ex.getMessage()); }
        };
        btnEnviar.addActionListener(enviarTexto);
        txtMsg.addActionListener(enviarTexto);

        btnSticker.addActionListener(e -> {
            try {
                List<String> stickers = stickerManager.obtenerTodos(usuarioActual.getUsername());
                String sel = (String) JOptionPane.showInputDialog(this,
                    "Elige un sticker:", "Stickers",
                    JOptionPane.PLAIN_MESSAGE, null,
                    stickers.toArray(), stickers.isEmpty() ? null : stickers.get(0));
                if (sel != null) {
                    inboxManager.enviarMensaje(usuarioActual.getUsername(), otroUsername, sel, "STICKER");
                    mensajeServer.enviarNotificacion(usuarioActual.getUsername(), otroUsername, sel, "STICKER");
                    cargarMensajes.run();
                }
            } catch (IOException ex) { showError(ex.getMessage()); }
        });

        JPanel botones = new JPanel(new GridLayout(1, 2, 4, 0));
        botones.setBackground(BLANCO);
        botones.add(btnSticker);
        botones.add(btnEnviar);
        inputPanel.add(txtMsg, BorderLayout.CENTER);
        inputPanel.add(botones, BorderLayout.EAST);

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
        bottom.add(inputPanel, BorderLayout.CENTER);
        bottom.add(btnEliminar, BorderLayout.SOUTH);
        wrapper.add(bottom, BorderLayout.SOUTH);

        homeCenter.add(wrapper, "CONVERSACION");
        homeCard.show(homeCenter, "CONVERSACION");
    }

    private JPanel burbujaMensaje(Inbox m, boolean soyYo) {
        JPanel fila = new JPanel(new FlowLayout(soyYo ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 4));
        fila.setBackground(BLANCO);
        fila.setMaximumSize(new Dimension(350, 999));

        String texto = m.getTipo().equals("STICKER") ? "🎭 " + m.getContenido() : m.getContenido();
        JLabel burbuja = new JLabel("<html><div style='padding:6px 10px;'>" + texto + "</div></html>");
        burbuja.setFont(FONT_NORM);
        burbuja.setOpaque(true);
        burbuja.setBackground(soyYo ? AZUL_IG : GRIS_CLARO);
        burbuja.setForeground(soyYo ? BLANCO : NEGRO);
        burbuja.setBorder(new EmptyBorder(0, 0, 0, 0));

        fila.add(burbuja);
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

        // Perfil ajeno: mostrar dentro de la misma ventana con botón volver
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BLANCO);

        // Header con botón ← Volver y nombre del usuario
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

        // Header con botón volver
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

        // Tarjeta centrada sin scrollbar horizontal
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

    private void dialogReactivar() {
        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        form.add(new JLabel("Username:")); form.add(txtUser);
        form.add(new JLabel("Contraseña:")); form.add(txtPass);

        int res = JOptionPane.showConfirmDialog(this, form, "Reactivar cuenta",
            JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                User u = userManager.buscarporUsername(txtUser.getText().trim());
                if (u == null) { showError("Usuario no encontrado."); return; }
                if (!u.getPassword().equals(new String(txtPass.getPassword()))) {
                    showError("Contraseña incorrecta."); return;
                }
                u.setEstadoCuenta(true);
                userManager.actualizarUsuario(u);
                JOptionPane.showMessageDialog(this, "Cuenta reactivada. Ya puedes iniciar sesión.");
            } catch (IOException e) { showError(e.getMessage()); }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VISIBILIDAD DE PUBLICACIONES
    // ══════════════════════════════════════════════════════════════════════════

    private List<Publicacion> obtenerPublicacionesVisibles(User objetivo) throws IOException {
        if (objetivo.getTipoCuenta() == UserManager.TipoCuenta.PUBLICO) {
            return pubManager.obtenerPublicaciones(objetivo.getUsername());
        }
        if (usuarioActual != null) {
            if (objetivo.getUsername().equalsIgnoreCase(usuarioActual.getUsername())) {
                return pubManager.obtenerPublicaciones(objetivo.getUsername());
            }
            boolean yoSigo   = followManager.yaSigo(usuarioActual.getUsername(), objetivo.getUsername());
            boolean elMeSigue = followManager.yaSigo(objetivo.getUsername(), usuarioActual.getUsername());
            if (yoSigo && elMeSigue) {
                return pubManager.obtenerPublicaciones(objetivo.getUsername());
            }
        }
        return new ArrayList<>();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS DE UI
    // ══════════════════════════════════════════════════════════════════════════

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

    /** Devuelve un JLabel con la foto recortada en círculo del tamaño indicado.
     *  Si la foto no existe muestra un círculo gris placeholder. */
    private JLabel avatarCircular(File fotoFile, int size) {
        java.awt.image.BufferedImage circle =
            new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circle.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (fotoFile != null && fotoFile.exists()) {
            // Cargar imagen y esperar a que esté lista antes de dibujar
            ImageIcon icon = new ImageIcon(fotoFile.getAbsolutePath());
            icon.getImage(); // trigger load
            Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            // Usar MediaTracker para asegurar carga completa
            java.awt.MediaTracker mt = new java.awt.MediaTracker(new java.awt.Canvas());
            mt.addImage(img, 0);
            try { mt.waitForAll(); } catch (InterruptedException ignored) {}
            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
            g2.drawImage(img, 0, 0, null);
        } else {
            // Círculo placeholder gris con borde
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

    // ══════════════════════════════════════════════════════════════════════════
    // CUENTAS PREDETERMINADAS
    // ══════════════════════════════════════════════════════════════════════════

    // ── Rutas de imágenes ────────────────────────────────────────────────────
    // Coloca tus imágenes en la carpeta assets/ junto a los .java
    // y cambia las rutas aquí según los nombres exactos de tus archivos.
    // Si un archivo no existe simplemente no se muestra (no rompe la app).

    private static final String ASSETS = "assets/";

    // Fotos de perfil
    private static final String FOTO_POPEYES    = ASSETS + "popeyes_perfil.jpg";
    private static final String FOTO_REALMADRID = ASSETS + "realmadrid_perfil.jpg";
    private static final String FOTO_UNITEC     = ASSETS + "unitec_perfil.jpg";

    // Imágenes de publicaciones — Popeyes
    private static final String POP_IMG1 = ASSETS + "popeyes_post1.jpg";
    private static final String POP_IMG2 = ASSETS + "popeyes_post2.jpg";
    private static final String POP_IMG3 = ASSETS + "popeyes_post3.jpg";

    // Imágenes de publicaciones — Real Madrid
    private static final String RM_IMG1  = ASSETS + "realmadrid_post1.jpg";
    private static final String RM_IMG2  = ASSETS + "realmadrid_post2.jpg";
    private static final String RM_IMG3  = ASSETS + "realmadrid_post3.jpg";

    // Imágenes de publicaciones — UNITEC
    private static final String UNI_IMG1 = ASSETS + "unitec_post1.jpg";
    private static final String UNI_IMG2 = ASSETS + "unitec_post2.jpg";
    private static final String UNI_IMG3 = ASSETS + "unitec_post3.jpg";

    private void inicializarCuentasPredeterminadas() {
        // Solo crea las cuentas y publicaciones si aún no existen
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

        // Hacer que las 3 cuentas se sigan entre sí (solo si aún no se siguen)
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

            // Foto de perfil: solo si el archivo existe
            File fotoFile = new File(fotoPerfil);
            userManager.crearUser(username, nombre,
                UserManager.Genero.M, password, 0,
                fotoFile.exists() ? fotoFile : null,
                UserManager.TipoCuenta.PUBLICO);

            // Publicaciones con sus imágenes
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

    // ══════════════════════════════════════════════════════════════════════════
    // WRAP LAYOUT — FlowLayout que ajusta filas automáticamente
    // Evita celdas vacías negras en el grid cuando hay < 3 publicaciones
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    // MAIN
    // ══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp());
    }
}
