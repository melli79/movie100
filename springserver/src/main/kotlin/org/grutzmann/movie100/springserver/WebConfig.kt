package org.grutzmann.movie100.springserver

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import ulid.ULID
import java.io.IOException
import kotlin.time.Instant

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                @Throws(IOException::class)
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requestedResource = location.createRelative(resourcePath)
                    return if (requestedResource.exists() && requestedResource.isReadable) {
                        requestedResource
                    } else {
                        // Route fallback to index.html for SPA client-side navigation
                        location.createRelative("index.html")
                    }
                }
            })
    }

    object KotlinInstantToStringSerializer : ValueSerializer<Instant>() {
        override fun serialize(value :Instant?, gen :JsonGenerator, ctx :SerializationContext) {
            if (value!=null)
                gen.writeString(value.toString())
            else
                gen.writeNull()
        }
    }

    object LongToKotlinInstantDeserializer : ValueDeserializer<Instant>() {
        override fun deserialize(p:JsonParser, ctx:DeserializationContext) :Instant? {
            var value :JsonToken? = p.currentToken()
            if (value==null)
                value = p.nextToken()
            return if (value != JsonToken.VALUE_NULL)
                Instant.parse(value.asString())
            else
                null
        }
    }

    object UlidInstantSerializer : ValueSerializer<ULID>() {
        override fun serialize(value :ULID?, gen :JsonGenerator, ctxt :SerializationContext) {
            if (value!=null)
                gen.writeString(value.toString())
            else
                gen.writeNull()
        }
    }

    object UlidInstantDeserializer : ValueDeserializer<ULID?>() {
        override fun deserialize(p :JsonParser, ctxt :DeserializationContext) :ULID? {
            var value :JsonToken? = p.currentToken()
            if (value==null)
                value = p.nextToken()
            return if (value == JsonToken.VALUE_NULL)
                null
            else
                ULID.parseULID(value.asString())
        }
    }

    @Bean
    fun kotlinInstantModule() :SimpleModule = SimpleModule("KotlinInstantModule").apply {
        addSerializer(Instant::class.java, KotlinInstantToStringSerializer)
        addDeserializer(Instant::class.java, LongToKotlinInstantDeserializer)
        addSerializer(ULID::class.java, UlidInstantSerializer)
        addDeserializer(ULID::class.java, UlidInstantDeserializer)
    }
}
