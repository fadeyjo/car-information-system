package com.example.dataproviderapp.glide

import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.example.dataproviderapp.retrofit.RetrofitClient
import java.io.InputStream

@GlideModule
class AuthOkHttpGlideModule : AppGlideModule() {
    override fun registerComponents(context: android.content.Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(RetrofitClient.auth_client)
        )
    }

    override fun isManifestParsingEnabled(): Boolean = false
}

