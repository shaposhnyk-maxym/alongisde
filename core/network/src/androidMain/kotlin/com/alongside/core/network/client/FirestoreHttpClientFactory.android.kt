package com.alongside.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Interceptor

public fun createFirestoreHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                addInterceptor(
                    Interceptor { chain ->
                        val request = chain.request()
                        val authHeader = request.header("Authorization")
                        println("OkHttpLog: ${request.method} ${request.url}")
                        println(
                            "OkHttpLog: Authorization header = ${authHeader ?: "MISSING"} " +
                                "(length=${authHeader?.length})",
                        )
                        val requestBody = request.body
                        if (requestBody != null) {
                            val buffer = okio.Buffer()
                            requestBody.writeTo(buffer)
                            println("OkHttpLog: request body=${buffer.readUtf8()}")
                        }
                        val response = chain.proceed(request)
                        val bodyString = response.peekBody(2048).string()
                        println("OkHttpLog: response code=${response.code} body=$bodyString")
                        response
                    },
                )
            }
        }
        configureFirestoreHttpClient()
    }
