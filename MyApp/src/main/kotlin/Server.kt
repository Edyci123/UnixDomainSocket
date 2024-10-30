import org.newsclub.net.unix.AFUNIXServerSocket
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Server(private val socketPath: String, private val filePath: String) {
    private lateinit var server: AFUNIXServerSocket
    @Volatile private var running = true
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()

    init {
        val osName = System.getProperty("os.name").lowercase()

        val maxPathLength = when {
            osName.contains("win") -> 260    // windows
            osName.contains("mac") -> 1024   // macos
            osName.contains("nix") || osName.contains("nux") -> 4096  // linux
            else -> throw IllegalArgumentException("Unsupported operating system: $osName")
        }

        val socketFile = File(socketPath)

        val absoluteSocketPath = socketFile.absolutePath
        if (absoluteSocketPath.length > maxPathLength) {
            throw IllegalArgumentException("The absolute socket path exceeds the maximum allowed length for $osName " +
                    "($maxPathLength characters). Provided path: $absoluteSocketPath")
        }

        val parentDir = socketFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            throw IllegalArgumentException("The parent directory for the socket path does not exist: ${parentDir.absolutePath}")
        }

        if (socketFile.exists() && socketFile.isDirectory) {
            throw IllegalArgumentException("The specified socket path is a directory: ${socketFile.absolutePath}")
        }

        val dataFile = File(filePath)

        val dataFileParentDir = dataFile.parentFile
        if (dataFileParentDir != null && !dataFileParentDir.exists()) {
            try {
                if (!dataFileParentDir.mkdirs()) {
                    throw IOException("Failed to create the directory: ${dataFileParentDir.absolutePath}")
                }
            } catch (e: IOException) {
                throw IOException("Could not create the directory for the file at $filePath. Please check the permissions.")
            }
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile()
            } catch (e: IOException) {
                throw IOException("Could not create the file at $filePath. Please check the permissions.")
            }
        }

        if (!dataFile.canWrite()) {
            throw IllegalArgumentException("The file at $filePath is not writable. Please check the file permissions.")
        }

        if (dataFile.isDirectory) {
            throw IllegalArgumentException("The specified file path is a directory: ${dataFile.absolutePath}")
        }
    }

    fun start() {
        val socketFile = File(socketPath)
        if (socketFile.exists()) socketFile.delete()

        server = AFUNIXServerSocket.newInstance()
        server.bind(AFUNIXSocketAddress.of(socketFile))

        println("Server started, listening on ${socketFile.absolutePath}")

        while (running) {
            val clientSocket = server.accept()
            threadPool.submit {
                handleClient(clientSocket)
            }
        }
    }

    fun stop() {
        running = false
        server.close()
        threadPool.shutdown()
    }

    private fun handleClient(clientSocket: AFUNIXSocket) {
        try {
            processRequest(clientSocket, filePath)
        } catch (e: Exception) {
            sendError(clientSocket, e.message.toString())
            e.printStackTrace()
        } finally {
            clientSocket.close()
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
            sendError(clientSocket, "Content must be empty!")
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
