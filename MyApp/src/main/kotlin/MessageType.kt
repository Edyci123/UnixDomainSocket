class MessageType {
    companion object {
        const val OK = 0x1
        const val WRITE = 0x2
        const val CLEAR = 0x3
        const val ERROR = 0x4
        const val PING = 0x5
    }
}