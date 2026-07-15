import { useState } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMember } from "../api/normalize.js";

export default function Auth() {
  const { api, member, setMember, run, loading } = useSession();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ loginId: "asd", name: "", password: "1234", nickname: "" });

  if (member) {
    return <Navigate to={searchParams.get("next") || "/"} replace />;
  }

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    await run(async () => {
      if (!form.loginId.trim() || !form.password.trim()) throw new Error("아이디와 비밀번호를 입력해주세요.");
      if (mode === "signup") {
        if (!form.name.trim() || !form.nickname.trim()) throw new Error("이름과 닉네임을 입력해주세요.");
        await api.signup(form);
        setMode("login");
        return;
      }
      const loginMember = await api.login({ loginId: form.loginId, password: form.password });
      setMember(normalizeMember(loginMember));
      navigate(searchParams.get("next") || "/");
    }, mode === "signup" ? "회원가입이 완료되었습니다." : "로그인되었습니다.");
  }

  return (
    <main className="form-page">
      <section className="form-panel auth-card">
        <div className="auth-toggle">
          <button type="button" className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>로그인</button>
          <button type="button" className={mode === "signup" ? "active" : ""} onClick={() => setMode("signup")}>회원가입</button>
        </div>
        <p>{mode === "login" ? "Welcome back" : "Create account"}</p>
        <h1>{mode === "login" ? "로그인" : "회원가입"}</h1>
        <form onSubmit={submit}>
          <input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" />
          <input name="password" type="password" value={form.password} onChange={change} placeholder="비밀번호" />
          {mode === "signup" && (
            <>
              <input name="name" value={form.name} onChange={change} placeholder="이름" />
              <input name="nickname" value={form.nickname} onChange={change} placeholder="닉네임" />
            </>
          )}
          <button disabled={loading}>{mode === "login" ? "로그인" : "가입하기"}</button>
        </form>
      </section>
    </main>
  );
}
