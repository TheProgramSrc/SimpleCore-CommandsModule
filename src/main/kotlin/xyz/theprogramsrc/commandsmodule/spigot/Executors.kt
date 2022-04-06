package xyz.theprogramsrc.commandsmodule.spigot

import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

/**
 * Represents a command executor
 * @param commandSender The sender of the command
 * @param args The arguments of the command
 */
class CommandExecutor(val commandSender: CommandSender, val args: IndexedArguments) {

    /**
     * Gets a player from the command sender
     */
    val player = commandSender as? Player

    /**
     * Gets a console command sender from the command sender
     */
    val console = commandSender as? ConsoleCommandSender

    /**
     * Sends a message to the command sender
     * @param message The message to send
     */
    fun sendMessage(message: String) = commandSender.sendMessage(message.replace("&", "§"))
}

/**
 * Represents an argument executor
 * @param commandSender The sender of the command
 * @param args The arguments of the command
 */
class SubCommandExecutor(val commandSender: CommandSender, val args: NamedArguments) {

    /**
     * Gets a player from the command sender
     */
    val player = commandSender as? Player

    /**
     * Gets a console command sender from the command sender
     */
    val console = commandSender as? ConsoleCommandSender

    /**
     * Sends a message to the command sender
     * @param message The message to send
     */
    fun sendMessage(message: String) = commandSender.sendMessage(message.replace("&", "§"))
}