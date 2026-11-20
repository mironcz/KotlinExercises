package cz.miron.news.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "proxy")
data class ProxyConfig(
    val enabled: Boolean = false,
    val host: String,
    val port: Int,
    val username: String,
    val password: String
)