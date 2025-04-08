package com.example.ecovision;

public class UsuarioModel {

    private int id;
    private String Nombre;
    private String Apellidos;
    private String Email;
    private String Telefono;
    private String Username;
    private String Password;

    public UsuarioModel(int id, String nombre, String apellidos, String email, String telefono, String username, String password) {
        this.id = id;
        Nombre = nombre;
        Apellidos = apellidos;
        Email = email;
        Telefono = telefono;
        Username = username;
        Password = password;
    }
}
