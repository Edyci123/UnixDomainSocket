import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.File
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppTest {
    private lateinit var serverThread: Thread
    private val socketPath = "/tmp/test.sock"
    private val filePath = "/tmp/testfile.txt"
    private lateinit var file: File
    private lateinit var server: Server

    @BeforeAll
    fun startServer() {
        file = File(filePath)
        val socketFile = File(socketPath)
        if (socketFile.exists()) socketFile.delete()

        val testFile = File(filePath)
        if (testFile.exists()) testFile.writeText("")


        server = Server(socketPath, filePath)
        serverThread = thread {
            server.start()
        }

        waitForServerToStart()
    }

    @AfterAll
    fun stopServer() {
        server.stop()
        serverThread.join()

        File(socketPath).delete()
        File(filePath).delete()
        println("Server stopped and cleaned up.")
    }


    @Test
    fun testPingMessage() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            val response = sendRequest(MessageType.PING, outputStream, socket, "", 0)
            assert(response.contentLength == 0)
            assert(response.messageType == MessageType.OK)
        }
    }

    @Test
    fun testOkRequest() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            assertTrue(sendOkRequest(outputStream, socket))
        }
    }

    @Test
    fun testOkRequestWithContent() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream

            val message = "Error for ok"

            val response = sendRequest(MessageType.OK, outputStream, socket, message, message.length)

            assert(response.messageType == MessageType.ERROR)
        }
    }


    @Test
    fun testClearRequest() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            val response = sendRequest(MessageType.CLEAR, outputStream, socket, "", 0)

            assert(response.contentLength == 0)
            assert(response.messageType == MessageType.OK)
            assert(file.readText(Charsets.UTF_8).isEmpty())
        }
    }

    @Test
    fun testWriteRequest() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            val message = "This is a message"
            val response = sendRequest(MessageType.WRITE, outputStream, socket, message, message.length)
            assert(response.messageType == MessageType.OK)
            assert(response.contentLength == 0)
            assert(file.readText(Charsets.UTF_8).contains(message))
        }
    }

    @Test
    fun testWriteEmptyRequest() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            val message = ""
            val response = sendRequest(MessageType.WRITE, outputStream, socket, message, message.length)
            assert(response.messageType == MessageType.OK)
            assert(file.readText(Charsets.UTF_8).contains(message))
        }
    }

    @Test
    fun testWriteRequestThrowsErrorContentOverflow() {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
            val outputStream = socket.outputStream
            val message = "Wrong size message"
            val response = sendRequest(MessageType.WRITE, outputStream, socket, message,  message.length + 1)
            assert(response.messageType == MessageType.ERROR)
            assert(String(response.content) == "The request content length does not match the actual content size!")
        }
    }

    @Test
    fun testConcurrentRequests() {
        val numberOfThreads = 10
        val latch = CountDownLatch(numberOfThreads)

        val message = "This is a test message from thread "
        val threads = mutableListOf<Thread>()

        repeat(numberOfThreads) { i ->
            val thread = thread {
                try {
                    AFUNIXSocket.newInstance().use { socket ->
                        socket.connect(AFUNIXSocketAddress.of(File(socketPath)))
                        val outputStream = socket.outputStream
                        val response = sendRequest(MessageType.WRITE, outputStream, socket, message + i, (message + i).length)

                        assert(response.messageType == MessageType.OK)
                        assert(file.readText(Charsets.UTF_8).contains(message + i))
                    }
                } catch (e: Exception) {
                    println("Error in thread $i: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
            threads.add(thread)
        }

        latch.await()

        val fileContent = file.readText(Charsets.UTF_8)
        repeat(numberOfThreads) { i ->
            assertTrue(fileContent.contains(message + i), "Message from thread $i not found in file")
        }
    }

    private fun waitForServerToStart(timeoutMillis: Long = 5000) {
        val socketFilePath = Path(socketPath)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (socketFilePath.exists()) {
                return
            }
            Thread.sleep(100)
        }

        throw IllegalStateException("Server did not start within the timeout period")
    }

    private fun sendRequest(messageType: Int, outputStream: OutputStream, socket: AFUNIXSocket, message: String, messageLength: Int): Request {
        var request = byteArrayOf(messageType.toByte(), 0x0, 0x0, 0x0)
        request += Utils.toByteArray(messageLength)
        if (message.isNotEmpty()) {
            request += message.toByteArray(Charsets.UTF_8)
        }
        outputStream.write(request)
        outputStream.flush()

        val response = Request(socket.inputStream)
        return response
    }

    private fun sendOkRequest(outputStream: OutputStream, socket: AFUNIXSocket): Boolean {
        val request = byteArrayOf(MessageType.OK.toByte(), 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0)
        outputStream.write(request)
        outputStream.flush()

        val response = socket.inputStream.readAllBytes()

        return response.isEmpty()
    }



}
