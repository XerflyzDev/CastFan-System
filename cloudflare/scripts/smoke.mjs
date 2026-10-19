const edgeUrlValue = process.env.CASTFAN_EDGE_URL;

if (!edgeUrlValue) {
  console.error("Set CASTFAN_EDGE_URL to the deployed or local Worker URL.");
  process.exit(1);
}

const edgeUrl = new URL(edgeUrlValue);

async function requireSuccessfulResponse(pathname) {
  const response = await fetch(new URL(pathname, edgeUrl), {
    redirect: "manual",
  });
  if (!response.ok) {
    throw new Error(`${pathname} returned HTTP ${response.status}`);
  }
  if (response.headers.get("x-castfan-edge") !== "cloudflare") {
    throw new Error(`${pathname} did not pass through the CastFan edge Worker`);
  }
  return response;
}

const healthResponse = await requireSuccessfulResponse("/actuator/health");
const health = await healthResponse.json();
if (health.status !== "UP") {
  throw new Error(`Health status was ${String(health.status)}`);
}

const loginResponse = await requireSuccessfulResponse("/login");
const loginHtml = await loginResponse.text();
if (!loginHtml.includes('name="_csrf"')) {
  throw new Error("Login page did not contain a CSRF token");
}
if (loginHtml.includes("asia-southeast1.run.app")) {
  throw new Error("Login page exposed the Cloud Run hostname");
}

console.log(`CastFan edge smoke test passed for ${edgeUrl.origin}`);
