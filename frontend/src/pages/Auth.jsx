import { useState } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMember } from "../api/normalize.js";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs.jsx";
import { Input } from "../components/ui/input.jsx";
import { Button } from "../components/ui/button.jsx";
import AddressSearchField from "../components/AddressSearchField.jsx";

export default function Auth() {
  const { api, member, setMember, run, loading, setNotice } = useSession();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({
    loginId: "asd",
    name: "",
    password: "1234",
    passwordConfirm: "",
    nickname: "",
    zonecode: "",
    roadAddress: "",
    jibunAddress: "",
    detailAddress: "",
  });
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

  function handleAddressSearch(address) {
    setForm((current) => ({ ...current, ...address }));
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
        await api.signup({
          ...form,
          address: {
            zonecode: form.zonecode,
            roadAddress: form.roadAddress,
            jibunAddress: form.jibunAddress,
            detailAddress: form.detailAddress,
          },
        });
        const signedUpMember = await api.login({ loginId: form.loginId, password: form.password });
        setMember(normalizeMember(signedUpMember));
        navigate(searchParams.get("next") || "/");
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
        <Tabs value={mode} onValueChange={setMode}>
          <TabsList className="w-full">
            <TabsTrigger value="login" className="flex-1">로그인</TabsTrigger>
            <TabsTrigger value="signup" className="flex-1">회원가입</TabsTrigger>
          </TabsList>
        </Tabs>
        <p>{mode === "login" ? "Welcome back" : "Create account"}</p>
        <h1>{mode === "login" ? "로그인" : "회원가입"}</h1>
        <form onSubmit={submit}>
          {mode === "signup" ? (
            <div className="field-with-action">
              <Input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" />
              <Button type="button" variant="outline" size="sm" onClick={checkId} disabled={loading}>중복확인</Button>
            </div>
          ) : (
            <Input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" />
          )}
          {mode === "signup" && idCheck && idCheck.value === form.loginId.trim() && (
            <p className={`field-hint ${idCheck.available ? "ok" : "error"}`}>
              {idCheck.available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다."}
            </p>
          )}
          <Input name="password" type="password" value={form.password} onChange={change} placeholder="비밀번호" />
          {mode === "signup" && (
            <>
              <Input name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={change} placeholder="비밀번호 확인" />
              <Input name="name" value={form.name} onChange={change} placeholder="이름" />
              <div className="field-with-action">
                <Input name="nickname" value={form.nickname} onChange={change} placeholder="닉네임" />
                <Button type="button" variant="outline" size="sm" onClick={checkNickname} disabled={loading}>중복확인</Button>
              </div>
              {nicknameCheck && nicknameCheck.value === form.nickname.trim() && (
                <p className={`field-hint ${nicknameCheck.available ? "ok" : "error"}`}>
                  {nicknameCheck.available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다."}
                </p>
              )}
              <AddressSearchField
                zonecode={form.zonecode}
                roadAddress={form.roadAddress}
                detailAddress={form.detailAddress}
                onSearch={handleAddressSearch}
                onDetailChange={change}
                disabled={loading}
              />
            </>
          )}
          <Button className="w-full" disabled={loading}>{mode === "login" ? "로그인" : "가입하기"}</Button>
        </form>
      </section>
    </main>
  );
}
