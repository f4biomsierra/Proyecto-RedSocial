/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RedSocial;

import javax.swing.*;
import java.awt.*;

public class MainApp extends JFrame {
    
    public MainApp(){
        setTitle("Instagram");
        setSize(390, 844);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("Instagram"));
        add(header, BorderLayout.NORTH);
        
        JPanel panelFeed = new JPanel();
        panelFeed.setLayout(new BoxLayout(panelFeed, BoxLayout.Y_AXIS));
        JScrollPane panelScroll = new JScrollPane(panelFeed);
        add(panelScroll, BorderLayout.CENTER);
        
        JPanel navBar = new JPanel(new GridLayout(1, 5));
        navBar.add(new JButton("Home"));
        navBar.add(new JButton("Buscar"));
        navBar.add(new JButton("Post"));
        navBar.add(new JButton("Inbox"));
        navBar.add(new JButton("Perfil"));
        add(navBar, BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    public static void main(String[] args) {
        new MainApp();
    }
}
