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
public class Comentario {
    private String autor;
    private String contenido;
    private long fechaHora;
    
    public Comentario(){}
    
    public Comentario(String autor, String contenido){
        this.autor = autor;
        this.contenido = contenido;
        this.fechaHora = Calendar.getInstance().getTimeInMillis();
    }
    
    public String getFechaStr(){
        return new Date(fechaHora).toString();
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public long getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(long fechaHora) {
        this.fechaHora = fechaHora;
    }
}
