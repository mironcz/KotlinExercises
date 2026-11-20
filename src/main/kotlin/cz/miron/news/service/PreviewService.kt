package cz.miron.news.service

import org.jsoup.Jsoup
import org.springframework.stereotype.Service

data class LinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val image: String?,
    val domain: String
)

@Service
class PreviewService {
    fun getPreview(targetUrl: String): LinkPreview {
        val parsedUrl = java.net.URI(targetUrl).toURL()
        val domain = parsedUrl.host ?: targetUrl

        try {
            val doc = Jsoup.connect(targetUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(7000)
                .followRedirects(true)
                .get()

            // Расширенный поиск заголовка
            val title = doc.select("meta[property=og:title]").attr("content")
                .ifEmpty { doc.select("meta[name=twitter:title]").attr("content") }
                .ifEmpty { doc.select("meta[name=title]").attr("content") }
                .ifEmpty { doc.select("h1").first()?.text() ?: "" }
                .ifEmpty { doc.title() }

            // Расширенный поиск описания
            val description = doc.select("meta[property=og:description]").attr("content")
                .ifEmpty { doc.select("meta[name=description]").attr("content") }
                .ifEmpty { doc.select("meta[name=twitter:description]").attr("content") }
                .ifEmpty { doc.select("p").first()?.text() ?: "" }

            // Картинка с автопреобразованием относительных путей в абсолютные
            val image = doc.select("meta[property=og:image]").attr("abs:content")
                .ifEmpty { doc.select("meta[name=twitter:image]").attr("abs:content") }
                .ifEmpty {
                    doc.select("article img, .content img, .post img, .entry-content img").first()?.attr("abs:src") ?: ""
                }

            return LinkPreview(
                url = targetUrl,
                title = title.ifEmpty { null },
                description = description.ifEmpty { null },
                image = image.ifEmpty { null },
                domain = domain
            )
        } catch (e: Exception) {
            return LinkPreview(
                url = targetUrl,
                title = null,
                description = null,
                image = null,
                domain = domain
            )
        }
    }
}