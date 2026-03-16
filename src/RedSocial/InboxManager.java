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
public class InboxManager {
    private final String carRaiz = "INSTA_RAIZ";
    
    public void enviarMensaje(String emisor, String receptor, String contenido, String tipo) throws IOException{
        Inbox inbox = new Inbox(emisor, receptor, contenido, tipo);
        añadirMensaje(archivoInbox(emisor), inbox);
        añadirMensaje(archivoInbox(receptor), inbox);
    }
    
    private void escribirMensaje(RandomAccessFile rInbox, Inbox inbox) throws IOException{
        rInbox.writeUTF(inbox.getEmisor());
        rInbox.writeUTF(inbox.getReceptor());
        rInbox.writeUTF(inbox.getContenido());
        rInbox.writeUTF(inbox.getTipo());
        rInbox.writeLong(inbox.getFechaHora());
        rInbox.writeBoolean(inbox.isLeido());
    }
    
    private Inbox leerMensaje(RandomAccessFile rInbox) throws IOException{
        Inbox inbox = new Inbox();
        inbox.setEmisor(rInbox.readUTF());
        inbox.setReceptor(rInbox.readUTF());
        inbox.setContenido(rInbox.readUTF());
        inbox.setTipo(rInbox.readUTF());
        inbox.setFechaHora(rInbox.readLong());
        inbox.setLeido(rInbox.readBoolean());
        return inbox;
    }
    
    private void añadirMensaje(String archivo, Inbox inbox) throws IOException{
        RandomAccessFile rInbox = new RandomAccessFile(archivo, "rw");
        rInbox.seek(rInbox.length());
        escribirMensaje(rInbox, inbox);
        rInbox.close();
    }
    
    public List<String> obtenerConversaciones(String miUsername) throws IOException{
        List<String> convs = new ArrayList<>();
        for(Inbox inbox : obtenerTodosMensajes(miUsername)){
            String otro = inbox.getEmisor().equalsIgnoreCase(miUsername)
                ? inbox.getReceptor() : inbox.getEmisor();
            if(!convs.contains(otro)){
                convs.add(otro);
            }
        }
        return convs;
    }
    
    public List<Inbox> obtenerConversacion(String yo, String otro) throws IOException{
        List<Inbox> todos = obtenerTodosMensajes(yo);
        List<Inbox> conv = new ArrayList<>();
        for(Inbox inbox : todos){
            if((inbox.getEmisor().equalsIgnoreCase(yo) && inbox.getReceptor().equalsIgnoreCase(otro)) || (inbox.getEmisor().equalsIgnoreCase(otro) && inbox.getReceptor().equalsIgnoreCase(yo))){
                conv.add(inbox);
            }
        }
        ordenarPorFecha(conv);
        return conv;
    }
    
    public int contarNoLeidosRecursivo(List<Inbox> mensajes, String username, int indice){
        if(indice >= mensajes.size()) return 0;
        Inbox in = mensajes.get(indice);
        int NoLeido = (in.getReceptor().equalsIgnoreCase(username) && !in.isLeido()) ? 1 : 0;
        return NoLeido + contarNoLeidosRecursivo(mensajes, username, indice + 1);
    }
    
    public int contarNoLeidos(String username) throws IOException{
        return contarNoLeidosRecursivo(obtenerTodosMensajes(username), username, 0);
    }
    
    public List<Inbox> obtenerTodosMensajes(String username) throws IOException{
        List<Inbox> lista = new ArrayList<>();
        File arInbox = new File(archivoInbox(username));
        if(!arInbox.exists() || arInbox.length() == 0) return lista;
        
        RandomAccessFile rafObj = new RandomAccessFile(arInbox, "r");
        while(rafObj.getFilePointer() < rafObj.length()){
            lista.add(leerMensaje(rafObj));
        }
        rafObj.close();
        return lista;
    }
    
    public void marcarLeidos(String yo, String otro) throws IOException{
        List<Inbox> todos = obtenerTodosMensajes(yo);
        boolean cambio = false;
        for(Inbox inbox : todos){
            if(inbox.getEmisor().equalsIgnoreCase(otro) && inbox.getReceptor().equalsIgnoreCase(yo) && !inbox.isLeido()){
                inbox.setLeido(true);
                cambio=true;
            }
        }
        if(cambio) reescribirInbox(yo, todos);
    }
    
    public void eliminarConversacion(String yo, String otro) throws IOException{
        List<Inbox> todos = obtenerTodosMensajes(yo);
        List<Inbox> filtrada = new ArrayList<>();
        for(Inbox in : todos){
            boolean esConv =
                    (in.getEmisor().equalsIgnoreCase(yo) && in.getReceptor().equalsIgnoreCase(otro)) || (in.getEmisor().equalsIgnoreCase(otro) && in.getReceptor().equalsIgnoreCase(yo));
            if(!esConv) filtrada.add(in);
        }
        reescribirInbox(yo, filtrada);
    }
    
    private void reescribirInbox(String username, List<Inbox> mensajes) throws IOException{
        RandomAccessFile rInbox = new RandomAccessFile(archivoInbox(username), "rw");
        rInbox.setLength(0);
        for(Inbox inbox : mensajes) escribirMensaje(rInbox, inbox);
        rInbox.close();
    }
    
    private void ordenarPorFecha(List<Inbox> lista){
        for(int contador1=0; contador1<lista.size() - 1; contador1++){
            for(int contador2=contador1 + 1; contador2<lista.size(); contador2++){
                if(lista.get(contador1).getFechaHora() > lista.get(contador2).getFechaHora()){
                    Inbox temp = lista.get(contador1);
                    lista.set(contador1, lista.get(contador2));
                    lista.set(contador2, temp);
                }
            }
        }
    }
    
    private String archivoInbox(String username){
        return carRaiz + "/" + username.toLowerCase() + "/inbox.ins";
    }
}
