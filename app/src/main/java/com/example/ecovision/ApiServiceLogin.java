package com.example.ecovision;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiServiceLogin {
    @Headers("Content-Type: application/json")
    @POST("api/usuarios") // Agrega aquí la ruta de tu API
    Call<Void> Login(@Body loginModel login);
}
