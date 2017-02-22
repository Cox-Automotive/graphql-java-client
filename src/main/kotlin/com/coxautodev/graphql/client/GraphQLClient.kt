package com.coxautodev.graphql.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder


/**
 * @author Andrew Potter
 */
class GraphQLClient @JvmOverloads constructor(private val url: String, private val configurer: (HttpRequestBase) -> Unit = {}) {

    private val mapper = ObjectMapper().registerKotlinModule()
    private val client = HttpClientBuilder.create().build()

    @JvmOverloads
    fun queryForResult(text: String, vars: Map<String, Any> = mapOf()): GraphQLResponse<Map<*, *>> = queryForResult(text, Map::class.java, vars)

    @JvmOverloads
    fun <T: Any> queryForResult(text: String, type: Class<T>, vars: Map<String, Any> = mapOf()): GraphQLResponse<T> {
        val post = HttpPost(url)
        post.entity = StringEntity(mapper.writeValueAsString(mapOf("query" to text, "variables" to vars)))
        configurer(post)

        val resp = client.execute(post)
        if (resp.statusLine.statusCode != 200) {
            throw IllegalStateException("Non-OK status code (${resp.statusLine.statusCode}): ${resp.entity.content.reader().readLines().joinToString("\n")}")
        }

        val result = mapper.readValue(resp.entity.content, GraphQLJsonResponse::class.java)
        return GraphQLResponse(mapper.convertValue(result.data, type), result.errors)
    }

    @JvmOverloads
    fun query(text: String, vars: Map<String, Any> = mapOf()): Map<*, *> = query(text, Map::class.java, vars)

    @JvmOverloads
    fun <T: Any> query(text: String, type: Class<T>, vars: Map<String, Any> = mapOf()): T {
        val result = queryForResult(text, type, vars)
        if (result.errors != null && result.errors.isNotEmpty()) {
            throw GraphQLError(result.errors)
        }

        if (result.data == null) {
            throw IllegalStateException("No errors found but data section was still empty!")
        }

        return result.data
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GraphQLJsonResponse(val data: Map<String, Any>?, val errors: List<Map<String, Any>>?)

data class GraphQLResponse<out T: Any>(val data: T?, val errors: List<Map<String, Any>>?)