import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

class ServerTest {

    private lateinit var tempFile: File
    private lateinit var tempSocketFile: File
    private lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        tempDir = File.createTempFile("tempDir", "").apply { delete(); mkdir() }
        tempFile = File(tempDir, "testFile.txt")
        tempSocketFile = File.createTempFile("testSocket", ".sock").apply { delete() }
    }

    @AfterEach
    fun clean() {
        if (tempFile.exists()) tempFile.delete()
        if (tempSocketFile.exists()) tempSocketFile.delete()
        if (tempDir.exists()) tempDir.deleteRecursively()
    }

    @Test
    fun shouldThrowExceptionWhenFilePathIsNotWritable() {
        tempFile.createNewFile()
        tempFile.setWritable(false)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            Server(tempSocketFile.absolutePath, tempFile.absolutePath)
        }

        assertTrue(exception.message!!.contains("The file at ${tempFile.absolutePath} is not writable"))
    }

    @Test
    fun shouldNotThrowIOExceptionIfFileNotExists() {
        val nonExistentFile = File(tempDir, "nonexistentFile.txt")
        nonExistentFile.delete()

        assertFalse(nonExistentFile.exists())

        assertDoesNotThrow {
            Server(tempSocketFile.absolutePath, nonExistentFile.absolutePath)
        }

        assertTrue(nonExistentFile.exists())
    }

    @Test
    fun shouldCreateFileAndParentDirectoryIfTheyDoNotExist() {
        val nonExistentDir = File(tempDir, "nonexistentDir")
        val nonExistentFile = File(nonExistentDir, "newFile.txt")

        assertFalse(nonExistentDir.exists())
        assertFalse(nonExistentFile.exists())

        assertDoesNotThrow {
            Server(tempSocketFile.absolutePath, nonExistentFile.absolutePath)
        }

        assertTrue(nonExistentDir.exists())
        assertTrue(nonExistentFile.exists())

        nonExistentFile.delete()
        nonExistentDir.delete()
    }

    @Test
    fun shouldThrowExceptionWhenSocketParentDirDoesNotExist() {
        val socketPath = "/nonexistentdir/socketfile.sock"

        val exception = assertThrows(IllegalArgumentException::class.java) {
            Server(socketPath, tempFile.absolutePath)
        }

        assertTrue(exception.message!!.contains("The parent directory for the socket path does not exist"))
    }

    @Test
    fun shouldThrowExceptionWhenSocketPathIsDirectory() {
        val socketDir = File(tempDir, "socketDir")
        socketDir.mkdir()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            Server(socketDir.absolutePath, tempFile.absolutePath)
        }

        assertTrue(exception.message!!.contains("The specified socket path is a directory"))

        socketDir.delete()
    }

    @Test
    fun shouldThrowExceptionWhenFilePathIsDirectory() {
        val dir = File(tempDir, "testDir")
        dir.mkdir()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            Server(tempSocketFile.absolutePath, dir.absolutePath)
        }

        assertTrue(exception.message!!.contains("The specified file path is a directory"))

        dir.delete()
    }
}