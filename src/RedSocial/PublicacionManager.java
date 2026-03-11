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
public class PublicacionManager {
    private final String raiz = "INSTA_RAIZ";
    
    public void agregarPublicacion(String username, String contenido, String rutaImagen, String tipoMultimedia) throws IOException{
        Publicacion publicacion = new Publicacion(username, contenido, rutaImagen, tipoMultimedia);
        
        RandomAccessFile rafObj = abrirInsta(username);
        rafObj.seek(rafObj.length());
        escribirPublicacion(rafObj, publicacion);
        rafObj.close();
    }
    
    private void escribirPublicacion(RandomAccessFile rafObj, Publicacion publicacion) throws IOException{
        rafObj.writeUTF(publicacion.getUserAutor());
        rafObj.writeUTF(publicacion.getContenido());
        rafObj.writeUTF(publicacion.getRutaImagen());
        rafObj.writeUTF(publicacion.getTipoMultimedia());
        rafObj.writeLong(publicacion.getFecha());
        rafObj.writeInt(publicacion.getLikes());
    }
    
    private Publicacion leerPublicacion(RandomAccessFile rafObj) throws IOException{
        Publicacion publicacion = new Publicacion();
        publicacion.setUserAutor(rafObj.readUTF());
        publicacion.setContenido(rafObj.readUTF());
        publicacion.setRutaImagen(rafObj.readUTF());
        publicacion.setTipoMultimedia(rafObj.readUTF());
        publicacion.setFecha(rafObj.readLong());
        publicacion.setLikes(rafObj.readInt());
        return publicacion;
    }
    
    public List<Publicacion> obtenerPublicaciones(String username) throws IOException{
        List<Publicacion> lista = new ArrayList<>();
        RandomAccessFile rInsta = abrirInsta(username);
        rInsta.seek(0);
        while(rInsta.getFilePointer() < rInsta.length()){
            lista.add(leerPublicacion(rInsta));
        }
        rInsta.close();
        lista.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        return lista;
    }
    
    public List<Publicacion> obtenerFeed(String miUsername, List<String> siguiendo, UserManager um) throws IOException{
        List<Publicacion> feed = new ArrayList<>();
        feed.addAll(obtenerPublicaciones(miUsername));
        for(String u : siguiendo){
            User uo = um.buscarporUsername(u);
            if(uo != null && uo.isEstadoCuenta()){
                feed.addAll(obtenerPublicaciones(u));
            }
        }
        feed.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));
        return feed;
    }
    
    public List<Publicacion> buscarPorHashtag(String hashtag, List<User> todosUsuarios) throws IOException{
        List<Publicacion> resultados = new ArrayList<>();
        for(User u : todosUsuarios){
            if(!u.isEstadoCuenta()) continue;
            for(Publicacion p : obtenerPublicaciones(u.getUsername())){
                if(p.contieneHashtag(hashtag))
                    resultados.add(p);
            }
        }
        return resultados;
    }
    
    public List<Publicacion> buscarMenciones(String username, List<User> todosUsuarios) throws IOException{
        List<Publicacion> resultados = new ArrayList<>();
        for(User u : todosUsuarios){
            if(!u.isEstadoCuenta()) continue;
            for(Publicacion p : obtenerPublicaciones(u.getUsername())){
                if(p.mencionUsuario(username))
                    resultados.add(p);
            }
        }
        return resultados;
    }
    
    private RandomAccessFile abrirInsta(String username) throws IOException{
        return new RandomAccessFile(raiz + "/" + username.toLowerCase() + "/insta.ins", "rw");
    }
}
