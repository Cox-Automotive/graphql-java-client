package com.coxautodev.graphql.client

/**
 * @author Andrew Potter
 */
class GraphQLError(val errors: List<Map<String, Any>>) : RuntimeException(errors.map { it.toString() }.joinToString(", "))
