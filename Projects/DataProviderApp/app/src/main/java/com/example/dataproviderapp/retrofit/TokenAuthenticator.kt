package com.example.dataproviderapp.retrofit

import com.example.dataproviderapp.api.RefreshTokenApi
import com.example.dataproviderapp.dto.requests.RefreshTokensRequest
import com.example.dataproviderapp.jwtutils.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val refreshApi: RefreshTokenApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.priorResponse != null) {
            return null
        }

        val request = response.request
        val urlPath = request.url.encodedPath
        val method = request.method.uppercase()

        // If we get 401 for an endpoint that is supposed to be public,
        // don't attempt refresh (and don't risk refresh-loops).
        if (isPublicRequest(urlPath, method)) return null

        // If refresh itself returned 401, do not try refresh again.
        if (urlPath.equals("/api/refresh-tokens/refresh", ignoreCase = true)) return null

        val refreshToken = TokenStorage.getRefreshToken()
            ?: return null

        val refreshResponse = runBlocking {
            try {
                refreshApi.refresh(RefreshTokensRequest(refreshToken))
            } catch (e: Exception) {
                null
            }
        }

        if (refreshResponse?.isSuccessful == true) {

            val body = refreshResponse.body() ?: return null

            TokenStorage.saveTokens(
                body.accessToken,
                body.refreshToken
            )

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .build()
        }

        TokenStorage.clear()
        return null
    }

    private fun isPublicRequest(urlPath: String, method: String): Boolean {
        // POST api/refresh-tokens/login
        if (urlPath.equals("/api/refresh-tokens/login", ignoreCase = true)) return true

        // POST api/Persons (signup) is marked [AllowAnonymous] on the server.
        if (urlPath.equals("/api/Persons", ignoreCase = true) && method == "POST") return true

        // GpsDataController has no [Authorize] attribute => public.
        if (urlPath.equals("/api/gps-data", ignoreCase = true)) return true
        if (urlPath.startsWith("/api/gps-data/", ignoreCase = true)) return true

        return false
    }
}