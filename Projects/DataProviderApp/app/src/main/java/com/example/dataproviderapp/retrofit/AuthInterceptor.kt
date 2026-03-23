package com.example.dataproviderapp.retrofit

import com.example.dataproviderapp.jwtutils.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val originalRequest = chain.request()
        val accessToken = TokenStorage.getAccessToken()

        // Some endpoints are explicitly public on the server side (e.g. login/signup/gps-data),
        // so we must not attach Authorization there even if token is currently stored.
        val isPublicEndpoint = isPublicRequest(originalRequest)
        if (accessToken.isNullOrBlank() || isPublicEndpoint) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(newRequest)
    }

    private fun isPublicRequest(request: okhttp3.Request): Boolean {
        val urlPath = request.url.encodedPath // e.g. "/api/Persons"
        val method = request.method.uppercase()

        // POST api/refresh-tokens/login
        if (urlPath.equals("/api/refresh-tokens/login", ignoreCase = true)) return true

        // POST api/refresh-tokens/refresh
        if (urlPath.equals("/api/refresh-tokens/refresh", ignoreCase = true)) return true

        // POST api/Persons (signup) is marked [AllowAnonymous] on the server.
        if (urlPath.equals("/api/Persons", ignoreCase = true) && method == "POST") return true

        // GpsDataController has no [Authorize] attribute => public.
        if (urlPath.equals("/api/gps-data", ignoreCase = true)) return true
        if (urlPath.startsWith("/api/gps-data/", ignoreCase = true)) return true

        return false
    }
}