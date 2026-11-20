package cz.miron.news.controller

import cz.miron.news.entity.Message
import cz.miron.news.service.MessageService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class MessageController(
    private val messageService: MessageService
) {

    @GetMapping("/channels")
    fun getChannels(): List<String> =
        messageService.getChannels()

    @GetMapping("/messages")
    fun getMessages(
        @RequestParam(required = false)
        channelId: String?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,

        @RequestParam(defaultValue = "0")
        page: Int,

        @RequestParam(defaultValue = "50")
        size: Int
    ): Page<Message> {
        val pageable = PageRequest.of(page, size, Sort.by("date").descending())

        return messageService.getMessages(channelId, from, to, pageable)
    }

    @PostMapping("/messages/{id}/reparse")
    fun reparseMessage(@PathVariable id: Int): Message {
        return messageService.reparseAndSaveMessage(id)
    }
}