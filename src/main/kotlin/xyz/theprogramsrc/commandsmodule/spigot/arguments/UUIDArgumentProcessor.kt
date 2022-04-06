package xyz.theprogramsrc.commandsmodule.spigot.arguments

class UUIDArgumentProcessor: ArgumentProcessor<String> {

    override fun validate(raw: String): Boolean = raw.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))

    override fun get(raw: String): String = raw

}