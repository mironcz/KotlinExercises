package cz.miron.news.service

import cz.miron.news.config.ProxyConfig
import cz.miron.news.entity.Message
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
class PageReaderService(
    private val proxyConfig: ProxyConfig
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun readAndParseChannel(channelId: String): List<Message> {

        val url = "https://t.me/s/$channelId"
        val messages = mutableListOf<Message>()

        try {
            log.info("Loading page {}", url)
            val html = loadPage(url)
            val doc: Document = Jsoup.parse(html, url)
            val elements = doc.select("div.tgme_widget_message_wrap")

            log.info("Channel {}: found {} message elements", channelId, elements.size)

            for (element in elements) {
                try {
                    val message = parseMessageElement(element, channelId)
                    if (message != null) {
                        messages.add(message)
                    }
                } catch (e: Exception) {
                    log.error("Ошибка при парсинге отдельного сообщения в канале $channelId", e)
                }
            }
        } catch (e: Exception) {
            log.error("Ошибка при подключении к сайту $url: ${e.message}", e)
        }

        return messages
    }

    fun reparseMessage(channelId: String, messageId: Int): Message? {
        val cleanChannel = channelId.replace("@", "")

        try {
            val messages = readAndParseChannel(cleanChannel)
            val found = messages.find { it.messageId == messageId }
            if (found != null) {
                return found
            }
        } catch (e: Exception) {
            log.warn("Не удалось прочитать ленту канала при репарсинге: ${e.message}")
        }

        val url = "https://t.me/$cleanChannel/$messageId"
        try {
            log.info("Reparsing message via direct url: {}", url)
            val html = loadPage(url)
            val doc = Jsoup.parse(html, url)
            val element = doc.select("div.tgme_widget_message_wrap").first()
                ?: doc.select("div.tgme_widget_message").first()
                ?: return null
            return parseMessageElement(element, cleanChannel)
        } catch (e: Exception) {
            log.error("Ошибка при перечитывании сообщения $messageId канала $cleanChannel", e)
            return null
        }
    }

    private fun loadPage(url: String): String {
        val command = mutableListOf(
            "curl",
            "--silent",
            "--show-error",
            "--fail",
            "--location",
            "-A", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        if (proxyConfig.enabled) {
            log.info("Используем proxy {}:{}", proxyConfig.host, proxyConfig.port)
            command.addAll(
                listOf(
                    "--proxy",
                    "http://${proxyConfig.host}:${proxyConfig.port}",
                    "--proxy-user",
                    "${proxyConfig.username}:${proxyConfig.password}"
                )
            )
        } else {
            log.info("Proxy отключен")
        }

        command.add(url)

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val result = process.inputStream
            .bufferedReader()
            .use { it.readText() }

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("curl завершился с кодом $exitCode: $result")
        }

        return result
    }

    private fun parseMessageElement(
        element: Element,
        channelId: String
    ): Message? {

        val href = element
            .select("a.tgme_widget_message_date")
            .attr("href")

        if (href.isEmpty()) {
            return null
        }

        val idRaw = href
            .split("/")
            .filter { part -> part.matches(Regex("\\d+")) }
            .lastOrNull() ?: "0"

        val messageId = idRaw.toInt()

        val dateRaw = element
            .select("time")
            .attr("datetime")

        val localDateTime = if (dateRaw.isNotEmpty()) {
            OffsetDateTime.parse(dateRaw).toLocalDateTime()
        } else {
            LocalDateTime.now()
        }

        val textElement = element
            .select("div.tgme_widget_message_text")
            .first()

        val text = if (textElement != null) {
            val el = textElement.clone()
            // Сохраняем переносы строк через теги <br>
            el.select("br").before("__BR_PLACEHOLDER__").remove()
            // Сохраняем ссылки Telegram, превращая их в Markdown [текст](url)
            el.select("a").forEach { a ->
                val linkHref = a.attr("href")
                val linkText = a.text()
                if (linkHref.isNotEmpty()) {
                    a.replaceWith(org.jsoup.nodes.TextNode("[$linkText]($linkHref)"))
                } else {
                    a.unwrap()
                }
            }
            var rawText = el.text()
            rawText = rawText.replace("__BR_PLACEHOLDER__", "\n")
            // Убираем лишние табуляции и отступы по краям строк
            rawText.lines().joinToString("\n") { line -> line.replace(Regex("^[ \t]+"), "").trimEnd() }.trim()
        } else {
            ""
        }

        // Сбор прикрепленных медиафайлов, надежно исключая аватарку автора/канала из шапки
        val mediaUrls = mutableListOf<String>()
        val searchElement = element.clone()
        searchElement.select("div.tgme_widget_message_info, .tgme_widget_message_user_photo, .tgme_widget_message_owner_photo, .tgme_widget_message_author").remove()

        searchElement.select("div.tgme_widget_message_media, a.tgme_widget_message_photo_wrap, div.tgme_widget_message_photo, .tgme_widget_message_video_thumb").forEach { el ->
            val style = el.attr("style")
            if (style.isNotEmpty()) {
                val regex = Regex("url\\(['\"]?(.*?)['\"]?\\)")
                regex.find(style)?.groupValues?.get(1)?.let { url ->
                    if (url.isNotEmpty() && !url.contains("emoji") && !mediaUrls.contains(url)) {
                        mediaUrls.add(url)
                    }
                }
            }
            el.select("img").forEach { img ->
                val src = img.attr("src")
                if (src.isNotEmpty() && !src.contains("emoji") && !mediaUrls.contains(src)) {
                    mediaUrls.add(src)
                }
            }
        }

        searchElement.select("img").forEach { img ->
            val src = img.attr("src")
            if (src.isNotEmpty() && !src.contains("emoji") && !mediaUrls.contains(src)) {
                mediaUrls.add(src)
            }
        }

        val mediaUrl = if (mediaUrls.isNotEmpty()) mediaUrls.joinToString(";") else null

        return Message(
            messageId = messageId,
            channelId = channelId,
            date = localDateTime,
            text = text,
            mediaUrl = mediaUrl
        )
    }
}