# UnixDomainSocket

This code implements a server using Unix domain sockets, 
designed to handle specific requests implementing the desired communication protocol. 
The Server class listens on a specified socket path and processes client requests, which can include writing and clearing a file, or pinging, and ok messages.
Upon receiving a request, the server interprets the message based on the given protocol and extracts the needed 
information like the message type and based on that it either writes content to the file or clears it. 
It also performs initial checks on file paths, permissions, and directory existence to ensure the client won't try to access forbidden paths of the system.
A thread pool is used to handle concurrent client connections, allowing efficient request processing while the server is running.