# SakaTickets — Setup Guide for Team Members (Windows + VS Code)

This guide takes you from **nothing installed** to **SakaTickets running on your computer**.
Follow the steps in order. It takes about 30–45 minutes the first time (mostly waiting for downloads).

> **What you'll end up with:** your own copy of the code, your own PostgreSQL database
> on your own computer, and the app running from VS Code. Your database is separate from
> everyone else's — events you create stay on your computer.

---

## Contents

1. [What you need to install](#1-what-you-need-to-install)
2. [Install Git](#2-install-git)
3. [Install the Java Development Kit (JDK 25)](#3-install-the-java-development-kit-jdk-25)
4. [Install VS Code and the Java extensions](#4-install-vs-code-and-the-java-extensions)
5. [Install PostgreSQL](#5-install-postgresql)
6. [Create the SakaTickets database](#6-create-the-sakatickets-database)
7. [Download the project from GitHub](#7-download-the-project-from-github)
8. [Connect the project to your database (`db.properties`)](#8-connect-the-project-to-your-database-dbproperties)
9. [Run the app](#9-run-the-app)
10. [Getting your teammates' latest changes](#10-getting-your-teammates-latest-changes)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. What you need to install

| Tool | Why we need it | Version |
|---|---|---|
| **Git** | Downloads the project from GitHub and lets you share changes | Any recent version |
| **JDK** (Java Development Kit) | Compiles and runs Java code | **25** (the project is set to Java 25) |
| **VS Code** | The code editor | Any recent version |
| **Extension Pack for Java** (inside VS Code) | Lets VS Code understand, build and run Java projects | Latest |
| **PostgreSQL** | The database where events, tickets and accounts are saved | 16 or 17 |

You do **not** need to install Maven separately — the Java extension in VS Code handles it for you.
It also downloads the project's libraries (FlatLaf, JCalendar and the PostgreSQL driver) automatically.

---

## 2. Install Git

1. Go to **https://git-scm.com/download/win** and download the installer.
2. Run it. You can click **Next** on every screen — the defaults are fine.
3. Check it worked: open **Command Prompt** (press the Windows key, type `cmd`, press Enter) and type:
   ```
   git --version
   ```
   You should see something like `git version 2.47.0.windows.1`.

---

## 3. Install the Java Development Kit (JDK 25)

1. Go to **https://adoptium.net/temurin/releases/?version=25**
2. Choose **Operating System: Windows**, **Architecture: x64**, **Package Type: JDK**, and download the **.msi** installer.
3. Run it. On the screen with a list of features, make sure these are **turned on** (click each one and choose
   *"Will be installed on local hard drive"*):
   - **Set JAVA_HOME variable**
   - **Add to PATH**
4. Check it worked: open a **new** Command Prompt and type:
   ```
   java -version
   ```
   The first line should start with `openjdk version "25`.

> **Why version 25?** The project's `pom.xml` file says `<maven.compiler.release>25</maven.compiler.release>`.
> An older JDK (like 17 or 21) will refuse to build it with the error *"release version 25 not supported"*.

---

## 4. Install VS Code and the Java extensions

1. Download VS Code from **https://code.visualstudio.com/** and install it (defaults are fine).
2. Open VS Code. Click the **Extensions** icon on the left bar (four squares), or press `Ctrl+Shift+X`.
3. Search for **Extension Pack for Java** (by Microsoft) and click **Install**.
   This installs several extensions at once, including *Language Support for Java by Red Hat*,
   *Debugger for Java* and *Maven for Java*.
4. Restart VS Code.

---

## 5. Install PostgreSQL

1. Go to **https://www.postgresql.org/download/windows/** and click **Download the installer**
   (it takes you to the EDB website). Download the latest **Windows x86-64** version.
2. Run the installer and click **Next** through it, paying attention to these screens:
   - **Select Components:** leave everything ticked (*PostgreSQL Server*, *pgAdmin 4*, *Command Line Tools*).
     *Stack Builder* is not needed — you can untick it.
   - **Password:** type a password for the main database user (called `postgres`).
     **Write this password down — you'll need it in step 6.**
   - **Port:** leave it as **5432**.
   - **Locale:** leave the default.
3. Finish the installation. PostgreSQL now runs in the background every time Windows starts.

---

## 6. Create the SakaTickets database

We'll create a database called `sakatickets` and a login called `saka` for the app to use.

1. Press the Windows key, type **SQL Shell (psql)** and open it. (It was installed with PostgreSQL.)
2. It asks five questions. Press **Enter** four times to accept the defaults:
   ```
   Server [localhost]:            ← press Enter
   Database [postgres]:           ← press Enter
   Port [5432]:                   ← press Enter
   Username [postgres]:           ← press Enter
   Password for user postgres:    ← type the password from step 5, then Enter
   ```
   (The password won't show while you type — that's normal.)
3. You'll see a prompt like `postgres=#`. Type these two lines, pressing Enter after each one.
   **Choose your own password** in place of `my-saka-password` and remember it:
   ```sql
   CREATE ROLE saka WITH LOGIN PASSWORD 'my-saka-password';
   CREATE DATABASE sakatickets OWNER saka;
   ```
   After each line you should see `CREATE ROLE` and then `CREATE DATABASE`.
4. Type `\q` and press Enter to close SQL Shell.

That's all the database setup. You don't need to create any tables — **the app creates its own tables
and fills in sample events and sales the first time it runs.**

---

## 7. Download the project from GitHub

> **Access:** the repository is **https://github.com/Kipruto-LM/sakaTickets-java-project**.
> If it's private, ask the team lead to add you as a collaborator first, and accept the email invitation.

1. Open VS Code. Press `Ctrl+Shift+P`, type **Git: Clone** and press Enter.
2. Paste this address and press Enter:
   ```
   https://github.com/Kipruto-LM/sakaTickets-java-project.git
   ```
3. Choose a folder to save it in (for example `Documents`). If GitHub asks you to sign in, sign in with your
   GitHub account in the browser window that opens.
4. When VS Code asks **"Would you like to open the cloned repository?"**, click **Open**.
5. **Switch to the right branch.** The finished project is on the branch **`main2`**.
   Click the branch name in the **bottom-left corner** of VS Code (it probably says `main`) and pick
   **`origin/main2`** from the list. The bottom-left should now say `main2`.

   *(Or, in the VS Code terminal — `` Ctrl+` `` — type `git checkout main2`.)*
6. Wait for Java to load the project. In the bottom bar you'll see **Java: Loading…** and then a
   **thumbs-up 👍** or **Java: Ready**. The first time can take a few minutes while it downloads libraries.
   If VS Code asks *"Do you trust the authors of the files in this folder?"*, click **Yes, I trust the authors**.

---

## 8. Connect the project to your database (`db.properties`)

The app reads your database login from a small file called **`db.properties`**. This file is **not** on
GitHub (it holds your password, so each person makes their own), but a template is.

1. In the VS Code file list (left side), find **`db.properties.example`** in the main project folder.
2. Right-click it → **Copy**, then right-click the main project folder → **Paste**.
   Rename the copy to exactly **`db.properties`** (right-click → **Rename**).
3. Open `db.properties` and change the password line to the password you chose for `saka` in step 6:
   ```properties
   url=jdbc:postgresql://localhost:5432/sakatickets
   user=saka
   password=my-saka-password
   ```
4. Save the file (`Ctrl+S`).

> ⚠️ Never commit `db.properties`. It's listed in `.gitignore`, so Git ignores it automatically —
> just don't force-add it.

---

## 9. Run the app

1. In the file list, open **`src/main/java/Main.java`**.
2. Click **Run** (the small link just above the line `public static void main`), or press **`Ctrl+F5`**.
3. The SakaTickets window opens. 🎉

**The first time** it runs, the app creates the database tables and adds 7 sample events with sample
ticket sales, so the dashboard has something to show.

**To try the host side:** click **Start hosting** on the welcome page and log in with:

| Username | Password |
|---|---|
| `Talel` | `admin123` |

Everything you do — creating events, buying tickets, registering hosts — is saved in your database,
and is still there next time you open the app.

---

## 10. Getting your teammates' latest changes

When someone pushes new code to GitHub:

1. Make sure you're on the right branch (bottom-left of VS Code says `main2`).
2. Click the **Source Control** icon on the left bar (or press `Ctrl+Shift+G`), click the **`···`** menu → **Pull**.
   *(Or in the terminal: `git pull`.)*
3. Run the app again.

**To share your own changes:** in Source Control, type a short message describing what you changed,
click **Commit**, then click **Sync Changes** (or `···` → **Push**).

---

## 11. Troubleshooting

| What you see | What it means | How to fix it |
|---|---|---|
| A pop-up: **"SakaTickets - database problem … Could not connect"** | The app can't reach PostgreSQL | • Check `db.properties` exists (not `db.properties.example`) and the password matches step 6.<br>• Check PostgreSQL is running: Windows key → **Services** → find **postgresql-x64-17** (or 16) → it should say *Running*. If not, right-click → **Start**. |
| **"password authentication failed for user saka"** | Wrong password in `db.properties` | Fix the password line, or reset it in SQL Shell: `ALTER ROLE saka WITH PASSWORD 'new-password';` |
| **"database sakatickets does not exist"** | Step 6 wasn't finished | Repeat step 6. |
| **"release version 25 not supported"** | VS Code is using an older JDK | Press `Ctrl+Shift+P` → **Java: Configure Java Runtime** → set the project to JDK 25. If JDK 25 isn't listed, repeat step 3, then restart VS Code. |
| **"No suitable driver found for jdbc:postgresql"** or red errors on every import | VS Code hasn't finished loading the project's libraries | Wait for **Java: Ready**. If it persists: `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → **Restart and delete**. |
| The **Run** link doesn't appear above `main` | Java extension not loaded yet | Wait for **Java: Ready** in the bottom bar, or reinstall the *Extension Pack for Java* (step 4). |
| The app looks right but **fonts look slightly different** from screenshots | The app prefers the Ubuntu fonts, which Windows doesn't have | Nothing to fix — Java uses a similar font instead. (Optional: install the free *Ubuntu* fonts from Google Fonts.) |
| You want to **start again with fresh sample data** | — | Open SQL Shell, log in as in step 6, then type `\c sakatickets` and run: `DROP TABLE bookings, event_details, ticket_tiers, events, hosts;` The next run recreates everything. |

Still stuck? Copy the exact error message and send it to the team group chat.
