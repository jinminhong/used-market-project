import { useState } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMember } from "../api/normalize.js";

export default function Auth() {
  const { api, member, setMember, run, loading } = useSession();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ loginId: "asd", name: "", password: "1234", passwordConfirm: "", nickname: "" });
  const [idCheck, setIdCheck] = useState(null);
  const [nicknameCheck, setNicknameCheck] = useState(null);

  if (member) {
    return <Navigate to={searchParams.get("next") || "/"} replace />;
  }

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    if (name === "loginId") setIdCheck(null);
    if (name === "nickname") setNicknameCheck(null);
  }

  async function checkId() {
    if (!form.loginId.trim()) return setNotice("아이디를 입력해주세요.");
    await run(async () => {
      const { duplicate } = await api.checkLoginId(form.loginId.trim());
      setIdCheck({ value: form.loginId.trim(), available: !duplicate });
    }, "");
  }

  async function checkNickname() {
    if (!form.nickname.trim()) return setNotice("닉네임을 입력해주세요.");
    await run(async () => {
      const { duplicate } = await api.checkNickname(form.nickname.trim());
      setNicknameCheck({ value: form.nickname.trim(), available: !duplicate });
    }, "");
  }

  async function submit(event) {
    event.preventDefault();
    await run(async () => {
      if (!form.loginId.trim() || !form.password.trim()) throw new Error("아이디와 비밀번호를 입력해주세요.");
      if (mode === "signup") {
        if (!form.name.trim() || !form.nickname.trim()) throw new Error("이름과 닉네임을 입력해주세요.");
        if (form.password !== form.passwordConfirm) throw new Error("비밀번호가 일치하지 않습니다.");
        if (!idCheck || idCheck.value !== form.loginId.trim() || !idCheck.available) {
          throw new Error("아이디 중복확인을 해주세요.");
        }
        if (!nicknameCheck || nicknameCheck.value !== form.nickname.trim() || !nicknameCheck.available) {
          throw new Error("닉네임 중복확인을 해주세요.");
        }
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
          {mode === "signup" ? (
            <div className="field-with-action">
              <input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" />
              <button type="button" onClick={checkId} disabled={loading}>중복확인</button>
            </div>
          ) : (
            <input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" />
          )}
          {mode === "signup" && idCheck && idCheck.value === form.loginId.trim() && (
            <p className={`field-hint ${idCheck.available ? "ok" : "error"}`}>
              {idCheck.available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다."}
            </p>
          )}
          <input name="password" type="password" value={form.password} onChange={change} placeholder="비밀번호" />
          {mode === "signup" && (
            <>
              <input name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={change} placeholder="비밀번호 확인" />
              <input name="name" value={form.name} onChange={change} placeholder="이름" />
              <div className="field-with-action">
                <input name="nickname" value={form.nickname} onChange={change} placeholder="닉네임" />
                <button type="button" onClick={checkNickname} disabled={loading}>중복확인</button>
              </div>
              {nicknameCheck && nicknameCheck.value === form.nickname.trim() && (
                <p className={`field-hint ${nicknameCheck.available ? "ok" : "error"}`}>
                  {nicknameCheck.available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다."}
                </p>
              )}
            </>
          )}
          <button disabled={loading}>{mode === "login" ? "로그인" : "가입하기"}</button>
        </form>
      </section>
    </main>
  );
}
