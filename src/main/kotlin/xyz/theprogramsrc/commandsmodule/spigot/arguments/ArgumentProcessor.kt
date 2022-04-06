package xyz.theprogramsrc.commandsmodule.spigot.arguments

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.UUID

interface ArgumentProcessor<T> {

    companion object {
        private val processors = mutableMapOf<Class<*>, ArgumentProcessor<*>>(
            String::class.java to StringArgumentProcessor(),
            Char::class.java to CharArgumentProcessor(),
            Int::class.java to IntArgumentProcessor(),
            Double::class.java to DoubleArgumentProcessor(),
            Float::class.java to FloatArgumentProcessor(),
            Long::class.java to LongArgumentProcessor(),
            ShortArgumentProcessor::class.java to ShortArgumentProcessor(),
            ByteArgumentProcessor::class.java to ByteArgumentProcessor(),
            Boolean::class.java to BooleanArgumentProcessor(),
            UUID::class.java to UUIDArgumentProcessor(),
            LocalDateTime::class.java to DateTimeArgumentProcessor(),
            Player::class.java to PlayerArgumentProcessor(),
            OfflinePlayer::class.java to OfflinePlayerArgumentProcessor(),
            World::class.java to WorldArgumentProcessor(),
            Location::class.java to LocationArgumentProcessor()
        )

        fun register(type: Class<*>, processor: ArgumentProcessor<*>) = apply {
            processors[type] = processor
        }

        fun get(type: Class<*>): ArgumentProcessor<*> = processors[type] ?: throw IllegalArgumentException("Cannot find a processor for type $type. Please contact our Team at: https://theprogramsrc.xyz")
    }

    fun validate(raw: String): Boolean

    fun get(raw: String): T
}