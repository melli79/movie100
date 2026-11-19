package org.grutzmann.movie100.springserver

import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.relational.core.dialect.AnsiDialect
import org.springframework.data.relational.core.dialect.Dialect
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations

object SqliteJdbcDialect : JdbcDialect, Dialect by AnsiDialect.INSTANCE

@Configuration
class DatabaseConfig : AbstractJdbcConfiguration() {
    override fun jdbcDialect(operations :NamedParameterJdbcOperations) :JdbcDialect {
        return SqliteJdbcDialect
    }
}
