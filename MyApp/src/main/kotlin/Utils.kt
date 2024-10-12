import java.nio.ByteBuffer

class Utils {
    companion object {
        fun toByteArray(value: Int): ByteArray {
            return ByteBuffer.allocate(4).putInt(value).array()
        }

    }
}