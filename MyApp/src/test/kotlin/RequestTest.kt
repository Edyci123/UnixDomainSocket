import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.newsclub.net.unix.AFInputStream
import java.nio.ByteBuffer

class RequestTest {

    @Test
    fun shouldParseValidRequestWithCorrectContentLength() {
        val input = mockk<AFInputStream>()

        val validRequestHeader = ByteBuffer.allocate(8)
            .put(1.toByte())
            .put(ByteArray(3))
            .putInt(5)
            .array()

        val validContent = "Hello".toByteArray()

        every { input.read(ByteArray(8)) } answers {
            validRequestHeader.copyInto(firstArg())
            8
        }

        every { input.read(ByteArray(5)) } answers {
            validContent.copyInto(firstArg())
            5
        }

        val request = Request(input)

        assertEquals(1, request.messageType)
        assertEquals(5, request.contentLength)
        assertArrayEquals(validContent, request.content)
    }

    @Test
    fun shouldThrowExceptionForIncompleteHeader() {
        val input = mockk<AFInputStream>()
        val incompleteHeader = ByteArray(4)

        every { input.read(ByteArray(8)) } answers {
            incompleteHeader.copyInto(firstArg())
            4
        }

        val exception = assertThrows(Exception::class.java) {
            Request(input)
        }
        assertTrue(exception.message!!.contains("The request is not the right format!"))
    }

    @Test
    fun shouldThrowExceptionWhenContentLengthDoesNotMatchActualContentSize() {
        val input = mockk<AFInputStream>()

        val validRequestHeader = ByteBuffer.allocate(8)
            .put(1.toByte())
            .put(ByteArray(3))
            .putInt(5)
            .array()

        val invalidContent = "Hi".toByteArray()

        every { input.read(ByteArray(8)) } answers {
            validRequestHeader.copyInto(firstArg())
            8
        }

        every { input.read(ByteArray(5)) } answers {
            invalidContent.copyInto(firstArg())
            2
        }

        val exception = assertThrows(Exception::class.java) {
            Request(input)
        }
        assertTrue(exception.message!!.contains("The request content length does not match the actual content size!"))
    }

    @Test
    fun shouldThrowExceptionWhenContentIsShorterThanExpected() {
        val input = mockk<AFInputStream>()

        val validRequestHeader = ByteBuffer.allocate(8)
            .put(1.toByte())
            .put(ByteArray(3))
            .putInt(5)
            .array()

        val partialContent = ByteArray(3)

        every { input.read(ByteArray(8)) } answers {
            validRequestHeader.copyInto(firstArg())
            8
        }

        every { input.read(ByteArray(5)) } answers {
            partialContent.copyInto(firstArg())
            3
        }

        val exception = assertThrows(Exception::class.java) {
            Request(input)
        }
        assertTrue(exception.message!!.contains("The request content length does not match the actual content size!"))
    }
}