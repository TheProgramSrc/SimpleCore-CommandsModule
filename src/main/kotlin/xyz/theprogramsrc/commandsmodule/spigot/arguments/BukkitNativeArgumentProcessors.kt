package xyz.theprogramsrc.commandsmodule.spigot.arguments

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

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