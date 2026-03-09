/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package RedSocial;

import RedSocial.UserManager.Genero;
import RedSocial.UserManager.TipoCuenta;
import java.io.*;
import java.util.*;
/**
 *
 * @author Fabio Sierra
 */
public class User {
    private String username;
    private String nombreCompleto;
    private Genero genero;
    private String password;
    private int edad;
    private long fechaRegistro;
    private boolean estadoCuenta;
    private File fotoPerfil;
    private TipoCuenta tipoCuenta;
    
    public User(){}
    
    public User(String username, String nombreCompleto, Genero genero, String password, int edad, File fotoPerfil, TipoCuenta tipoCuenta){
       this.username = username;
       this.nombreCompleto = nombreCompleto;
       this.genero = genero;
       this.password = password;
       this.edad = edad;
       this.fechaRegistro = Calendar.getInstance().getTimeInMillis();
       this.estadoCuenta = true;
       this.fotoPerfil = fotoPerfil;
       this.tipoCuenta = tipoCuenta;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public long getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(long fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public boolean isEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(boolean estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public File getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(File fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }
    
    public String getFechaRegistroString(){
        return new Date(fechaRegistro).toString();
    }
    
        
}
