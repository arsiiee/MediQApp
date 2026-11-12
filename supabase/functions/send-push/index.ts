import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Full Firebase service account JSON from env var FCM_SERVICE_ACCOUNT
let serviceAccount: any = null;

function base64urlEncode(data: Uint8Array): string {
  let binary = "";
  for (let i = 0; i < data.length; i++) {
    binary += String.fromCharCode(data[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function getServiceAccount() {
  if (serviceAccount) return serviceAccount;
  const raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!raw) throw new Error("FCM_SERVICE_ACCOUNT env var not set");
  serviceAccount = JSON.parse(raw);
  return serviceAccount;
}

async function getAccessToken(): Promise<string> {
  const now = Date.now();
  if (getAccessToken.token && now < getAccessToken.expiry - 60000) {
    return getAccessToken.token;
  }

  const sa = await getServiceAccount();
  const headerObj = { alg: "RS256", typ: "JWT" };
  const header64 = base64urlEncode(new TextEncoder().encode(JSON.stringify(headerObj)));

  const nowSec = Math.floor(now / 1000);
  const payloadObj = {
    iss: sa.client_email,
    sub: sa.client_email,
    aud: "https://oauth2.googleapis.com/token",
    iat: nowSec,
    exp: nowSec + 3600,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
  };
  const payload64 = base64urlEncode(new TextEncoder().encode(JSON.stringify(payloadObj)));

  const signingInput = `${header64}.${payload64}`;

  const keyData = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----\n/, "")
    .replace(/\n-----END PRIVATE KEY-----\n/, "");
  const keyBytes = Uint8Array.from(atob(keyData), (c) => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signingInput)
  );
  const sig64 = base64urlEncode(new Uint8Array(signature as ArrayBuffer));

  const jwt = `${signingInput}.${sig64}`;

  const resp = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  const data = await resp.json();
  getAccessToken.token = data.access_token;
  getAccessToken.expiry = now + (data.expires_in * 1000);
  return getAccessToken.token;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { fcm_token, title, body, click_action, appointment_id } = await req.json();

    if (!fcm_token) {
      return new Response(JSON.stringify({ error: "Missing fcm_token" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const token = await getAccessToken();

    const message = {
      message: {
        token: fcm_token,
        notification: {
          title: title || "MediQ",
          body: body || "You have a new notification",
        },
        data: {
          click_action: click_action || "",
          appointment_id: appointment_id || "",
        },
        android: {
          priority: "high",
          notification: {
            channel_id: "mediq_notifications",
            icon: "ic_launcher",
            sound: "default",
          },
        },
      },
    };

    const resp = await fetch(
      `https://fcm.googleapis.com/v1/projects/${(await getServiceAccount()).project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(message),
      }
    );

    const result = await resp.json();

    if (!resp.ok) {
      return new Response(JSON.stringify({ error: result }), {
        status: resp.status,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ success: true, messageId: result.name }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
