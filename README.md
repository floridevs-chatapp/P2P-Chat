# Java P2P Swing Chat

A simple, peer-to-peer (P2P) chat application built in Java using the Swing library for the graphical user interface. This application allows two users to connect directly and chat over a network without a central server.

## 🚀 Features

* **Server-less (P2P):** One user "Hosts" the chat, and the other "Joins."
* **Simple GUI:** A clean and straightforward chat interface built with Java Swing.
* **Real-time Messaging:** Send and receive messages instantly.
* **1-to-1 Connection:** Designed for a direct connection between two peers.
* **Cross-Platform:** Runs on any operating system with the Java Runtime Environment (JRE).

## 📋 Requirements

* **Java Development Kit (JDK)** (version 8 or higher) to compile the code.
* **Java Runtime Environment (JRE)** to run the application.

## ⚡ How to Use

To use this chat application, you need two people (or two terminal windows on the same computer) to act as the two peers.

### 1. Compile the Code

First, compile the Java file:

javac P2PChatGUI.java

### 2. Run the Application

You will need to run the compiled code in two separate terminal windows.

java P2PChatGUI

### 3. Establish a Connection

#### Peer 1 (Host):

1. Run the command `java P2PChatGUI`.
2. In the first dialog box, select **"Host a Chat"**.
3. Enter a port number to listen on (e.g., `5000`) and click **OK**.
4. The application will now wait for the other peer to connect. It will display the host's IP address and the port it is listening on.

#### Peer 2 (Join):

1. Run the command `java P2PChatGUI` in a separate terminal.
2. In the first dialog box, select **"Join a Chat"**.
3. Enter the **Host IP** address (this is displayed in Peer 1's window). If you are testing on the same computer, you can use `localhost`.
4. Enter the **Port** that Peer 1 chose (e.g., `5000`).
5. Click **OK**.

### 4. Start Chatting!

As soon as Peer 2 clicks OK, the connection will be established. The input fields in both windows will become active, and you can start sending and receiving messages.

## 🛠️ How It Works (Technical Overview)

This application combines both server and client logic in a single class.

* **Host (Server Role):** The "Host" peer creates a `ServerSocket` on a specified port. It then calls `serverSocket.accept()`, which blocks and waits until a "Join" peer (the client) connects to it.
* **Join (Client Role):** The "Join" peer creates a `Socket` and attempts to connect to the Host's IP address and port.
* **Connection:** Once the connection is established, both peers have a `Socket` object. The application is symmetrical from this point on.
* **Multithreading:**
  * The **main GUI thread** (Event Dispatch Thread) handles user input (typing and clicking "Send").
  * A **background thread** is started on both peers. This thread continuously runs a loop, blocking on `reader.readLine()`, to listen for incoming messages. When a message is received, it uses `SwingUtilities.invokeLater()` to safely update the GUI's message area.
* **Streams:** `PrintWriter` is used to send (write) messages to the socket, and `BufferedReader` is used to receive (read) messages from the socket.
