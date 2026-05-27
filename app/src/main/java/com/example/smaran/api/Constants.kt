package com.example.smaran.api

object Constants {
    const val BASE_URL = "https://smaranbackend-production.up.railway.app/"
//const val BASE_URL = "https://countable-tactful-legible.ngrok-free.dev/"

    const val ENDPOINT_SIGNUP = "api/auth/signup"
    const val ENDPOINT_LOGIN  = "api/auth/login"
    const val ENDPOINT_MEMORY = "api/memory"
    const val ENDPOINT_QUERY  = "api/memory/ask"

    const val DEEPGRAM_BASE_URL = "https://api.deepgram.com/"

    const val SILK_BASE_URL = "https://silk-api.rumik.ai/"
    // API keys live in local.properties → injected as BuildConfig fields at build time
}