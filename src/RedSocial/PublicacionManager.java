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
        // Verificar que el archivo existe antes de abrirlo
        File f = new File(raiz + "/" + username.toLowerCase() + "/insta.ins");
        if(!f.exists() || f.length() == 0) return lista;
        RandomAccessFile rInsta = abrirInsta(username);
        rInsta.seek(0);
        while(rInsta.getFilePointer() < rInsta.length()){
            lista.add(leerPublicacion(rInsta));
        }
        rInsta.close();
        ordenarPorFecha(lista);
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
        ordenarPorFecha(feed);
        return feed;
    }
    
    public void buscarPorHashtagRecursivo(String hashtag, List<User> usuarios, int indice, List<Publicacion> resultados) throws IOException{
        if(indice >= usuarios.size()) return;
        User user = usuarios.get(indice);
        if(user.isEstadoCuenta()){
            for(Publicacion p : obtenerPublicaciones(user.getUsername()))
                if(p.contieneHashtag(hashtag)) resultados.add(p);
        }
        buscarPorHashtagRecursivo(hashtag, usuarios, indice + 1, resultados);
    }
    
    public List<Publicacion> buscarPorHashtag(String hashtag, List<User> todosUsuarios) throws IOException{
        List<Publicacion> resultados = new ArrayList<>();
        buscarPorHashtagRecursivo(hashtag, todosUsuarios, 0, resultados);
        return resultados;
    }
    
    public void buscarMencionesRecursivo(String username, List<User> usuarios, int indice, List<Publicacion> resultados) throws IOException{
        if(indice >= usuarios.size()) return;
        User user = usuarios.get(indice);
        if(user.isEstadoCuenta()){
            for(Publicacion p : obtenerPublicaciones(user.getUsername()))
                if(p.mencionUsuario(username)) resultados.add(p);
        }
        buscarMencionesRecursivo(username, usuarios, indice + 1, resultados);
    }
    
    public List<Publicacion> buscarMenciones(String username, List<User> todosUsuarios) throws IOException{
        List<Publicacion> resultados = new ArrayList<>();
        buscarMencionesRecursivo(username, todosUsuarios, 0, resultados);
        return resultados;
    }
    
    // Ordenamiento burbuja descendente (más reciente primero)
    // contador2 empieza en contador1+1 para no comparar elementos ya procesados
    // comparación < para ordenar de mayor a menor fecha
    private void ordenarPorFecha(List<Publicacion> lista){
        for(int contador1 = 0; contador1 < lista.size() - 1; contador1++){
            for(int contador2 = contador1 + 1; contador2 < lista.size(); contador2++){
                if(lista.get(contador1).getFecha() < lista.get(contador2).getFecha()){
                    Publicacion temp = lista.get(contador1);
                    lista.set(contador1, lista.get(contador2));
                    lista.set(contador2, temp);
                }
            }
        }
    }
    
    public void darLike(String usernameAutor, long fechaHora) throws IOException {
        cambiarLike(usernameAutor, fechaHora, +1);
    }

    public void quitarLike(String usernameAutor, long fechaHora) throws IOException {
        cambiarLike(usernameAutor, fechaHora, -1);
    }

    private void cambiarLike(String usernameAutor, long fechaHora, int delta) throws IOException {
        List<Publicacion> lista = obtenerPublicaciones(usernameAutor);
        boolean cambio = false;
        for (Publicacion p : lista) {
            if (p.getFecha() == fechaHora) {
                int nuevo = p.getLikes() + delta;
                p.setLikes(nuevo < 0 ? 0 : nuevo);
                cambio = true;
                break;
            }
        }
        if (!cambio) return;
        RandomAccessFile rf = abrirInsta(usernameAutor);
        rf.setLength(0);
        for (Publicacion p : lista) escribirPublicacion(rf, p);
        rf.close();
    }

    private RandomAccessFile abrirInsta(String username) throws IOException{
        return new RandomAccessFile(raiz + "/" + username.toLowerCase() + "/insta.ins", "rw");
    }
}
