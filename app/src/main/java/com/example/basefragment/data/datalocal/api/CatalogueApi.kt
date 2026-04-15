package com.example.basefragment.data.datalocal.api

import com.example.basefragment.data.model.api.CharacterResponse
import com.example.basefragment.data.model.api.PartAPI
import retrofit2.Response
import retrofit2.http.GET

interface CatalogueApi {
    @GET("api/ST183_PrincessAvatarMaker")
    suspend fun getData(): Response<CharacterResponse>
}