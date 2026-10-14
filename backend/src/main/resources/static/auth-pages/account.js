"use strict";

const main = document.querySelector("main");
const form = document.querySelector("form");
const result = document.querySelector("#result");
const button = form.querySelector("button");
const token = new URLSearchParams(location.hash.slice(1)).get("token")
    || new URLSearchParams(location.search).get("token");
history.replaceState(null, "", location.pathname);

if (!token) {
    result.textContent = "인증 정보가 없습니다. 앱에서 새 링크를 요청해 주세요.";
    button.disabled = true;
}

form.addEventListener("submit", async event => {
    event.preventDefault();
    const payload = { token };
    const resetting = main.dataset.action === "reset";
    if (resetting) {
        const password = document.querySelector("#password").value;
        if (password !== document.querySelector("#confirm").value) {
            result.textContent = "비밀번호가 일치하지 않습니다.";
            return;
        }
        if (password.length < 8 || new TextEncoder().encode(password).length > 72) {
            result.textContent = "비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력해 주세요.";
            return;
        }
        payload.newPassword = password;
    }
    button.disabled = true;
    result.textContent = "처리하고 있습니다.";
    try {
        const response = await fetch(resetting ? "/api/v1/auth/password-resets/confirm" : "/api/v1/auth/email-verifications/confirm", {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload), credentials: "omit", cache: "no-store"
        });
        if (!response.ok) {
            const body = await response.json();
            result.textContent = body.error?.message || "처리하지 못했습니다. 앱에서 새 링크를 요청해 주세요.";
            button.disabled = false;
            return;
        }
        // This page does not persist login tokens. The user logs in again in the app.
        form.reset();
        form.hidden = true;
        result.textContent = resetting ? "비밀번호가 변경됐습니다. 앱에서 새 비밀번호로 로그인하세요."
            : "이메일 인증이 완료됐습니다. 앱으로 돌아가 로그인하세요.";
    } catch {
        result.textContent = "서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        button.disabled = false;
    }
});
