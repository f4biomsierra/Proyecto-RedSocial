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
public class UserManager {
    
    public enum TipoCuenta{
        PUBLICO, PRIVADO;
    }
    
    public enum Genero{
        M, F;
    }
    
    public UserManager(){
        try{
            File carRaiz = new File("INSTA_RAIZ");
            carRaiz.mkdirs();
            
            File stickerGlob = new File("INSTA_RAIZ/stickers_globales");
            stickerGlob.mkdirs();
            
            File usersFile = new File("INSTA_RAIZ/users.ins");
            if(!usersFile.exists())
                usersFile.createNewFile();
            
            initStickersGlobales();
            
        }catch (IOException e){
            System.out.println("Error" + e.getMessage());
        } 
    }
    
    private void initStickersGlobales(){
        String[] stickers = {"Feliz", "Triste", "Corazon", "Risa", "Enojado"};
        for(String sticker : stickers){
            File stFile = new File("INSTA_RAIZ/stickers_globales/" + sticker + ".stk");
            if(!stFile.exists()){
                try{
                    stFile.createNewFile();
                }catch (IOException e){
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
    
    public boolean crearUser(String username, String nombreCompleto, Genero genero, String password, int edad, File fotoPerfil, TipoCuenta tipoCuenta) throws IOException{
        if(buscarporUsername(username)!=null)
            return false;
        
        User user = new User(username.toLowerCase(), nombreCompleto, genero, password, edad, fotoPerfil, tipoCuenta);
        
        RandomAccessFile archivoUsuarios = new RandomAccessFile("INSTA_RAIZ/users.ins", "rw");
        archivoUsuarios.seek(archivoUsuarios.length());
        ingresarUsuario(archivoUsuarios, user);
        archivoUsuarios.close();
        
        crearCarpetasUsuarios(username.toLowerCase());
        return true;
    }
    
    private void ingresarUsuario(RandomAccessFile rf, User user) throws IOException{
        rf.writeUTF(user.getUsername());
        rf.writeUTF(user.getNombreCompleto());
        rf.writeUTF(user.getGenero().name());
        rf.writeUTF(user.getPassword());
        rf.writeInt(user.getEdad());
        rf.writeLong(user.getFechaRegistro());
        rf.writeBoolean(user.isEstadoCuenta());
        String rutaFoto = (user.getFotoPerfil() != null) ? user.getFotoPerfil().getAbsolutePath() : "";
        rf.writeUTF(rutaFoto);
        rf.writeUTF(user.getTipoCuenta().name());
    }
    
    private User leerUsuario(RandomAccessFile rf) throws IOException{
        User user = new User();
        user.setUsername(rf.readUTF());
        user.setNombreCompleto(rf.readUTF());
        user.setGenero(Genero.valueOf(rf.readUTF()));
        user.setPassword(rf.readUTF());
        user.setEdad(rf.readInt());
        user.setFechaRegistro(rf.readLong());
        user.setEstadoCuenta(rf.readBoolean());
        String ruta = rf.readUTF();
        user.setFotoPerfil(ruta.isEmpty() ? null : new File(ruta));
        user.setTipoCuenta(TipoCuenta.valueOf(rf.readUTF()));
        return user;
    }
    
    public User buscarporUsername(String username) throws IOException{
        RandomAccessFile archivoUsuarios = new RandomAccessFile("INSTA_RAIZ/users.ins", "r");
        while(archivoUsuarios.getFilePointer() < archivoUsuarios.length()){
            User user = leerUsuario(archivoUsuarios);
            if(user.getUsername().equalsIgnoreCase(username))
                archivoUsuarios.close();
                return user;
        }
        archivoUsuarios.close();
        return null;
    }
    
    public List<User> busquedaParcial(String query) throws IOException{
        List<User> resultados = new ArrayList<>();
        String consulta = query.toLowerCase();
        RandomAccessFile archivoUsuarios = new RandomAccessFile("INSTA_RAIZ/users.ins", "r");
        while(archivoUsuarios.getFilePointer() < archivoUsuarios.length()){
            User user = leerUsuario(archivoUsuarios);
            if(user.isEstadoCuenta() && user.getUsername().toLowerCase().contains(consulta) || user.getNombreCompleto().toLowerCase().contains(consulta)){
                resultados.add(user);
            }
        }
        archivoUsuarios.close();
        return resultados;
    }
    
    public List<User> obtenerUsuarios() throws IOException{
        List<User> listaUsuarios = new ArrayList<>();
        RandomAccessFile archivoUsuarios = new RandomAccessFile("INSTA_RAIZ/users.ins", "r");
        while(archivoUsuarios.getFilePointer() < archivoUsuarios.length()){
            listaUsuarios.add(leerUsuario(archivoUsuarios));
        }
        archivoUsuarios.close();
        return listaUsuarios;
    }
    
    public void actualizarUsuario(User actualizado) throws IOException{
        List<User> todosUsuarios = obtenerUsuarios();
        RandomAccessFile archivoUsuarios = new RandomAccessFile("INSTA_RAIZ/users.ins", "rw");
        archivoUsuarios.setLength(0);
        for(User user : todosUsuarios){
            if(user.getUsername().equalsIgnoreCase(actualizado.getUsername())){
                ingresarUsuario(archivoUsuarios, actualizado);
            } else {
                ingresarUsuario(archivoUsuarios, user);
            }
        }
        archivoUsuarios.close();
    }
    
    public String carpetaUsuario(String username){
        return "INSTA_RAIZ/" + username.toLowerCase();
    }
    
    private void crearCarpetasUsuarios(String username) throws IOException{
        File directorio = new File(carpetaUsuario(username));
        directorio.mkdirs();
        new File(carpetaUsuario(username) + "/imagenes").mkdir();
        new File(carpetaUsuario(username) + "/folders_personales").mkdir();
        new File(carpetaUsuario(username) + "/stickers_personales").mkdir();
        
        new RandomAccessFile(carpetaUsuario(username) + "/followers.ins", "rw").close();
        new RandomAccessFile(carpetaUsuario(username) + "/following.ins", "rw").close();
        new RandomAccessFile(carpetaUsuario(username) + "/insta.ins", "rw").close();
        new RandomAccessFile(carpetaUsuario(username) + "/inbox.ins", "rw").close();
        new RandomAccessFile(carpetaUsuario(username) + "/stickers.ins", "rw").close();
    }
}
