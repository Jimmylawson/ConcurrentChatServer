# 🧵 Java Non-Blocking Chat Server (Telnet Compatible)

This project is a **Java-based multi-client chat server** built using **non-blocking I/O (NIO)**. It supports **Telnet connections**, **private messaging**, **ANSI color-coded outputs**, **user commands**, and **server-side logging with timestamps**.

---

## 🔄 Evolution of the Project

### 🔹 Phase 1: Blocking I/O with `ServerSocket` and Threads
- Used `ServerSocket` and `Socket` to accept clients.
- Each client connection ran on a separate `Thread`.
- Suffered from scalability and thread management issues.

### 🔹 Phase 2: Non-Blocking I/O with `ServerSocketChannel` and `Selector`
- Switched to Java NIO:
    - `ServerSocketChannel` for non-blocking server setup
    - `Selector` to monitor channels for I/O readiness
- One-threaded event-driven server using `SelectionKey`s:
    - `key.isAcceptable()` to accept new clients
    - `key.isReadable()` to read messages
- Much better scalability and performance

---

## 💬 Features

### ✅ Core Chat Features
- Multi-client support over **Telnet**
- Broadcast messages to all users
- Private messaging via `/msg <user> <message>`
- View online users with `/who`
- Graceful disconnection with `/quit`

### 🖍️ Terminal Color Output
Uses **ANSI escape codes** to colorize messages:
- `🔒 Private messages`: Green
- `👋 Join/leave messages`: Yellow
- `❌ Errors`: Red
- `🆘 Help`: Cyan

### 📜 Logging
All activities are logged using `java.util.logging`:
- Log file: `chat-server.log`
- Includes timestamps and method context
- Example:
- Jun 27, 2025 8:13:00 PM ChatServer handleMessage
  INFO: Private message from User1 to User3: yes i am
- ---

## 🛠️ Commands

| Command                         | Description                                      |
|----------------------------------|--------------------------------------------------|
| `/msg <user> <message>`         | Send private message to a user                   |
| `/who`                          | List all currently online users                 |
| `/quit`                         | Leave the chat                                   |
| `/help`                         | Show all available commands                      |

---

## 🧪 How to Run

```bash
javac ChatServer.java
java ChatServer
Then, from multiple terminals:
telnet localhost 5050
ChatServer.java         // Main server logic with Selector + SocketChannel
Colors.java             // ANSI color enum helper
chat-server.log         // Generated log file
chat-server.log.lck     // Logger lock file (auto-created)

## 🧱 Tech Stack

- **Java NIO:** `ServerSocketChannel`, `SocketChannel`, `Selector`
- **Terminal UI:** ANSI Escape Codes
- **Logging:** `java.util.logging`
- **Testing:** Telnet CLI

## 📌 Next Steps (Planned Features)

- `/nick` command to change usernames
- File sharing (Base64 encoded)
- GUI Client using JavaFX
- Netty-based refactor for production-grade I/O