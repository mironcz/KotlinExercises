package cz.miron.news.service

import cz.miron.news.entity.Message
import cz.miron.news.repository.MessageRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val pageReaderService: PageReaderService
) {
    fun getChannels(): List<String> =
        messageRepository.findAllChannels()

    fun getMessages(
        channelId: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable
    ): Page<Message> {

        val spec = Specification<Message> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            channelId?.let {
                predicates += cb.equal(root.get<String>("channelId"), it)
            }

            from?.let {
                predicates += cb.greaterThanOrEqualTo(root.get("date"), it)
            }

            to?.let {
                predicates += cb.lessThanOrEqualTo(root.get("date"), it)
            }

            cb.and(*predicates.toTypedArray())
        }
        return messageRepository.findAll(spec, pageable)
    }

    fun reparseAndSaveMessage(id: Int): Message {
        val existing = messageRepository.findById(id)
            .orElseThrow { RuntimeException("Message not found with id: $id") }

        val channelId = existing.channelId ?: throw RuntimeException("Channel ID is null")
        val messageId = existing.messageId ?: throw RuntimeException("Message ID is null")

        val parsed = pageReaderService.reparseMessage(channelId, messageId)
            ?: throw RuntimeException("Could not reparse message from Telegram")

        existing.text = parsed.text
        existing.mediaUrl = parsed.mediaUrl
        existing.date = parsed.date

        return messageRepository.save(existing)
    }
}