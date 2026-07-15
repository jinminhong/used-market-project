import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMemberInfo } from "../api/normalize.js";

const emptyForm = { name: "", nickname: "", password: "", city: "", street: "", zipcode: "" };

export default function ProfileEdit() {
  const navigate = useNavigate();
  const { api, setMember, run, loading, setNotice } = useSession();
  const [form, setForm] = useState(emptyForm);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        const data = await api.getMyInfo();
        const info = normalizeMemberInfo(data);
        if (cancelled) return;
        setForm({
          name: info.name,
          nickname: info.nickName,
          password: "",
          city: info.address.city,
          street: info.address.street,
          zipcode: info.address.zipcode,
        });
        setReady(true);
      } catch (error) {
        if (cancelled) return;
        setNotice(error.message || "회원정보를 불러오지 못했습니다.");
        navigate("/profile", { replace: true });
      }
    })();

    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    await run(async () => {
      if (!form.name.trim() || !form.nickname.trim()) throw new Error("이름과 닉네임을 입력해주세요.");
      const payload = {
        name: form.name,
        nickname: form.nickname,
        address: { city: form.city, street: form.street, zipcode: form.zipcode },
      };
      if (form.password.trim()) payload.password = form.password;
      await api.updateMyInfo(payload);
      setMember((current) => (current ? { ...current, nickName: form.nickname } : current));
      navigate("/profile");
    }, "회원정보가 수정되었습니다.");
  }

  if (!ready) return null;

  return (
    <main className="form-page">
      <section className="form-panel wide-panel">
        <p>Account settings</p>
        <h1>개인정보 수정</h1>
        <form onSubmit={submit}>
          <input name="name" value={form.name} onChange={change} placeholder="이름" />
          <input name="nickname" value={form.nickname} onChange={change} placeholder="닉네임" />
          <input name="password" type="password" value={form.password} onChange={change} placeholder="새 비밀번호 (변경 시에만 입력)" />
          <div className="split-fields">
            <input name="city" value={form.city} onChange={change} placeholder="시/도" />
            <input name="street" value={form.street} onChange={change} placeholder="도로명 주소" />
          </div>
          <input name="zipcode" value={form.zipcode} onChange={change} placeholder="우편번호" />
          <button disabled={loading}>저장</button>
        </form>
        <button className="text-button" type="button" onClick={() => navigate("/profile")}>취소</button>
      </section>
    </main>
  );
}
