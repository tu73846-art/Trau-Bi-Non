# Trâu Bi Non AI Chat Worker

This folder contains a server-side proxy for the app's optional online AI chat.

## Important security notes
- Never put an API key in the Android app, HTML, repository, or a public message.
- An adult project maintainer should configure the provider account, review its current age, billing, and usage terms, and set usage limits before enabling the service.
- Requests sent through this proxy may be processed by the AI provider. Do not send passwords, addresses, school details, or other private information.

## Deploy
1. Create a Cloudflare Worker project using `ai-chat-worker.js`.
2. Add the secret `OPENAI_API_KEY` in the Worker settings (Secrets). Do not commit it.
3. Optionally set `OPENAI_MODEL` (default: `gpt-6-luna`) and `ALLOWED_ORIGIN`.
4. Deploy the Worker. Its endpoint must be HTTPS and accept `POST /chat`.
5. In Trâu Bi Non, open AI → AI CHAT ONLINE → CÀI ĐẶT AI and enter the Worker base URL (without `/chat`).

The app stores only the server URL on the device. The Worker forwards requests to the OpenAI Responses API and returns `{ "reply": "..." }`.
