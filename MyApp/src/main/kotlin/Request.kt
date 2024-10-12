import org.newsclub.net.unix.AFInputStream
import java.nio.ByteBuffer

class Request(input: AFInputStream) {
    var messageType: Int = 0
    var content = ByteArray(0)
    var contentLength: Int = 0

    init {
        val buffer = ByteArray(8)
        val bytes = input.read(buffer)
        println(bytes)
        if (bytes < 8) {
            throw Exception("The request is not the right format!")
        }

        val byteBuffer = ByteBuffer.wrap(buffer, 0, bytes)
        this.messageType = byteBuffer.get().toInt()
        byteBuffer.position(byteBuffer.position() + 3)
        contentLength = byteBuffer.int

        val contentBuffer = ByteArray(contentLength)
        val contentBytes = input.read(contentBuffer)

        if (contentBytes != contentLength) {
            throw Exception("The request content length does not match the actual content size!")
        }

        content = ByteBuffer.wrap(contentBuffer, 0, contentBytes).array()
    }
}
