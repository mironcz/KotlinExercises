package cz.miron.news.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class LiquibaseConfig {

    @Bean
    fun liquibase(dataSource: DataSource): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        // Укажите точный путь к вашему мастер-файлу
        liquibase.changeLog = "classpath:db/changelog/db.changelog-master.yaml"
        return liquibase
    }
}