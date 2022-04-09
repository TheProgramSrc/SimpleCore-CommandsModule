package xyz.theprogramsrc.commandsmodule

import xyz.theprogramsrc.commandsmodule.bungee.arguments.BungeeArgumentProcessors
import xyz.theprogramsrc.commandsmodule.spigot.arguments.BukkitArgumentProcessors
import xyz.theprogramsrc.filesmodule.config.JsonConfig
import xyz.theprogramsrc.filesmodule.utils.folder
import xyz.theprogramsrc.simplecoreapi.global.SimpleCoreAPI
import xyz.theprogramsrc.simplecoreapi.global.module.Module
import xyz.theprogramsrc.simplecoreapi.global.utils.SoftwareType
import xyz.theprogramsrc.translationsmodule.Translation
import java.io.File
import java.time.format.DateTimeFormatter

class Main: Module() {

    companion object {
        val NO_PERMISSION = Translation(
            id = "NoPermission",
            group = "Commands",
            defaultValue = "You don't have permission to use this command",
            mainColor = "&c",
        )

        val PLAYER_COMMAND = Translation(
            id = "PlayerCommand",
            group = "Commands",
            defaultValue = "This command can **only** be used by **players**",
            mainColor = "&c",
            colors = arrayOf("&e&b", "&e")
        )

        val CONSOLE_COMMAND = Translation(
            id = "ConsoleCommand",
            group = "Commands",
            defaultValue = "This command can **only** be used by **console**",
            mainColor = "&c",
            colors = arrayOf("&e&b", "&e")
        )

        val MISSING_REQUIREMENTS = Translation(
            id = "MissingRequirements",
            group = "Commands",
            defaultValue = "You didn't pass some requirements to execute the command:",
            mainColor = "&c",
        )

        val MISSING_ARGUMENT = Translation(
            id = "MissingArgument",
            group = "Commands",
            defaultValue = "The argument **{argument}** is missing!",
            mainColor = "&c",
            colors = arrayOf("&e&b")
        )

        private val config = JsonConfig(File(
            File("plugins/SimpleCoreAPI/modules/CommandsModule").folder(),
            "config.json"
        )).apply {
            add("dateFormat", "dd-MM-yyyy HH:mm:ss")
        }

        fun getDefaultDateFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern(config.getString("dateFormat") ?: "dd-MM-yyyy HH:mm:ss")

    }

    override fun onLoad() {
        if(SimpleCoreAPI.instance.softwareType == SoftwareType.UNKNOWN) {
            error("You're currently running on an unsupported server software! Please contact us at https://go.theprogramsrc.xyz/discord so we can help you further.")
        }

        if(SimpleCoreAPI.instance.softwareType in arrayOf(SoftwareType.BUNGEE, SoftwareType.WATERFALL)) { // Proxies!
            BungeeArgumentProcessors.load()
        }else{
            BukkitArgumentProcessors.load()
        }
    }

}