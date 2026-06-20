# Used Market Frontend

This is the React frontend for the Spring REST API backend.

## Run in IntelliJ

Install Node.js first if `node -v` does not work. Node.js 18 LTS or newer is recommended.

Open two terminals in IntelliJ.

Terminal 1: run Spring Boot from the project root.

```powershell
cd C:\JavaStudy\springStudy\project
.\gradlew.bat bootRun
```

Terminal 2: run React from the frontend folder.

```powershell
cd C:\JavaStudy\springStudy\project\frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:5173
```

## How API calls work

React calls `/api/items`.

During development, Vite proxies the request:

```text
React http://localhost:5173/api/items
-> Vite proxy
-> Spring http://localhost:8080/api/items
```

Because of this proxy, you usually do not need CORS settings while developing locally.

When `Mock API` is checked, the frontend uses fake data and does not call Spring.
When `Mock API` is unchecked, the frontend calls the Spring REST API.
