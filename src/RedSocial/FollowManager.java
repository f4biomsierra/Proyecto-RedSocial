/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package RedSocial;

import java.io.*;
import java.util.*;
/**
 *
 * @author Fabio Sierra
 */

public class FollowManager {

    private final String raiz = "INSTA_RAIZ";

    public boolean seguir(String miUsername, String targetUsername) throws IOException {
        if (yaSigo(miUsername, targetUsername)) return false;
        agregarEnArchivo(archivoFollowing(miUsername), targetUsername);
        agregarEnArchivo(archivoFollowers(targetUsername), miUsername);
        return true;
    }

    public boolean dejarDeSeguir(String miUsername, String targetUsername) throws IOException {
        boolean a = eliminarDeArchivo(archivoFollowing(miUsername), targetUsername);
        boolean b = eliminarDeArchivo(archivoFollowers(targetUsername), miUsername);
        return a && b;
    }

    public boolean yaSigo(String yo, String target) throws IOException {
        return obtenerLista(archivoFollowing(yo)).contains(target.toLowerCase());
    }

    public List<String> obtenerFollowers(String username) throws IOException {
        return obtenerLista(archivoFollowers(username));
    }

    public List<String> obtenerFollowing(String username) throws IOException {
        return obtenerLista(archivoFollowing(username));
    }

    public void amigosEnComunRecursivo(List<String> miFollowing, List<String> otroFollowing, int indice, List<String> comunes) {
        if (indice >= miFollowing.size()) return;

        String usuario = miFollowing.get(indice);
        if (otroFollowing.contains(usuario))
            comunes.add(usuario);

        // Llamada recursiva con el siguiente índice
        amigosEnComunRecursivo(miFollowing, otroFollowing, indice + 1, comunes);
    }

    public List<String> obtenerAmigosEnComun(String yo, String otro) throws IOException {
        List<String> miFollowing   = obtenerFollowing(yo);
        List<String> otroFollowing = obtenerFollowing(otro);
        List<String> comunes       = new ArrayList<>();
        amigosEnComunRecursivo(miFollowing, otroFollowing, 0, comunes);
        return comunes;
    }

    private void agregarEnArchivo(String archivo, String username) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(archivo, "rw");
        rf.seek(rf.length());
        rf.writeUTF(username.toLowerCase());
        rf.close();
    }

    private boolean eliminarDeArchivo(String archivo, String target) throws IOException {
        List<String> lista = obtenerLista(archivo);
        boolean eliminado = lista.remove(target.toLowerCase());
        if (!eliminado) return false;
        RandomAccessFile rf = new RandomAccessFile(archivo, "rw");
        rf.setLength(0);
        for (String u : lista) rf.writeUTF(u);
        rf.close();
        return true;
    }

    private List<String> obtenerLista(String archivo) throws IOException {
        List<String> lista = new ArrayList<>();
        File f = new File(archivo);
        if (!f.exists() || f.length() == 0) return lista;
        RandomAccessFile rf = new RandomAccessFile(archivo, "r");
        while (rf.getFilePointer() < rf.length()) lista.add(rf.readUTF());
        rf.close();
        return lista;
    }

    private String archivoFollowers(String username) {
        return raiz + "/" + username.toLowerCase() + "/followers.ins";
    }

    private String archivoFollowing(String username) {
        return raiz + "/" + username.toLowerCase() + "/following.ins";
    }
}

