import org.newsclub.net.unix.AFOutputStream

class Response {

    companion object {
        fun writeResponse(outputStream: AFOutputStream, type: Int, message: String) {
            outputStream.write(type)
            for (i in 1..3) outputStream.write(0x0)
            outputStream.write(message.length)
            outputStream.write(message.toByteArray())
            outputStream.flush()
        }

        fun writeResponse(outputStream: AFOutputStream, type: Int) {
            outputStream.write(type)
            for (i in 1..3) outputStream.write(0x0)
            outputStream.write(0x0)
            outputStream.flush()
        }
    }

}