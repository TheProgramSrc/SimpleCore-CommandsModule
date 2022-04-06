package xyz.theprogramsrc.commandsmodule.spigot

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import xyz.theprogramsrc.commandsmodule.Main
import xyz.theprogramsrc.commandsmodule.objects.CommandRequirement
import xyz.theprogramsrc.commandsmodule.objects.CommandType
import xyz.theprogramsrc.tasksmodule.spigot.SpigotTasks

/**
 * Representation of a command
 * @param name The name of the command
 * @param onExecute The function to execute when the command is executed.
 */
class Command(val name: String, val onExecute: (CommandExecutor) -> Unit = {}) {

    var description: String? = null
        private set
    var usage: String? = null
        private set
    var permission: String? = null
        private set
    var commandType: CommandType = CommandType.PLAYER_AND_CONSOLE
        private set
    var tabCompleter: (CommandSender, Array<String>) -> Collection<String> = { _, _ -> emptyList() }
        private set
    val aliases: MutableList<String> = mutableListOf()
    val subCommands: MutableList<SubCommand> = mutableListOf()
    val requirements: MutableList<CommandRequirement<CommandSender>> = mutableListOf()
    private var registered = false

    init {
        register()
    }

    /**
     * Sets the command description
     * @param description The description of the command
     * @return this command
     */
    fun setDescription(description: String): Command = this.apply {
        this.description = description
    }

    /**
     * Sets the command usage
     * @param usage The usage of the command
     * @return this command
     */
    fun setUsage(usage: String): Command = this.apply {
        this.usage = usage
    }

    /**
     * Sets the command permission to the given permission
     * @param permission The permission to set
     * @return this command
     */
    fun withPermission(permission: String): Command = this.apply {
        this.permission = permission
    }

    /**
     * Adds the given aliases to the command
     * @param aliases the aliases to add
     * @return this command
     */
    fun withAlias(vararg aliases: String): Command = this.apply {
        this.aliases.addAll(aliases)
    }

    /**
     * Makes this command to be executable only by players
     * @return this command
     */
    fun playersOnly(): Command = this.apply {
        this.commandType = CommandType.PLAYER
    }

    /**
     * Makes this command to be executable only through console
     * @return this command
     */
    fun consoleOnly(): Command = this.apply {
        this.commandType = CommandType.CONSOLE
    }

    /**
     * Adds a new requirement to the command
     * @param message The message to send to the command sender if the requirement is not met
     * @param check The function to check if the requirement is met
     */
    fun require(message: String, check: (CommandSender) -> Boolean): Command = this.apply {
        requirements.add(CommandRequirement(message, check))
    }

    /**
     * Replaces the default tab completer for this command
     * @param tabCompleter The tab completer to set
     * @return this command
     */
    fun withTabCompleter(tabCompleter: (CommandSender, Array<String>) -> Collection<String>): Command = this.apply {
        this.tabCompleter = tabCompleter
    }

    /**
     * Registers an argument to this command
     * @param signature The name and signature of the command
     * @param executor The function to execute when the argument is executed
     * @return this command
     */
    fun subCommand(signature: String, executor: (SubCommandExecutor) -> Unit): Command = this.apply {
        val name = signature.split(" ")[0]
        val arguments = signature.split(" ").drop(1).map {
            it.dropWhile { char -> char == '{' }.dropLastWhile { char -> char == '}' }
        }
        subCommands.add(SubCommand(name, arguments, executor))
    }

    /**
     * Registers this command in the server
     * @return true if the command was registered, false otherwise
     */
    private fun register(): Boolean {
        check(!registered) { "Command $name is already registered" }
        try {
            val command = object : BukkitCommand(this.name, this.description ?: "", this.usage ?: "/$name", aliases) {
                override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
                    val cmd = this@Command
                    val failedRequirements = cmd.requirements.filter { !it.check(sender) }
                    // First we check the requirements
                    if(failedRequirements.isNotEmpty()){
                        SpigotTasks.instance.runTaskAsynchronously { // Send async to prevent lag
                            sender.sendMessage(
                                Main.MISSING_REQUIREMENTS
                                    .translate()
                                    .plus("\n")
                                    .plus(failedRequirements.joinToString("\n") { "&7- &c${it.message}" })
                                    .replace("&", "§")
                            )
                        }
                        return false
                    }

                    if(args.isNotEmpty() && cmd.subCommands.isNotEmpty()){
                        val subCommandName = args.first() // Here we get the subcommand name
                        val rawArguments = args.drop(1) // Arguments provided by bukkit
                        val subCommand = cmd.subCommands.find { it.name.lowercase() == subCommandName.lowercase() && it.arguments.size == rawArguments.size } // Let's look for a sub command. And because we can specify multiple sub commands with the same name, we need to check the amount of arguments too
                        if(subCommand == null) { // No sub commands? No worries, let's run the main action
                            cmd.onExecute(CommandExecutor(sender, IndexedArguments(args.toList())))
                            return false
                        }
                        val inputArguments = mutableMapOf<String, String>() // The arguments that the API will provide to the executor
                        for(index in 0..subCommand.arguments.size) { // Here we fill the map with the arguments
                            val name = subCommand.arguments[index]
                            if(rawArguments.getOrNull(index) == null){ // Should not be null because all the arguments are required
                                SpigotTasks.instance.runTaskAsynchronously { // If we need to send a message, let's do it async!
                                    sender.sendMessage(Main.MISSING_ARGUMENT.translate(placeholders = mapOf(
                                        "argument" to name
                                    )))
                                }
                                return false
                            }
                            inputArguments[name] = rawArguments[index] // Here we add the argument to the map
                        }
                        val arguments = NamedArguments(inputArguments) // The sub commands always uses named arguments
                        subCommand.executor(SubCommandExecutor(sender, arguments)) // Now we execute it!
                    } else { // Now that we have sub commands, let's find one!
                        cmd.onExecute(CommandExecutor(sender, IndexedArguments(args.toList())))
                    }
                    return true
                }

                override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): MutableList<String> =
                    this@Command.tabCompleter(sender, args).toMutableList()
            }
            val commandMapField = Bukkit::class.java.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(Bukkit.getServer()) as CommandMap
            commandMap.register("command", command)
            registered = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}



/**
 * Representation of a SubCommand
 * @param name The name of the argument
 * @param arguments The arguments of the subCommand
 * @param executor The function to execute when the argument is executed
 *
 * Signature format:
 * {argument1} {argument2}...
 *
 * Examples:
 * - ban {player} -> The name is ban, the argument is player.
 * - ban {uuid} -> The name is ban, the argument is uuid.
 */
data class SubCommand(val name: String, val arguments: List<String>, val executor: (SubCommandExecutor) -> Unit = {})