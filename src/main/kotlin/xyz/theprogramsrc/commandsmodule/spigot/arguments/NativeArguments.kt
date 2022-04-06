package xyz.theprogramsrc.commandsmodule.spigot.arguments

class StringArgumentProcessor: ArgumentProcessor<String> {

    override fun validate(raw: String): Boolean = true

    override fun get(raw: String): String = raw

}

class IntArgumentProcessor: ArgumentProcessor<Int> {

    override fun validate(raw: String): Boolean = raw.toIntOrNull() != null

    override fun get(raw: String): Int = raw.toInt()

}

class DoubleArgumentProcessor: ArgumentProcessor<Double> {

    override fun validate(raw: String): Boolean = raw.toDoubleOrNull() != null

    override fun get(raw: String): Double = raw.toDouble()

}

class FloatArgumentProcessor: ArgumentProcessor<Float> {

    override fun validate(raw: String): Boolean = raw.toFloatOrNull() != null

    override fun get(raw: String): Float = raw.toFloat()

}

class LongArgumentProcessor: ArgumentProcessor<Long> {

    override fun validate(raw: String): Boolean = raw.toLongOrNull() != null

    override fun get(raw: String): Long = raw.toLong()

}

class ShortArgumentProcessor: ArgumentProcessor<Short> {

    override fun validate(raw: String): Boolean = raw.toShortOrNull() != null

    override fun get(raw: String): Short = raw.toShort()

}

class ByteArgumentProcessor: ArgumentProcessor<Byte> {

    override fun validate(raw: String): Boolean = raw.toByteOrNull() != null

    override fun get(raw: String): Byte = raw.toByte()

}

class BooleanArgumentProcessor: ArgumentProcessor<Boolean> {

    override fun validate(raw: String): Boolean = raw.toBoolean()

    override fun get(raw: String): Boolean = raw.toBoolean()

}

class CharArgumentProcessor: ArgumentProcessor<Char> {

    override fun validate(raw: String): Boolean = raw.length == 1

    override fun get(raw: String): Char = raw[0]

}