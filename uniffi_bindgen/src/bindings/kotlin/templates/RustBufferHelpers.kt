// Helpers for reading primitive data types from a bytebuffer.

fun<T> liftFromRustBuffer(rbuf: RustBuffer.ByValue, readItem: (ByteBuffer) -> T): T {
    val buf = rbuf.asByteBuffer()!!
    try {
       val item = readItem(buf)
       if (buf.hasRemaining()) {
           throw RuntimeException("junk remaining in buffer after lifting, something is very wrong!!")
       }
       return item
    } finally {
        RustBuffer.free(rbuf)
    }
}

fun<T> lowerIntoRustBuffer(v: T, writeItem: (T, RustBufferBuilder) -> Unit): RustBuffer.ByValue {
    // TODO: maybe we can calculate some sort of initial size hint?
    val buf = RustBufferBuilder()
    try {
        writeItem(v, buf)
        return buf.finalize()
    } catch (e: Throwable) {
        buf.discard()
        throw e
    }
}

// For every type used in the interface, we provide helper methods for conveniently
// lifting and lowering that type from C-compatible data, and for reading and writing
// values of that type in a buffer.

{% for typ in ci.iter_types() %}
{% let type_name = typ.canonical_name()|class_name_kt %}
{%- match typ -%}

{% when Type::Boolean -%}

fun Boolean.Companion.lift(v: Byte): Boolean {
    return v.toInt() != 0
}

fun Boolean.Companion.read(buf: ByteBuffer): Boolean {
    return Boolean.lift(buf.get())
}

fun Boolean.lower(): Byte {
    return if (this) 1.toByte() else 0.toByte()
}

fun Boolean.write(buf: RustBufferBuilder) {
    buf.putByte(this.lower())
}

{% when Type::Int8 -%}

fun Byte.Companion.lift(v: Byte): Byte {
    return v
}

fun Byte.Companion.read(buf: ByteBuffer): Byte {
    return buf.get()
}

fun Byte.lower(): Byte {
    return this
}

fun Byte.write(buf: RustBufferBuilder) {
    buf.putByte(this)
}

{% when Type::Int16 -%}

fun Short.Companion.lift(v: Short): Short {
    return v
}

fun Short.Companion.read(buf: ByteBuffer): Short {
    return buf.getShort()
}

fun Short.lower(): Short {
    return this
}

fun Short.write(buf: RustBufferBuilder) {
    buf.putShort(this)
}

{% when Type::Int32 -%}

fun Int.Companion.lift(v: Int): Int {
    return v
}

fun Int.Companion.read(buf: ByteBuffer): Int {
    return buf.getInt()
}

fun Int.lower(): Int {
    return this
}

fun Int.write(buf: RustBufferBuilder) {
    buf.putInt(this)
}

{% when Type::Int64 -%}

fun Long.Companion.lift(v: Long): Long {
    return v
}

fun Long.Companion.read(buf: ByteBuffer): Long {
    return buf.getLong()
}

fun Long.lower(): Long {
    return this
}

fun Long.write(buf: RustBufferBuilder) {
    buf.putLong(this)
}

{% when Type::UInt8 -%}

@ExperimentalUnsignedTypes
fun UByte.Companion.lift(v: Byte): UByte {
    return v.toUByte()
}

@ExperimentalUnsignedTypes
fun UByte.Companion.read(buf: ByteBuffer): UByte {
    return UByte.lift(buf.get())
}

@ExperimentalUnsignedTypes
fun UByte.lower(): Byte {
    return this.toByte()
}

@ExperimentalUnsignedTypes
fun UByte.write(buf: RustBufferBuilder) {
    buf.putByte(this.toByte())
}

{% when Type::UInt16 -%}

@ExperimentalUnsignedTypes
fun UShort.Companion.lift(v: Short): UShort {
    return v.toUShort()
}

@ExperimentalUnsignedTypes
fun UShort.Companion.read(buf: ByteBuffer): UShort {
    return UShort.lift(buf.getShort())
}

@ExperimentalUnsignedTypes
fun UShort.lower(): Short {
    return this.toShort()
}

@ExperimentalUnsignedTypes
fun UShort.write(buf: RustBufferBuilder) {
    buf.putShort(this.toShort())
}

{% when Type::UInt32 -%}

@ExperimentalUnsignedTypes
fun UInt.Companion.lift(v: Int): UInt {
    return v.toUInt()
}

@ExperimentalUnsignedTypes
fun UInt.Companion.read(buf: ByteBuffer): UInt {
    return UInt.lift(buf.getInt())
}

@ExperimentalUnsignedTypes
fun UInt.lower(): Int {
    return this.toInt()
}

@ExperimentalUnsignedTypes
fun UInt.write(buf: RustBufferBuilder) {
    buf.putInt(this.toInt())
}

{% when Type::UInt64 -%}

@ExperimentalUnsignedTypes
fun ULong.Companion.lift(v: Long): ULong {
    return v.toULong()
}

@ExperimentalUnsignedTypes
fun ULong.Companion.read(buf: ByteBuffer): ULong {
    return ULong.lift(buf.getLong())
}

@ExperimentalUnsignedTypes
fun ULong.lower(): Long {
    return this.toLong()
}

@ExperimentalUnsignedTypes
fun ULong.write(buf: RustBufferBuilder) {
    buf.putLong(this.toLong())
}

{% when Type::Float32 -%}

fun Float.Companion.lift(v: Float): Float {
    return v
}

fun Float.Companion.read(buf: ByteBuffer): Float {
    return buf.getFloat()
}

fun Float.lower(): Float {
    return this
}

fun Float.write(buf: RustBufferBuilder) {
    buf.putFloat(this)
}

{% when Type::Float64 -%}

fun Double.Companion.lift(v: Double): Double {
    return v
}

fun Double.Companion.read(buf: ByteBuffer): Double {
    val v = buf.getDouble()
    return v
}

fun Double.lower(): Double {
    return this
}

fun Double.write(buf: RustBufferBuilder) {
    buf.putDouble(this)
}

{% when Type::String -%}

fun String.Companion.lift(rbuf: RustBuffer.ByValue): String {
    try {
        val byteArr = ByteArray(rbuf.len)
        rbuf.asByteBuffer()!!.get(byteArr)
        return byteArr.toString(Charsets.UTF_8)
    } finally {
        RustBuffer.free(rbuf)
    }
}

fun String.Companion.read(buf: ByteBuffer): String {
    val len = buf.getInt()
    val byteArr = ByteArray(len)
    buf.get(byteArr)
    return byteArr.toString(Charsets.UTF_8)
}

fun String.lower(): RustBuffer.ByValue {
    val byteArr = this.toByteArray(Charsets.UTF_8)
    // Ideally we'd pass these bytes to `ffi_bytebuffer_from_bytes`, but doing so would require us
    // to copy them into a JNA `Memory`. So we might as well directly copy then into a `RustBuffer`.
    val rbuf = RustBuffer.alloc(byteArr.size)
    rbuf.asByteBuffer()!!.put(byteArr)
    return rbuf
}

fun String.write(buf: RustBufferBuilder) {
    val byteArr = this.toByteArray(Charsets.UTF_8)
    buf.putInt(byteArr.size)
    buf.put(byteArr)
}

{% when Type::Optional with (inner_type) -%}
{% let inner_type_name = inner_type|type_kt %}

fun lift{{ type_name }}(rbuf: RustBuffer.ByValue): {{ inner_type_name }}? {
    return liftFromRustBuffer(rbuf) { buf ->
        read{{ type_name }}(buf)
    }
}

fun read{{ type_name }}(buf: ByteBuffer): {{ inner_type_name }}? {
    if (buf.get().toInt() == 0) {
        return null
    }
    return {{ "buf"|read_kt(inner_type) }}
}

fun lower{{ type_name }}(v: {{ inner_type_name }}?): RustBuffer.ByValue {
    return lowerIntoRustBuffer(v) { v, buf ->
        write{{ type_name }}(v, buf)
    }
}

fun write{{ type_name }}(v: {{ inner_type_name }}?, buf: RustBufferBuilder) {
    if (v === null) {
        buf.putByte(0)
    } else {
        buf.putByte(1)
        {{ "v"|write_kt("buf", inner_type) }}
    }
}

{% when Type::Sequence with (inner_type) -%}
{% let inner_type_name = inner_type|type_kt %}

fun lift{{ type_name }}(rbuf: RustBuffer.ByValue): List<{{ inner_type_name }}> {
    return liftFromRustBuffer(rbuf) { buf ->
        read{{ type_name }}(buf)
    }
}

fun read{{ type_name }}(buf: ByteBuffer): List<{{ inner_type_name }}> {
    val len = buf.getInt()
    return List<{{ inner_type|type_kt }}>(len) {
        {{ "buf"|read_kt(inner_type) }}
    }
}

fun lower{{ type_name }}(v: List<{{ inner_type_name }}>): RustBuffer.ByValue {
    return lowerIntoRustBuffer(v) { v, buf ->
        write{{ type_name }}(v, buf)
    }
}

fun write{{ type_name }}(v: List<{{ inner_type_name }}>, buf: RustBufferBuilder) {
    buf.putInt(v.size)
    v.forEach {
        {{ "it"|write_kt("buf", inner_type) }}
    }
}

{% when Type::Map with (inner_type) -%}
{% let inner_type_name = inner_type|type_kt %}

fun lift{{ type_name }}(rbuf: RustBuffer.ByValue): Map<String, {{ inner_type_name }}> {
    return liftFromRustBuffer(rbuf) { buf ->
        read{{ type_name }}(buf)
    }
}

fun read{{ type_name }}(buf: ByteBuffer): Map<String, {{ inner_type_name }}> {
    // TODO: Once Kotlin's `buildMap` API is stabilized we should use it here.
    val items : MutableMap<String, {{ inner_type_name }}> = mutableMapOf()
    val len = buf.getInt()
    repeat(len) {
        val k = String.read(buf)
        val v = {{ "buf"|read_kt(inner_type) }}
        items[k] = v
    }
    return items
}

fun lower{{ type_name }}(m: Map<String, {{ inner_type_name }}>): RustBuffer.ByValue {
    return lowerIntoRustBuffer(m) { m, buf ->
        write{{ type_name }}(m, buf)
    }
}

fun write{{ type_name }}(v: Map<String, {{ inner_type_name }}>, buf: RustBufferBuilder) {
    buf.putInt(v.size)
    v.forEach { k, v ->
        k.write(buf)
        {{ "v"|write_kt("buf", inner_type) }}
    }
}

{% when Type::Enum with (enum_name) -%}
{# Helpers for Enum types are defined inline with the Enum class #}

{% when Type::Record with (record_name) -%}
{# Helpers for Record types are defined inline with the Record class #}

{% when Type::Object with (object_name) -%}
{# Object types cannot be lifted, lowered or serialized (yet) #}

{% when Type::Error with (error_name) -%}
{# Error types cannot be lifted, lowered or serialized (yet) #}

{% endmatch %}
{% endfor %}