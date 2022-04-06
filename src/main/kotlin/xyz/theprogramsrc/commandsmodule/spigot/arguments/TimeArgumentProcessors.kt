package xyz.theprogramsrc.commandsmodule.spigot.arguments

import xyz.theprogramsrc.commandsmodule.Main
import java.time.LocalDateTime

class DateTimeArgumentProcessor : ArgumentProcessor<LocalDateTime> {

    override fun validate(raw: String): Boolean = Main.getDefaultDateFormatter().parse(raw, LocalDateTime::from) != null

    override fun get(raw: String): LocalDateTime = LocalDateTime.parse(raw, Main.getDefaultDateFormatter())

}

