package com.example.ecovision;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("api/Cliente") // Agrega aquí la ruta de tu API
    Call<Void> CrearCliente(@Body UsuarioModel usuario);
}
