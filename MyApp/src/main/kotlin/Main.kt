import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    if (args.size != 2) {
        throw RuntimeException("You need to provide both, a socket path and a file path.")
    }

    val socketPath = args[0]
    val filePath = args[1]

    val server = Server(socketPath, filePath)
    val serverShutdownSignal = CompletableDeferred<Unit>()

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutdown hook triggered, stopping server...")
        server.stop()
        serverShutdownSignal.complete(Unit)
        // TODO solve error after exiting
    })

    server.start()
    serverShutdownSignal.await()


}