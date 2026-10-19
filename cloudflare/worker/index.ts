interface Env {
  ORIGIN_BASE_URL: string;
}

const STATIC_PATH_PREFIXES = ["/css/", "/js/", "/fonts/", "/images/"];
const STATIC_CACHE_SECONDS = 60 * 60 * 24;

function isStaticAsset(pathname: string): boolean {
  return STATIC_PATH_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

function getOrigin(env: Env): URL {
  const origin = new URL(env.ORIGIN_BASE_URL);
  if (origin.protocol !== "https:") {
    throw new Error("ORIGIN_BASE_URL must use HTTPS");
  }
  return origin;
}

function rewriteOriginLocation(
  location: string,
  origin: URL,
  publicRequestUrl: URL,
): string {
  let redirectUrl: URL;
  try {
    redirectUrl = new URL(location, origin);
  } catch {
    return location;
  }

  if (redirectUrl.origin !== origin.origin) {
    return location;
  }

  redirectUrl.protocol = publicRequestUrl.protocol;
  redirectUrl.host = publicRequestUrl.host;
  return redirectUrl.toString();
}

async function proxyToOrigin(request: Request, env: Env): Promise<Response> {
  const publicUrl = new URL(request.url);
  const origin = getOrigin(env);

  if (publicUrl.origin === origin.origin) {
    return new Response("Cloudflare edge origin is misconfigured", { status: 500 });
  }

  const targetUrl = new URL(`${publicUrl.pathname}${publicUrl.search}`, origin);
  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.delete("x-forwarded-for");
  headers.set("x-forwarded-host", publicUrl.host);
  headers.set("x-forwarded-proto", publicUrl.protocol.slice(0, -1));
  const clientIp = request.headers.get("cf-connecting-ip");
  if (clientIp) {
    headers.set("x-forwarded-for", clientIp);
  }

  const cacheable = request.method === "GET" && isStaticAsset(publicUrl.pathname);
  const originRequest = new Request(targetUrl, {
    method: request.method,
    headers,
    body:
      request.method === "GET" || request.method === "HEAD"
        ? undefined
        : request.body,
    redirect: "manual",
  });

  const originResponse = await fetch(originRequest, {
    cf: cacheable
      ? {
          cacheEverything: true,
          cacheTtl: STATIC_CACHE_SECONDS,
          cacheTtlByStatus: {
            "200-299": STATIC_CACHE_SECONDS,
            "404": 60,
            "500-599": 0,
          },
        }
      : { cacheEverything: false },
  });

  const responseHeaders = new Headers(originResponse.headers);
  const location = responseHeaders.get("location");
  if (location) {
    responseHeaders.set(
      "location",
      rewriteOriginLocation(location, origin, publicUrl),
    );
  }

  responseHeaders.set("x-castfan-edge", "cloudflare");

  return new Response(originResponse.body, {
    status: originResponse.status,
    statusText: originResponse.statusText,
    headers: responseHeaders,
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await proxyToOrigin(request, env);
    } catch (error) {
      console.error("CastFan edge request failed", {
        method: request.method,
        path: new URL(request.url).pathname,
        error: error instanceof Error ? error.message : "Unknown error",
      });
      return new Response("CastFan is temporarily unavailable", {
        status: 502,
        headers: {
          "cache-control": "no-store",
          "content-type": "text/plain; charset=utf-8",
        },
      });
    }
  },
} satisfies ExportedHandler<Env>;
