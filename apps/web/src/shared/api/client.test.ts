import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";

import { ApiError, apiRequest, clearCsrfToken } from "./client";

const CSRF = { headerName: "X-XSRF-TOKEN", token: "masked-1" };

let csrfCalls = 0;
let tokenSeq = 0;
const csrfHandler = http.get("/api/auth/csrf-token", () => {
  csrfCalls += 1;
  tokenSeq += 1;
  return HttpResponse.json({ status: "SUCCESS", data: { ...CSRF, token: `masked-${tokenSeq}` } });
});

beforeEach(() => {
  clearCsrfToken();
  csrfCalls = 0;
  tokenSeq = 0;
});

async function failureOf(promise: Promise<unknown>) {
  try {
    await promise;
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError);
    return (error as ApiError).failure;
  }
  throw new Error("expected rejection");
}

describe("apiRequest", () => {
  it("GET은 CSRF 헤더 없이 요청하고 data 반환", async () => {
    let header: string | null = "unset";
    server.use(
      csrfHandler,
      http.get("/api/x", ({ request }) => {
        header = request.headers.get(CSRF.headerName);
        return HttpResponse.json({ status: "SUCCESS", data: { a: 1 } });
      }),
    );

    await expect(apiRequest("/api/x")).resolves.toEqual({ a: 1 });
    expect(header).toBeNull();
    expect(csrfCalls).toBe(0);
  });

  it("첫 변경 요청 전에 토큰을 1회 받고 두 번째는 재사용", async () => {
    const seen: (string | null)[] = [];
    server.use(
      csrfHandler,
      http.post("/api/x", ({ request }) => {
        seen.push(request.headers.get(CSRF.headerName));
        return HttpResponse.json({ status: "SUCCESS", data: null });
      }),
    );

    await apiRequest("/api/x", { method: "POST", json: {} });
    await apiRequest("/api/x", { method: "POST", json: {} });

    expect(csrfCalls).toBe(1);
    expect(seen).toEqual(["masked-1", "masked-1"]);
  });

  it("동시에 들어온 첫 변경 요청은 토큰 취득을 공유", async () => {
    server.use(csrfHandler, http.post("/api/x", () => HttpResponse.json({ status: "SUCCESS" })));

    await Promise.all([
      apiRequest("/api/x", { method: "POST" }),
      apiRequest("/api/x", { method: "POST" }),
    ]);

    expect(csrfCalls).toBe(1);
  });

  it.each([401, 403])("%i 응답 후 다음 변경 요청은 토큰을 새로 받음", async (status) => {
    const seen: (string | null)[] = [];
    let first = true;
    server.use(
      csrfHandler,
      http.post("/api/x", ({ request }) => {
        seen.push(request.headers.get(CSRF.headerName));
        if (first) {
          first = false;
          return HttpResponse.json({ status: "FAILURE", data: { code: "FORBIDDEN", message: "m" } }, { status });
        }
        return HttpResponse.json({ status: "SUCCESS" });
      }),
    );

    await failureOf(apiRequest("/api/x", { method: "POST" }));
    await apiRequest("/api/x", { method: "POST" });

    expect(csrfCalls).toBe(2);
    expect(seen).toEqual(["masked-1", "masked-2"]);
  });

  it("clearCsrfToken 후 토큰을 새로 받음", async () => {
    server.use(csrfHandler, http.post("/api/x", () => HttpResponse.json({ status: "SUCCESS" })));

    await apiRequest("/api/x", { method: "POST" });
    clearCsrfToken();
    await apiRequest("/api/x", { method: "POST" });

    expect(csrfCalls).toBe(2);
  });

  it("204는 null 반환", async () => {
    server.use(csrfHandler, http.delete("/api/x", () => new HttpResponse(null, { status: 204 })));

    await expect(apiRequest("/api/x", { method: "DELETE" })).resolves.toBeNull();
  });

  it("FAILURE 본문은 code·message·fields를 담아 throw", async () => {
    server.use(
      csrfHandler,
      http.post("/api/x", () =>
        HttpResponse.json(
          {
            status: "FAILURE",
            data: { code: "INVALID_INPUT", message: "bad", fields: [{ field: "title", message: "필수" }] },
          },
          { status: 400 },
        ),
      ),
    );

    expect(await failureOf(apiRequest("/api/x", { method: "POST", json: {} }))).toEqual({
      kind: "http",
      status: 400,
      code: "INVALID_INPUT",
      message: "bad",
      fields: [{ field: "title", message: "필수" }],
    });
  });

  it("HTML 오류 본문은 code null", async () => {
    server.use(
      http.get("/api/x", () => new HttpResponse("<html>oops</html>", { status: 500, headers: { "Content-Type": "text/html" } })),
    );

    expect(await failureOf(apiRequest("/api/x"))).toEqual({
      kind: "http",
      status: 500,
      code: null,
      message: null,
      fields: [],
    });
  });

  it("fetch reject는 network 실패, 변경 요청을 다시 보내지 않음", async () => {
    let calls = 0;
    server.use(
      csrfHandler,
      http.post("/api/x", () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );

    expect(await failureOf(apiRequest("/api/x", { method: "POST" }))).toEqual({ kind: "network" });
    expect(calls).toBe(1);
  });

  it("FormData 본문이면 Content-Type을 직접 지정하지 않음", async () => {
    let contentType: string | null = null;
    server.use(
      csrfHandler,
      http.post("/api/x", ({ request }) => {
        contentType = request.headers.get("Content-Type");
        return HttpResponse.json({ status: "SUCCESS" });
      }),
    );
    const formData = new FormData();
    formData.append("file", new File(["a"], "a.png", { type: "image/png" }));

    await apiRequest("/api/x", { method: "POST", formData });

    expect(contentType).toMatch(/^multipart\/form-data; boundary=/);
  });
});
