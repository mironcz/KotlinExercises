package cz.miron.news.controller

import cz.miron.news.service.LinkPreview
import cz.miron.news.service.PreviewService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class PreviewController(
    private val previewService: PreviewService
) {
    @GetMapping("/preview")
    fun getPreview(@RequestParam url: String): LinkPreview {
        return previewService.getPreview(url)
    }
}