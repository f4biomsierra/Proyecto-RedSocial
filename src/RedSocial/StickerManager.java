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
public class StickerManager {
    private final String carRaiz = "INSTA_RAIZ";
    
    public StickerManager(){
        crearStickerGlobales();
    }
    
    private void crearStickerGlobales(){
        File dir = new File(carRaiz + "/sticker_globales");
        dir.mkdirs();
        String[] nombres = {"Feliz", "Triste", "Corazon", "Risa", "Aplauso"};
        for(String n : nombres){
            File f = new File(carRaiz + "/stickers_globales/" + n + ".stk");
            if(!f.exists()) try { f.createNewFile(); } catch (IOException e) {}
        }
    }
    
    public List<String> obtenerStickersGlobales(){
        List<String> lista = new ArrayList<>();
        File dir = new File(carRaiz + "/stickers_globales");
        File[] archivos = dir.listFiles((d, name) -> name.endsWith(".stk"));
        if(archivos != null)
            for(File f : archivos)
                lista.add(f.getName().replace(".stk", ""));
        return lista;
    }
    
    public List<String> obtenerStickersPersonales(String username) throws IOException {
        List<String> lista = new ArrayList<>();
        File f = new File(carRaiz + "/" + username + "/stickers.ins");
        if (!f.exists() || f.length() == 0) return lista;
        RandomAccessFile rf = new RandomAccessFile(f, "r");
        while (rf.getFilePointer() < rf.length()) lista.add(rf.readUTF());
        rf.close();
        return lista;
    }

    public List<String> obtenerTodos(String username) throws IOException {
        List<String> todos = new ArrayList<>(obtenerStickersGlobales());
        todos.addAll(obtenerStickersPersonales(username));
        return todos;
    }

    public void importarSticker(String username, String nombreSticker) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(
            carRaiz + "/" + username + "/stickers.ins", "rw");
        rf.seek(rf.length());
        rf.writeUTF(nombreSticker);
        rf.close();
        new File(carRaiz + "/" + username + "/stickers_personales/" + nombreSticker + ".stk")
            .createNewFile();
    }
}
