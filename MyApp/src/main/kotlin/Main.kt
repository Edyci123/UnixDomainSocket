fun main(args: Array<String>) {
    if (args.size != 2) {
        throw RuntimeException("You need to provide both, a socket path and a file path.")
    }

    val socketPath = args[0]
    val filePath = args[1]

    val server = Server(socketPath, filePath)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutdown hook triggered, stopping server...")
        server.stop()
        // TODO solve error after exiting
    })

    server.start()

}