/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RedSocial;

import RedSocial.UserManager.Genero;
import RedSocial.UserManager.TipoCuenta;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.io.*;

public class MainApp extends JFrame {
    private final UserManager usuarioManager = new UserManager();

    private User usuarioActual = null;

    //Colores y Fonts
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

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     mainPanel  = new JPanel(cardLayout);

    private final CardLayout homeCard   = new CardLayout();
    private final JPanel     homeCenter = new JPanel(homeCard);

    public MainApp() {
        setTitle("Instagram");
        setSize(390, 844);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(pantallaLogin(),    "LOGIN");
        mainPanel.add(pantallaRegistro(), "REGISTRO");
        mainPanel.add(pantallaHome(),     "HOME");

        cardLayout.show(mainPanel, "LOGIN");
        setVisible(true);
    }

    //Login
    private JPanel pantallaLogin() {
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
                User u = usuarioManager.buscarporUsername(user);
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
            } catch (IOException ex) { 
                showError(ex.getMessage()); }
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

        panel.add(caja);
        return panel;
    }

    //Registro
    private JPanel pantallaRegistro() {
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
        JTextField txtEdad = styledTextField("Edad");

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
            Genero genero   = rbM.isSelected() ? Genero.M : Genero.F;
            TipoCuenta tipo = rbPub.isSelected() ? TipoCuenta.PUBLICO : TipoCuenta.PRIVADO;

            if (nombre.isEmpty() || username.isEmpty() || pass.isEmpty() || edadStr.isEmpty()) {
                showError("Completa todos los campos."); return;
            }
            int edad;
            try { edad = Integer.parseInt(edadStr); } catch (NumberFormatException ex) {
                showError("Edad inválida."); return;
            }
            try {
                boolean ok = usuarioManager.crearUser(username, nombre, genero, pass, edad, archivoFoto[0], tipo);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "¡Cuenta creada! Ya puedes iniciar sesión.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "LOGIN");
                } else {
                    showError("El nombre de usuario ya existe.");
                }
            } catch (IOException ex) { 
                showError(ex.getMessage()); }
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

    //Home
    private JPanel pantallaHome() {
        JPanel panel = new JPanel(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLANCO);
        header.setBorder(new MatteBorder(0, 0, 1, 0, GRIS_BORDE));
        header.setPreferredSize(new Dimension(390, 50));
        JLabel logoHeader = new JLabel("  Instagram");
        logoHeader.setFont(new Font("Serif", Font.ITALIC, 22));
        header.add(logoHeader, BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        // Centro
        homeCenter.setBackground(BLANCO);
        panel.add(homeCenter, BorderLayout.CENTER);

        // NavBar inferior
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
            nav.add(btn);
        }
        return nav;
    }

    private void mostrarHome() {
        homeCenter.removeAll();
        homeCenter.add(construirFeed(), "FEED");
        homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
        homeCard.show(homeCenter, "FEED");
        cardLayout.show(mainPanel, "HOME");
    }

    private void mostrarVista(String vista) {
        if (vista.equals("FEED")) {
            homeCenter.removeAll();
            homeCenter.add(construirFeed(), "FEED");
            homeCard.show(homeCenter, "FEED");
        } else if (vista.equals("PERFIL")){
            homeCenter.removeAll();
            homeCenter.add(construirPerfil(usuarioActual.getUsername(), true), "PERFIL");
            homeCard.show(homeCenter, "PERFIL");
        } else {
            homeCenter.removeAll();
            homeCenter.add(construirPlaceholder(vista), vista);
            homeCard.show(homeCenter, vista);
        }
        homeCenter.revalidate();
        homeCenter.repaint();
    }

    //Feed
    private JPanel construirFeed() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(GRIS_CLARO);

        JPanel feedPanel = new JPanel();
        feedPanel.setLayout(new BoxLayout(feedPanel, BoxLayout.Y_AXIS));
        feedPanel.setBackground(GRIS_CLARO);
        feedPanel.add(Box.createVerticalStrut(40));

        JLabel lbl = new JLabel("Bienvenido/a, @" + usuarioActual.getUsername() + " 👋");
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(Color.GRAY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl2 = new JLabel("Aún no hay publicaciones.");
        lbl2.setFont(FONT_NORM);
        lbl2.setForeground(Color.GRAY);
        lbl2.setAlignmentX(Component.CENTER_ALIGNMENT);

        feedPanel.add(lbl);
        feedPanel.add(Box.createVerticalStrut(8));
        feedPanel.add(lbl2);

        JScrollPane scroll = new JScrollPane(feedPanel);
        scroll.setBorder(null);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel construirPlaceholder(String nombre) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BLANCO);
        JLabel lbl = new JLabel(nombre);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(Color.GRAY);
        panel.add(lbl);
        return panel;
    }
    
    //Perfil
    private JPanel construirPerfil(String username, boolean esMiPerfil) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLANCO);

        try {
            User u = usuarioManager.buscarporUsername(username);
            if (u == null) {
                panel.add(grayLabel("Usuario no encontrado."));
                return panel;
            }

            // ── Header ────────────────────────────────────────────────────────
            JPanel headerPerfil = new JPanel();
            headerPerfil.setLayout(new BoxLayout(headerPerfil, BoxLayout.Y_AXIS));
            headerPerfil.setBackground(BLANCO);
            headerPerfil.setBorder(new EmptyBorder(14, 14, 10, 14));

            // Avatar + stats
            JPanel filaStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
            filaStats.setBackground(BLANCO);

            JLabel avatarGrande = new JLabel("○");
            avatarGrande.setFont(new Font("SansSerif", Font.PLAIN, 50));
            avatarGrande.setForeground(GRIS_BORDE);

            // Contadores en 0 por ahora (sin FollowManager)
            JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
            stats.setBackground(BLANCO);
            stats.add(statBox("0", "Publicaciones"));
            stats.add(statBox("0", "Seguidores"));
            stats.add(statBox("0", "Siguiendo"));

            filaStats.add(avatarGrande);
            filaStats.add(stats);

            // Nombre y tipo de cuenta
            JLabel lblNombre = new JLabel("  " + u.getNombreCompleto());
            lblNombre.setFont(FONT_BOLD);
            JLabel lblTipo = new JLabel("  " + (u.getTipoCuenta() == TipoCuenta.PRIVADO
                    ? "Esta cuenta es privada" : "Cuenta pública"));
            lblTipo.setFont(FONT_SMALL);
            lblTipo.setForeground(Color.GRAY);

            // Botones
            JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            botonesPanel.setBackground(BLANCO);

            JButton btnLogout = igButton("Cerrar sesión", GRIS_BORDE, NEGRO);
            btnLogout.addActionListener(e -> {
                usuarioActual = null;
                cardLayout.show(mainPanel, "LOGIN");
            });
            JButton btnDesactivar = igButton("Desactivar cuenta", GRIS_BORDE, Color.RED);
            btnDesactivar.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(this,
                        "¿Desactivar tu cuenta?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (conf == JOptionPane.YES_OPTION) {
                    try {
                        usuarioActual.setEstadoCuenta(false);
                        usuarioManager.actualizarUsuario(usuarioActual);
                        usuarioActual = null;
                        cardLayout.show(mainPanel, "LOGIN");
                    } catch (IOException ex) {
                        showError(ex.getMessage());
                    }
                }
            });
            botonesPanel.add(btnLogout);
            botonesPanel.add(btnDesactivar);

            headerPerfil.add(filaStats);
            headerPerfil.add(lblNombre);
            headerPerfil.add(lblTipo);
            headerPerfil.add(botonesPanel);

            // ── Grid vacío por ahora ──────────────────────────────────────────
            JPanel gridPanel = new JPanel(new FlowLayout());
            gridPanel.setBackground(GRIS_CLARO);
            gridPanel.add(grayLabel("Aún no has publicado nada."));

            JScrollPane scroll = new JScrollPane(gridPanel);
            scroll.setBorder(null);

            panel.add(headerPerfil, BorderLayout.NORTH);
            panel.add(scroll, BorderLayout.CENTER);

        } catch (IOException e) {
            showError(e.getMessage());
        }
        return panel;
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

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private JLabel avatarLabel(String icon) {
        JLabel lbl = new JLabel(icon);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 28));
        lbl.setForeground(GRIS_BORDE);
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
        box.add(num);
        box.add(tit);
        return box;
    }
    
    private JLabel grayLabel(String texto) {
        JLabel lbl = new JLabel("  " + texto);
        lbl.setFont(FONT_NORM);
        lbl.setForeground(Color.GRAY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApp());
    }
}
