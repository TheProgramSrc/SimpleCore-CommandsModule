package xyz.theprogramsrc.commandsmodule.spigot.arguments

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import xyz.theprogramsrc.commandsmodule.objects.arguments.ArgumentProcessor

object BukkitArgumentProcessors {

    private var loaded = false

    fun load(){
        check(!loaded) { "Bukkit Argument processors already loaded" }
        ArgumentProcessor.register(mapOf(
            Player::class.java to PlayerArgumentProcessor(),
            OfflinePlayer::class.java to OfflinePlayerArgumentProcessor(),
            World::class.java to WorldArgumentProcessor(),
            Location::class.java to LocationArgumentProcessor()
        ))
    }

}

class PlayerArgumentProcessor: ArgumentProcessor<Player> {

    override fun validate(raw: String): Boolean = Bukkit.getOnlinePlayers().any { it.name == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): Player = Bukkit.getOnlinePlayers().first { it.name == raw || it.uniqueId.toString() == raw }

}

class OfflinePlayerArgumentProcessor: ArgumentProcessor<OfflinePlayer> {

    override fun validate(raw: String): Boolean = Bukkit.getOfflinePlayers().any { it.name == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): OfflinePlayer = Bukkit.getOfflinePlayers().first { it.name == raw || it.uniqueId.toString() == raw }

}

class WorldArgumentProcessor: ArgumentProcessor<World> {

    override fun validate(raw: String): Boolean = Bukkit.getWorlds().any { it.name == raw}

    override fun get(raw: String): World = Bukkit.getWorlds().first { it.name == raw}

}

class LocationArgumentProcessor: ArgumentProcessor<Location> {

    override fun validate(raw: String): Boolean {
        val split = raw.split(",")
        if (split.size !in arrayOf(4,6)) return false
        if (Bukkit.getWorlds().none { it.name == split[0] }) return false
        repeat(4) {
            if(split[it].toDoubleOrNull() == null) return false
        }
        if (split.size == 6) {
            if(split[4].toFloatOrNull() == null) return false
            if(split[5].toFloatOrNull() == null) return false
        }
        return true
    }

    override fun get(raw: String): Location {
        val split = raw.split(",")
        val world = Bukkit.getWorld(split[0]) ?: throw IllegalArgumentException("World ${split[0]} not found")
        val x = split[1].toDouble()
        val y = split[2].toDouble()
        val z = split[3].toDouble()
        val yaw = if (split.size == 6) split[4].toFloat() else 0f
        val pitch = if (split.size == 6) split[5].toFloat() else 0f
        return Location(world, x, y, z, yaw, pitch)
    }

}