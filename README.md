# LetsShare - File Sharing Application

**LetsShare** is a Java-based desktop application designed for securely transferring files between computers on the same local network using TCP sockets. With a simple GUI built with Swing, it allows users to send and receive files effortlessly while logging transfer history.

---

## Features
- **Send Files:** Connect to a receiver by IP address and transfer files securely.
- **Receive Files:** Accept incoming file transfers and save them to your Downloads folder.
- **Transfer History:** View a detailed log of all file transfers, including timestamps, operations, and file details.
- **Progress Tracking:** Real-time progress bars and speed indicators during transfers.
- **Local Network Only:** Ensures data stays within your network for security.

---

## System Requirements
- **Operating System:** Windows
- **Java:** No manual Java installation required (JRE bundled with the app) or JDK 21 for source code execution
- **Disk Space:** Minimum 100 MB free
- **Network:** Both sender and receiver must be on the same local network

---

## Installation

### Precompiled Setup
1. Download the setup file from the [Releases](https://github.com/m-arjaz/letsshare/releases) section.
2. Run the installer and follow the on-screen instructions.
3. Allow the application through your firewall when prompted.

### From Source
1. Clone this repository:
   ```bash
   git clone https://github.com/m-arjaz/letsshare.git
   cd letsshare
   ```
2. Ensure you have JDK 21 installed ([download here](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)).
3. Compile and run the application:
   ```bash
   javac -d bin src/tester/*.java
   java -cp bin tester.ApplicationGUI
   ```

---

## Quick Start
1. Launch the application.
2. **First-time setup:** Allow firewall access if prompted.
3. Choose your role:
   - **Sender:** Click "Send" and follow the sender instructions.
   - **Receiver:** Click "Receive" and follow the receiver instructions.

---

## Usage Instructions

### As a Sender
1. Open the app and click **Send**.
2. Enter the receiver’s local IP address (e.g., `192.168.1.100`).
3. Click **Connect** (Note: The receiver must click "Connect" first).
4. Once connected, click **Browse** to select a file.
5. Click **Send** to start the transfer.
6. The socket closes automatically after each operation—reconnect for additional transfers.

### As a Receiver
1. Open the app and click **Receive**.
2. Click **Connect** to start listening (this must be done before the sender connects).
3. Wait for the sender to connect and send the file.
4. Received files are saved automatically to your `Downloads` folder.

### Viewing History
1. Click **History** from the main menu.
2. View a table of past transfers, including time, operation, source/destination, file name, and size.
3. Click **Refresh** to update the list or **Exit** to return to the main menu.

---

## Network Configuration
- **Default Port:** 5000 (TCP)
- **Protocol:** TCP/IP
- **Firewall:** Ensure port 5000 is open on both devices. The app attempts to add/remove rules automatically, but manual configuration may be needed:
  ```cmd
  netsh advfirewall firewall add rule name="LetsShare" dir=in action=allow protocol=TCP localport=5000
  netsh advfirewall firewall add rule name="LetsShare" dir=out action=allow protocol=TCP localport=5000
  ```

---

## Common Issues and Solutions

### Connection Failed
- **Check:** Verify the IP address and ensure port 5000 is open.
- **Fix:** Confirm both devices are on the same network and firewall rules are set.

### Application Won’t Start
- **Check:** Ensure antivirus isn’t blocking it and port 5000 isn’t in use.
- **Fix:** Run as administrator or use JDK 21 to execute the source code.

### "Port Already in Use" Error
- **Fix:** Wait a moment and retry, or change the port in `AppConstants.java` (update both sender and receiver).

---

## Security Notes
- Transfers are limited to the local network—no internet exposure.
- No encryption is implemented; use on trusted networks only.

---

## Technical Support
- **Logs:** Check `C:\Users\m-arjaz\OneDrive\Documents\LetsShare\logs.txt` for details.
- **Contact:** Reach out to Assladday Oubaida (0612777397) or Arjaz Mohamed (0771658644).

---

## Contributing
1. Fork this repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes (`git commit -m "Add feature"`).
4. Push to your fork (`git push origin feature-name`).
5. Open a Pull Request.

---

## Contributors
- **Arjaz Mohamed**
- **Assladday Oubaida**


---

## Notes
- If the precompiled app fails, run the source code with JDK 21 using `ApplicationGUI` as the main class.
- Feedback and bug reports are welcome!

---

This README provides a professional yet approachable guide for users and developers. Replace `m-arjaz` with your actual GitHub username when pushing to GitHub. Let me know if you’d like further tweaks!
