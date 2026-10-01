# Mateo Airways Reservation System

Console-based airline reservation system in Java (OOP, role-based access, file persistence).

## Requirements
- JDK 11 or newer
- Visual Studio Code with the "Extension Pack for Java"

## Run (VS Code only)
1. Open this folder (`MateoAirways`) in VS Code.
2. Open `src/airline/Main.java` and press **F5**, or click the Code Runner play button (both are configured in `.vscode/`).
3. Always open the **MateoAirways** folder itself (not `src` or `airline`), so the paths resolve.

The program checks that it is running inside the VS Code terminal and exits otherwise.

## Demo logins
| Role      | Username | Password |
|-----------|----------|----------|
| Admin     | admin    | admin123 |
| Agent     | agent    | agent123 |
| Passenger | juan     | juan123  |

Data is saved to `airline-data.ser`. Delete that file to reset to the seed data.
