package cz.miron.news.repository

import cz.miron.news.entity.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface MessageRepository : JpaRepository<Message, Int>, JpaSpecificationExecutor<Message> {

    fun existsByMessageIdAndChannelId(
        messageId: Int?,
        channelId: String?
    ): Boolean

    @Query("select distinct m.channelId from Message m order by m.channelId")
    fun findAllChannels(): List<String>
}

// TODO pagination