package xyz.theprogramsrc.commandsmodule.bungee

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import xyz.theprogramsrc.commandsmodule.objects.arguments.Arguments

/**
 * Represents a command executor
 * @param commandSender The sender of the command
 * @param args The arguments of the command
 */
class CommandExecutor(val commandSender: CommandSender, val args: Arguments) {

    /**
     * Gets a player from the command sender
     */
    val player = commandSender as? ProxiedPlayer

    /**
     * Sends a message to the command sender
     * @param message The message to send
     */
    fun sendMessage(message: String) = commandSender.sendMessage(TextComponent(message.replace("&", "§")))
}