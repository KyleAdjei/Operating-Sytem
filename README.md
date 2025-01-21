# Operating System Simulation Project

## Description
This project simulates an operating system (OS) with key components such as process management, virtual memory, a virtual file system (VFS), and device interaction. It provides a framework for understanding how an OS handles processes, memory allocation, and communication between userland processes. The simulation also includes custom devices like a random number generator and a fake file system.

The project is designed to run userland processes (like `Ping` and `Pong`) and simulate their interactions using kernel-level message passing, scheduling, and cooperative multitasking.

---

## Features
- **Kernel**: Manages system resources, process switching, and inter-process communication.
- **Scheduler**: Implements priority-based process scheduling (real-time, interactive, and background).
- **Virtual Memory Management**: Handles memory allocation, page table management, and TLB (Translation Lookaside Buffer).
- **Virtual File System (VFS)**: Provides device abstraction and manages custom devices.
- **Devices**:
  - **RandomDevice**: Simulates a random number generator.
  - **FakeFileSystem**: Simulates basic file system operations like opening, reading, writing, and seeking files.
- **Userland Processes**:
  - `Ping`: Sends messages to the `Pong` process and waits for responses.
  - `Pong`: Receives messages from the `Ping` process and replies back.

---

## Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: Version 8 or higher.
- **IntelliJ IDEA** (recommended) or another Java IDE.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/operating-system-simulation.git
   cd operating-system-simulation
