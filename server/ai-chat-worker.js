/**
 * Trâu Bi Non - secure AI chat proxy (Cloudflare Worker)
 *
 * Configure OPENAI_API_KEY as a Worker secret. Never put it in the APK.
 * Optional vars: OPENAI_MODEL (defaults to gpt-6-luna), ALLOWED_ORIGIN.
 */
export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";
    const allowedOrigin = env.ALLOWED_ORIGIN || "*";
    const corsOrigin = allowedOrigin === "*" || origin === allowedOrigin ? (allowedOrigin === "*" ? "*" : origin) : "null";
    const headers = {
      "Access-Control-Allow-Origin": corsOrigin,
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Content-Type": "application/json; charset=utf-8",
      "Vary": "Origin"
    };
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers });
    if (request.method !== "POST" || new URL(request.url).pathname !== "/chat") {
      return new Response(JSON.stringify({ error: "Not found" }), { status: 404, headers });
    }
    if (corsOrigin === "null") return new Response(JSON.stringify({ error: "Origin not allowed" }), { status: 403, headers });
    if (!env.OPENAI_API_KEY) return new Response(JSON.stringify({ error: "Máy chủ chưa được cấu hình khóa AI." }), { status: 503, headers });
    try {
      const body = await request.json();
      const message = typeof body.message === "string" ? body.message.trim() : "";
      if (!message || message.length > 4000) {
        return new Response(JSON.stringify({ error: "Tin nhắn trống hoặc dài quá 4.000 ký tự." }), { status: 400, headers });
      }
      const history = Array.isArray(body.history) ? body.history.slice(-12) : [];
      const input = [
        { role: "system", content: "Bạn là trợ lý thân thiện tích hợp trong ứng dụng Trâu Bi Non. Trả lời bằng tiếng Việt, rõ ràng, phù hợp lứa tuổi học sinh. Hỗ trợ học tập, kiến thức và hướng dẫn trò chơi an toàn. Không hướng dẫn nội dung nguy hiểm, không yêu cầu thông tin cá nhân hoặc bí mật. Trả lời súc tích." },
        ...history.filter(item => item && ["user", "assistant"].includes(item.role) && typeof item.content === "string")
          .map(item => ({ role: item.role, content: item.content.slice(0, 4000) })),
        { role: "user", content: "Game đang chọn: " + String(body.game || "không rõ").slice(0, 100) + ". Câu hỏi: " + message }
      ];
      const upstream = await fetch("https://api.openai.com/v1/responses", {
        method: "POST",
        headers: { "Authorization": "Bearer " + env.OPENAI_API_KEY, "Content-Type": "application/json" },
        body: JSON.stringify({ model: env.OPENAI_MODEL || "gpt-6-luna", input, max_output_tokens: 700 })
      });
      const data = await upstream.json();
      if (!upstream.ok) {
        return new Response(JSON.stringify({ error: "Dịch vụ AI đang bận hoặc cấu hình chưa đúng." }), { status: 502, headers });
      }
      const reply = typeof data.output_text === "string" ? data.output_text.trim() :
        (data.output || []).flatMap(item => item.content || []).filter(part => part.type === "output_text").map(part => part.text).join("\n").trim();
      if (!reply) return new Response(JSON.stringify({ error: "AI chưa tạo được câu trả lời." }), { status: 502, headers });
      return new Response(JSON.stringify({ reply }), { status: 200, headers });
    } catch (error) {
      return new Response(JSON.stringify({ error: "Yêu cầu không hợp lệ hoặc kết nối bị lỗi." }), { status: 400, headers });
    }
  }
};
