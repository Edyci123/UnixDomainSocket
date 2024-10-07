import org.newsclub.net.unix.AFUNIXServerSocket
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.File
import java.io.IOException

class Server(private val socketPath: String, private val filePath: String) {


    fun start() {
        val socketFile = File(socketPath)
        if (socketFile.exists()) socketFile.delete()

        val server = AFUNIXServerSocket.newInstance()
        server.bind(AFUNIXSocketAddress.of(socketFile))

        println("Server started, listening on ${socketFile.absolutePath}")

        while (true) {
            val clientSocket = server.accept()
            try {
                processRequest(clientSocket, filePath)
            } catch (e: Exception) {
                println(e.stackTrace)
            } finally {
                clientSocket.close()
            }
        }

    }

    private fun processRequest(clientSocket: AFUNIXSocket, filePath: String) {
        val inputStream = clientSocket.inputStream
        val req = Request(inputStream)

        when (req.messageType) {
            MessageType.OK -> handleOk(clientSocket, req.contentLength)
            MessageType.WRITE -> handleWrite(clientSocket, req.content, filePath)
            MessageType.CLEAR -> handleClear(clientSocket, req.contentLength, filePath)
            MessageType.PING -> handlePing(clientSocket, req.contentLength)
        }


    }

    private fun handleOk(clientSocket: AFUNIXSocket, contentLength: Int) {
        if (contentLength > 0) {
            sendError(clientSocket, "Content length must be 0!")
            return
        }
    }

    private fun handleWrite(clientSocket: AFUNIXSocket, content: ByteArray, filePath: String) {
        try {
            val file = File(filePath)
            file.appendBytes(content)
            sendOk(clientSocket)
        } catch (e: IOException) {
            sendError(clientSocket, "Couldn't write to the file!")
        }
    }

    private fun handleClear(clientSocket: AFUNIXSocket, contentLength: Int, filePath: String) {
        if (contentLength > 0) {
            sendError(clientSocket, "Content length must be 0!")
            return
        }

        try {
            val file = File(filePath)
            file.writeText("")
            sendOk(clientSocket)
        } catch (e: IOException) {
            sendError(clientSocket, "Couldn't clear the file!")
        }
    }

    private fun handlePing(clientSocket: AFUNIXSocket, contentLength: Int) {
        if (contentLength > 0) {
            sendError(clientSocket, "Content length must be 0!")
            return
        }
        sendOk(clientSocket)
    }

    private fun sendOk(clientSocket: AFUNIXSocket) {
        val outputStream = clientSocket.outputStream
        Response.writeResponse(outputStream, MessageType.OK)
    }

    private fun sendError(clientSocket: AFUNIXSocket, err: String) {
        val outputStream = clientSocket.outputStream
        Response.writeResponse(outputStream, MessageType.ERROR, err)
    }

}