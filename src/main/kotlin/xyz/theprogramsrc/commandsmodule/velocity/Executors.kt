package xyz.theprogramsrc.commandsmodule.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import xyz.theprogramsrc.commandsmodule.objects.arguments.Arguments

/**
 * Represents a command executor
 * @param commandSource The source of the command
 * @param args The arguments of the command
 */
class CommandExecutor(val commandSource: CommandSource, val args: Arguments) {

    /**
     * Gets a player from the command source
     */
    val player = commandSource as? Player

    /**
     * Gets a console command sender from the command source
     */
    val console = commandSource as? ConsoleCommandSource

    /**
     * Sends a message to the command sender
     * @param message The message to send
     */
    fun sendMessage(message: String) = commandSource.sendMessage(Component.text(message.replace("&", "§")))
}