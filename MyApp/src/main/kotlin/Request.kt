import org.newsclub.net.unix.AFInputStream
import java.nio.ByteBuffer

class Request(input: AFInputStream) {
    var messageType: Int = 0
    var contentLength: Int = 0
    var content = ByteArray(0)

    init {
        val buffer = ByteArray(1024)
        val bytes = input.read(buffer)
        if (bytes < 8) {
            throw Exception("The request is not the right format!")
        }
        val byteBuffer = ByteBuffer.wrap(buffer, 0, bytes)
        this.messageType = byteBuffer.get().toInt()
        byteBuffer.position(byteBuffer.position() + 3)
        contentLength = byteBuffer.int
        content = ByteArray(contentLength)
        if (contentLength > 0) {
            byteBuffer.get(content)
        }
    }

}