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
public class ComentarioManager {
    private final String carRaiz = "INSTA_RAIZ";
    
    private String archivoComentarios(String usernameAutor, long fechaPub){
        return carRaiz + "/" + usernameAutor.toLowerCase() + "/comentarios_" + fechaPub + ".ins";
    }
    
    public void agregarComentario(String usernameAutor, long fechaPub, String autorComentario, String contenido) throws IOException{
        Comentario comment = new Comentario(autorComentario, contenido);
        RandomAccessFile rcomentarios = new RandomAccessFile(archivoComentarios(usernameAutor, fechaPub), "rw");
        rcomentarios.seek(rcomentarios.length());
        rcomentarios.writeUTF(comment.getAutor());
        rcomentarios.writeUTF(comment.getContenido());
        rcomentarios.writeLong(comment.getFechaHora());
        rcomentarios.close();
    }
    
    public List<Comentario> obtenerComentarios(String usernameAutor, long fechaPub) throws IOException{
        List<Comentario> lista = new ArrayList<>();
        File f = new File(archivoComentarios(usernameAutor, fechaPub));
        if(!f.exists() || f.length() == 0) return lista;
        RandomAccessFile rafObj = new RandomAccessFile(f, "r");
        while(rafObj.getFilePointer() < rafObj.length()){
            Comentario c = new Comentario();
            c.setAutor(rafObj.readUTF());
            c.setContenido(rafObj.readUTF());
            c.setFechaHora(rafObj.readLong());
            lista.add(c);
        }
        rafObj.close();
        return lista;
    }
}
