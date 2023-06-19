package net.yakclient.components.daemon.routing

import com.durganmcbroom.artifact.resolver.simple.maven.HashType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.yakclient.boot.component.artifact.SoftwareComponentDescriptor
import net.yakclient.boot.component.artifact.SoftwareComponentRepositorySettings
import net.yakclient.boot.component.context.ContextNodeTypes
import net.yakclient.components.daemon.BootDaemon

internal fun Application.configureRouting(boot: BootDaemon) {
    routing {
        get("/") {
            call.respond("Hey, howre you?")
        }

        post("/cache") {
            val request = call.receive<CacheComponentRequest>()

            val settings = when (request.repositoryType.lowercase()) {
                "local" -> SoftwareComponentRepositorySettings.local(
                        request.repository,
                        preferredHash = HashType.SHA1
                )

                "default" -> SoftwareComponentRepositorySettings.default(
                        request.repository,
                        preferredHash = HashType.SHA1
                )

                else -> throw IllegalArgumentException("Unknown repository type: '${request.repositoryType}', must be [local, default] (case insensitive).")
            }

            val descriptor = SoftwareComponentDescriptor.parseDescription(request.request)
            checkNotNull(descriptor) { "Invalid request: '${request.request}', not properly formatted." }
            boot.cache(
                    descriptor, settings
            )

            call.respond(HttpStatusCode.OK)// TODO
        }

        put("/start") {
            val context1 : Map<String, Any> = call.receive()


            val descriptor = SoftwareComponentDescriptor(
                    checkNotNull(call.request.queryParameters["group"]) {"Invalid request, group id of component not specified in query."},
                    checkNotNull(call.request.queryParameters["artifact"]) {"Invalid request, artfact id of component not specified in query."},
                    checkNotNull(call.request.queryParameters["version"]) {"Invalid request, version of component not specified in query."},null
            )


                boot.start(descriptor, ContextNodeTypes.newValueType(context1))
            call.respond(HttpStatusCode.OK)// TODO

        }
    }
}

internal fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule.Builder().build())
        }
    }
}


internal enum class ComponentRepositoryType {
    LOCAL,
    DEFAULT
}

public data class CacheComponentRequest(
        val repositoryType: String,
        val repository: String,
        val request: String
)