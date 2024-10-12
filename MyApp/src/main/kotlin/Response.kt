import org.newsclub.net.unix.AFOutputStream

class Response {

    companion object {
        fun writeResponse(outputStream: AFOutputStream, type: Int, message: String) {
            var request = byteArrayOf(type.toByte(), 0x0, 0x0, 0x0)
            request += Utils.toByteArray(message.length)
            request += message.toByteArray()
            outputStream.write(request)
            outputStream.flush()
        }

        fun writeResponse(outputStream: AFOutputStream, type: Int) {
            var request = byteArrayOf(type.toByte(), 0x0, 0x0, 0x0)
            request += Utils.toByteArray(0)
            outputStream.write(request)
            outputStream.flush()
            println("Done writing")
        }


    }

}