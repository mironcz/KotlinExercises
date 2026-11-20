package cz.miron.news.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "parser")
data class ParserConfig(

    var delay: String = "10m",

    var channels: List<String> = emptyList()
)