package xyz.theprogramsrc.commandsmodule.spigot.arguments

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class PlayerArgumentProcessor: ArgumentProcessor<Player> {

    override fun validate(raw: String): Boolean = Bukkit.getOnlinePlayers().any { it.name == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): Player = Bukkit.getOnlinePlayers().first { it.name == raw || it.uniqueId.toString() == raw }

}

class OfflinePlayerArgumentProcessor: ArgumentProcessor<OfflinePlayer> {

    override fun validate(raw: String): Boolean = Bukkit.getOfflinePlayers().any { it.name == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): OfflinePlayer = Bukkit.getOfflinePlayers().first { it.name == raw || it.uniqueId.toString() == raw }

}