package cz.miron.news.entity

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "message")
data class Message(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    var channelId: String? = null,

    var messageId: Int? = null,

    var date: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var text: String? = null,

    @Column(columnDefinition = "TEXT")
    var mediaUrl: String? = null,
)