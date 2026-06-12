# Eably — Frontend

React single-page app for the Eably platform: clients browse student/provider services, book availability slots, pay via Stripe, chat in real time and leave reviews. Includes student, client and admin areas plus public landing pages.

## Tech stack

| Area          | Technology                          |
|---------------|-------------------------------------|
| Framework     | React 18 + Vite 7                   |
| Routing       | React Router 7                      |
| Data fetching | TanStack React Query                |
| State         | Zustand (`authStore`)               |
| HTTP          | Axios (`src/lib/api.js`)            |
| Styling       | Tailwind CSS 4                      |
| Real-time     | STOMP over SockJS (`@stomp/stompjs`)|
| Payments      | Stripe (`@stripe/react-stripe-js`)  |
| Icons         | lucide-react                        |
| Notifications | react-hot-toast                     |

## Project layout

```
src
├── assets
├── components   # calendar, chat, common, layout, skeleton, stripe
├── hooks
├── lib          # api.js (axios), getErrorMessage.js, weekCalendar.js
├── pages        # admin, auth, client, common, landing, student, stripe
├── services     # one module per backend domain (auth, booking, payment, ...)
└── store        # authStore (Zustand)
```

## API connection

The axios client uses base URL `/api/v1`. In development, Vite proxies `/api` and the WebSocket path `/ws-eably` to the backend at `http://localhost:8080` (see [vite.config.js](vite.config.js)). In production the app is served by nginx (see [nginx.conf](nginx.conf)).

## Environment variables

Create a `.env` file (Vite exposes only `VITE_`-prefixed vars to the client):

| Variable         | Description                  |
|------------------|------------------------------|
| `VITE_STRIPE_PK` | Stripe publishable key       |

## Scripts

```bash
npm install
npm run dev       # dev server with HMR + backend proxy
npm run build     # production build to dist/
npm run preview   # preview the production build
npm run lint      # ESLint
```

## Running with Docker

The repository-root `docker-compose.yml` builds and serves the frontend (nginx) alongside the backend and database:

```bash
docker compose up --build
```

The frontend is then served on `http://localhost:8081`.

## Requirements

- Node.js 18+ (Vite 7 / React 18)
- A running backend on `http://localhost:8080` for local development
