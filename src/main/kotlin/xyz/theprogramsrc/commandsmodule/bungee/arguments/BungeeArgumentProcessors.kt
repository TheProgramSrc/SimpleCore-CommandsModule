package xyz.theprogramsrc.commandsmodule.bungee.arguments

import net.md_5.bungee.api.connection.ProxiedPlayer
import xyz.theprogramsrc.commandsmodule.objects.arguments.ArgumentProcessor
import xyz.theprogramsrc.simplecoreapi.bungee.BungeeLoader

object BungeeArgumentProcessors {

    private var loaded = false

    fun load(){
        check(!loaded) { "Argument processors already loaded" }
        ArgumentProcessor.register(mapOf(
            ProxiedPlayer::class.java to ProxiedPlayerArgumentProcessor(),
        ))
    }

}

class ProxiedPlayerArgumentProcessor: ArgumentProcessor<ProxiedPlayer> {

    override fun validate(raw: String): Boolean = BungeeLoader.instance.proxy.players.any { it.name == raw || it.uniqueId.toString() == raw }

    override fun get(raw: String): ProxiedPlayer = BungeeLoader.instance.proxy.players.first { it.name == raw || it.uniqueId.toString() == raw }

}