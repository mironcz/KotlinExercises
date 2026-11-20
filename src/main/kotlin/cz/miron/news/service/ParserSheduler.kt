package cz.miron.news.service

import cz.miron.news.config.ParserConfig
import cz.miron.news.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ParserScheduler(
    private val parserConfig: ParserConfig,
    private val pageReaderService: PageReaderService,
    private val messageRepository: MessageRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${parser.delay}")
    fun scanChannels() {
        log.info("Starting channels scan")

        parserConfig.channels.forEach { channelId ->
            try {
                log.info("Processing channel {}", channelId)

                val parsedMessages = pageReaderService.readAndParseChannel(channelId)
                log.info("Channel {} loaded, found {} messages", channelId, parsedMessages.size)

                var savedCount = 0

                parsedMessages.forEach { message ->
                    val exists = messageRepository.existsByMessageIdAndChannelId(
                        message.messageId,
                        message.channelId
                    )

                    if (!exists) {
                        messageRepository.save(message)
                        savedCount++
                    }
                }

                log.info("Parsing and saving finished for channel {}, saved {} new messages", channelId, savedCount)

            } catch (e: Exception) {
                log.error("Failed to process channel {}", channelId, e)
            }
        }

        log.info("Channels scan finished")
    }
}