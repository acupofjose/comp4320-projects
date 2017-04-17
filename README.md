# COMP 4320 Intro To Networks Project 1

##### GROUP 10 - Cameron Schaerfl (crs0051), Joseph Schultz (jjs0021), Erich Wu (ejw0013)

### Introduction
This project is adapted from the code provided in class outlining basic UDP packet transmission between a client and a server using UDP datagram packets. The approach is such that the server binds to an established port and the client sends request to the server. The server then responds to the client by servering the file using segmentation. The client then reassembles the file using the checksum and sequence number provided by the server in the header of each packet.

### Header Structure
Our header structure is 4 bytes. The checksum and sequence are both 2 bytes. This allows for 124 bytes of data to be sent in each packet.

		|          |            |              | 
		| CHECKSUM | SEQUENCE # | DATA PAYLOAD |
		|__________|____________|______________|

					   PACKET_SIZE

### Compiling

For ease of use we did not use package names. To compile navigate to the src directory and execute the following command: `javac *.java`

### Running (Command Line Arguments)

Both the client and server are initialized through the main class. **The server must be run before the client.**

#### To Run the Server:

`java Main -run server` this will run the server with default host (localhost) and default port(10036)

`java Main -run server [options]`

##### Server Options:

`-host HOSTNAME`
* Bind server socket on provided HOSTNAME the default host is localhost.

`-port PORT`
* Bind server socket on provided PORT the default port is 10036 assigned to our group

#### To Run the Client:

`java Main -run client` this will run the client with default host (localhost) and default port(10036). The default request file is TestFile.html and default outputfile is reassembled.html.

`java Main -run client [options]`

##### Client Options:

`-host HOSTNAME`
* Attempt to request the request file from the server on the provided HOSTNAME default is localhost. This should be the same hostname the server socket is bound to.

`-port PORT`
* Attempt to request the request file from the server on the provided PORT default is 10036. This should be the same port the server socket is bound to.

`-gremlin GREMLINCHANCE`
* Initialize Gremlin with GREMLINCHANCE double between 0 and 1 to corrupt packets. Default is 0.

`-rfile REQUESTFILE`
* Attempt to request the REQUESTFILE from the server. Default request file is TestFile.html

`-ofile OUTFILE`
* Print reassembled packet data to OUTFILE. Default out file is reassembled.html. The machine memory may need to be flushed before the out file is written.

### Conclusion

This was an excellent introduction to basic UDP communication between a client and server. It was also a good refresher on manipulating byte arrays and buffers.
