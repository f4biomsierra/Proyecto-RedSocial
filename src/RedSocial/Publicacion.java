/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package RedSocial;

import java.util.*;

/**
 *
 * @author Fabio Sierra
 */
public class Publicacion {
    private String userAutor;
    private String contenido;
    private String rutaImagen;
    private String tipoMultimedia;
    private long fecha;
    private int likes;
    
    public Publicacion(){}
    
    public Publicacion(String userAutor, String contenido, String rutaImagen, String tipoMultimedia){
        this.userAutor = userAutor;
        this.contenido = contenido.length() > 220 ? contenido.substring(0, 220) : contenido;
        this.rutaImagen = rutaImagen == null ? "" : rutaImagen;
        this.tipoMultimedia = tipoMultimedia == null ? "NINGUNA" : tipoMultimedia;
        this.fecha = Calendar.getInstance().getTimeInMillis();
        this.likes = 0;
    }
    
    public String[] getHastags(){
        List<String> lista= new ArrayList<>();
        for(String palabra : contenido.split("\\s+")){
            if(palabra.startsWith("#") && palabra.length() > 1)
                lista.add(palabra.toLowerCase());
        }
        return lista.toArray(new String[0]);
    }
    
    public String[] getMenciones(){
        List<String> lista= new ArrayList<>();
        for(String palabra : contenido.split("\\s+")){
            if(palabra.startsWith("@") && palabra.length() > 1)
                lista.add(palabra.toLowerCase());
        }
        return lista.toArray(new String[0]);
    }
    
    public boolean contieneHashtag(String tag){
        for(String hash : getHastags())
            if(hash.equalsIgnoreCase(tag)) return true;
        return false;
    }
    
    public boolean mencionUsuario(String username){
        for(String mencion : getMenciones())
            if(mencion.equalsIgnoreCase(username)) return true;
        return false;
    }
    
    public String getFechaStr(){
        return new Date(fecha).toString();
    }

    public String getUserAutor() {
        return userAutor;
    }

    public void setUserAutor(String userAutor) {
        this.userAutor = userAutor;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getRutaImagen() {
        return rutaImagen;
    }

    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }

    public String getTipoMultimedia() {
        return tipoMultimedia;
    }

    public void setTipoMultimedia(String tipoMultimedia) {
        this.tipoMultimedia = tipoMultimedia;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }
}
