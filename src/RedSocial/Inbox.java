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
public class Inbox {
    private String emisor;
    private String receptor;
    private String contenido;
    private String tipo;
    private long fechaHora;
    private boolean leido;
    
    public Inbox(){}
    
    public Inbox(String emisor, String receptor, String contenido, String tipo){
        this.emisor = emisor;
        this.receptor = receptor;
        this.contenido = contenido.length() > 300 ? contenido.substring(0, 300) : contenido;
        this.tipo = tipo;
        this.fechaHora = Calendar.getInstance().getTimeInMillis();
        this.leido = false;
    }
    
    public String getFechaStr(){
        return new Date(fechaHora).toString();
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }

    public String getReceptor() {
        return receptor;
    }

    public void setReceptor(String receptor) {
        this.receptor = receptor;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(long fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }
}
