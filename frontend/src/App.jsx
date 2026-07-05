import { useEffect, useMemo, useState } from "react";

const CATEGORIES = ["All", "OUTER", "TOP", "BOTTOM", "BAG", "SHOES", "ACCESSORY", "ETC"];
const STATUSES = ["SELLING", "RESERVED", "SOLD"];

const sampleItems = [
  { itemId: 1, name: "Archive Leather Jacket", description: "부드러운 양가죽 소재의 빈티지 재킷입니다. 자연스러운 사용감이 있고 전체적인 컨디션은 좋습니다.", price: 182000, status: "SELLING", nickName: "hong", category: "OUTER", imageUrl: "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=900&q=80" },
  { itemId: 2, name: "Washed Cotton Shirt", description: "워싱감이 예쁜 코튼 셔츠입니다. 단품이나 이너로 활용하기 좋습니다.", price: 46000, status: "RESERVED", nickName: "lee", category: "TOP", imageUrl: "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?auto=format&fit=crop&w=900&q=80" },
  { itemId: 3, name: "Minimal Cross Bag", description: "데일리로 들기 좋은 크로스백입니다. 내부 수납 깨끗합니다.", price: 69000, status: "SELLING", nickName: "hong", category: "BAG", imageUrl: "https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=80" },
  { itemId: 4, name: "Suede Loafers", description: "스웨이드 로퍼 260 사이즈입니다. 밑창 마모 적습니다.", price: 88000, status: "SOLD", nickName: "mori", category: "SHOES", imageUrl: "https://images.unsplash.com/photo-1614252369475-531eba835eb1?auto=format&fit=crop&w=900&q=80" },
];

function defaultImage() {
  return "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80";
}

function imageUrlFromItemImages(itemImages) {
  const storedFilename = itemImages?.[0]?.storedFilename;
  return storedFilename ? `/api/images/${encodeURIComponent(storedFilename)}` : "";
}

function normalizeMember(member) {
  if (!member) return null;
  return {
    memberId: member.memberId ?? member.id ?? null,
    loginId: member.loginId ?? "",
    nickName: member.nickName ?? member.nickname ?? "",
  };
}

function normalizeItem(item, fallbackId) {
  return {
    itemId: item.itemId ?? item.id ?? fallbackId ?? null,
    name: item.name ?? item.title ?? "이름 없는 상품",
    description: item.description ?? "",
    price: Number(item.price ?? 0),
    status: item.status ?? "SELLING",
    nickName: item.nickName ?? item.nickname ?? "",
    category: item.category ?? "ETC",
    imageUrl: item.imageUrl || imageUrlFromItemImages(item.itemImages) || defaultImage(),
  };
}

function buildItemFormData(data) {
  const itemSaveDto = {
    name: data.name,
    description: data.description,
    price: Number(data.price),
    category: data.category === "All" ? "ETC" : data.category,
  };
  const formData = new FormData();
  formData.append("itemSaveDto", new Blob([JSON.stringify(itemSaveDto)], { type: "application/json" }));
  data.imageFiles.forEach((file) => formData.append("multipartFiles", file));
  return formData;
}

function createApi(useMock) {
  let mockItems = [...sampleItems];
  let mockMember = null;

  async function request(path, options = {}) {
    const method = options.method || "GET";
    const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
    const body = options.body && !isFormData ? JSON.parse(options.body) : null;

    if (useMock) return mockRequest(path, method, body);

    const { headers: optionHeaders, ...fetchOptions } = options;
    const headers = isFormData ? optionHeaders : { "Content-Type": "application/json", ...optionHeaders };
    const response = await fetch(`/api${path}`, {
      credentials: "include",
      ...fetchOptions,
      headers,
    });
    if (!response.ok) throw new Error((await response.text()) || `요청 실패 (${response.status})`);
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
  }

  async function mockRequest(path, method, body) {
    await new Promise((resolve) => setTimeout(resolve, 140));
    if (path === "/login" && method === "POST") {
      mockMember = { memberId: body.loginId === "lee" ? 2 : 1, loginId: body.loginId, nickName: body.loginId === "lee" ? "lee" : "hong" };
      return mockMember;
    }
    if (path === "/members/add" && method === "POST") return body;
    if (path === "/items" && method === "GET") return mockItems;
    if (path === "/items" && method === "POST") {
      if (!mockMember) throw new Error("로그인이 필요합니다.");
      const item = { ...body, itemId: Date.now(), price: Number(body.price), status: "SELLING", nickName: mockMember.nickName, imageUrl: body.imageUrl || defaultImage() };
      mockItems = [item, ...mockItems];
      return item;
    }
    const itemId = Number(path.match(/\/items\/(\d+)/)?.[1]);
    if (path.startsWith("/items/") && method === "GET") return mockItems.find((item) => item.itemId === itemId) ?? null;
    if (path.startsWith("/items/") && method === "PATCH") {
      mockItems = mockItems.map((item) => item.itemId === itemId ? { ...item, ...body, price: Number(body.price) } : item);
      return mockItems.find((item) => item.itemId === itemId);
    }
    if (path.endsWith("/delete") && method === "POST") {
      mockItems = mockItems.filter((item) => item.itemId !== itemId);
      return null;
    }
    throw new Error("정의되지 않은 요청입니다.");
  }

  return {
    login: (data) => request("/login", { method: "POST", body: JSON.stringify(data) }),
    signup: (data) => request("/members/add", { method: "POST", body: JSON.stringify(data) }),
    listItems: () => request("/items"),
    findItem: (id) => request(`/items/${id}`),
    createItem: (data) => useMock
      ? request("/items", { method: "POST", body: JSON.stringify({ ...data, imageUrl: data.imagePreview || data.imageUrl }) })
      : request("/items", { method: "POST", body: buildItemFormData(data) }),
    updateItem: (id, data) => request(`/items/${id}`, { method: "PATCH", body: JSON.stringify(data) }),
    deleteItem: (id) => request(`/items/${id}/delete`, { method: "POST" }),
  };
}

function Nav({ member, useMock, setUseMock, go, logout }) {
  return (
    <header className="site-header">
      <button className="brand" type="button" onClick={() => go("home")}>Fruits Market</button>
      <nav className="nav-links">
        <button type="button" onClick={() => go("home")}>Shop</button>
        <button type="button" onClick={() => go("new")}>Sell</button>
      </nav>
      <div className="nav-actions">
        <label className="mock-toggle"><input type="checkbox" checked={useMock} onChange={(event) => setUseMock(event.target.checked)} /> Mock</label>
        {member ? <><span className="member-chip">@{member.nickName}</span><button type="button" onClick={logout}>Logout</button></> : <button type="button" onClick={() => go("auth")}>Login</button>}
      </div>
    </header>
  );
}

function Home({ items, category, search, setCategory, setSearch, openItem }) {
  const visibleItems = items.filter((item) => {
    const keyword = search.trim().toLowerCase();
    return (category === "All" || item.category === category) && (!keyword || `${item.name} ${item.description} ${item.nickName}`.toLowerCase().includes(keyword));
  });

  return (
    <main className="page-shell">
      <section className="hero-strip"><div><p>Latest vintage drops</p><h1>개성 있는 셀러들의 상품을 둘러보세요</h1></div><div className="hero-count"><strong>{items.length}</strong><span>items</span></div></section>
      <section className="shop-toolbar">
        <div className="category-tabs">{CATEGORIES.map((name) => <button key={name} type="button" className={category === name ? "active" : ""} onClick={() => setCategory(name)}>{name}</button>)}</div>
        <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="상품, 설명, 셀러 검색" />
      </section>
      <section className="product-grid">
        {visibleItems.map((item, index) => <button className="product-card" type="button" key={`${item.itemId ?? item.name}-${index}`} onClick={() => openItem(item)}><span className="product-image-wrap"><img src={item.imageUrl} alt="" /><span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span></span><span className="product-info"><strong>{item.name}</strong><span>{item.price.toLocaleString()}원</span><em>@{item.nickName}</em></span></button>)}
      </section>
      {visibleItems.length === 0 && <p className="quiet-message">조건에 맞는 상품이 없습니다.</p>}
    </main>
  );
}

function Detail({ item, member, go, edit, remove, loading }) {
  if (!item) return <main className="page-shell narrow-page"><button className="text-button" type="button" onClick={() => go("home")}>Back to shop</button><p className="quiet-message">상품을 찾을 수 없습니다.</p></main>;
  const isOwner = member?.nickName === item.nickName;
  const canMutate = isOwner && item.itemId;

  return (
    <main className="detail-page">
      <button className="text-button" type="button" onClick={() => go("home")}>Back to shop</button>
      <section className="detail-layout">
        <div className="detail-image"><img src={item.imageUrl} alt="" /></div>
        <aside className="detail-info">
          <div className="seller-line"><span>@{item.nickName}</span><span className={`status-pill ${item.status.toLowerCase()}`}>{item.status}</span></div>
          <h1>{item.name}</h1><strong className="detail-price">{item.price.toLocaleString()}원</strong><p>{item.description || "등록된 설명이 없습니다."}</p>
          {isOwner ? <div className="owner-actions"><button type="button" onClick={edit} disabled={!canMutate || loading}>수정하기</button><button className="danger" type="button" onClick={remove} disabled={!canMutate || loading}>삭제하기</button>{!item.itemId && <small>현재 목록 응답에 상품 ID가 없어 실제 수정/삭제 요청은 보낼 수 없습니다.</small>}</div> : <div className="buyer-actions"><button type="button" onClick={() => !member && go("auth")}>{member ? "구매 문의" : "로그인하고 문의하기"}</button></div>}
        </aside>
      </section>
    </main>
  );
}

function Auth({ mode, setMode, form, change, submit, loading }) {
  return <main className="form-page"><section className="form-panel"><p>{mode === "login" ? "Welcome back" : "Create account"}</p><h1>{mode === "login" ? "로그인" : "회원가입"}</h1><form onSubmit={submit}><input name="loginId" value={form.loginId} onChange={change} placeholder="로그인 ID" /><input name="password" type="password" value={form.password} onChange={change} placeholder="비밀번호" />{mode === "signup" && <><input name="name" value={form.name} onChange={change} placeholder="이름" /><input name="nickname" value={form.nickname} onChange={change} placeholder="닉네임" /></>}<button disabled={loading}>{mode === "login" ? "로그인" : "가입하기"}</button></form><button className="text-button" type="button" onClick={() => setMode(mode === "login" ? "signup" : "login")}>{mode === "login" ? "계정 만들기" : "이미 계정이 있어요"}</button></section></main>;
}

function Editor({ title, form, change, submit, cancel, loading, allowImageUpload }) {
  return <main className="form-page"><section className="form-panel wide-panel"><p>Seller studio</p><h1>{title}</h1><form onSubmit={submit}><input name="name" value={form.name} onChange={change} placeholder="상품명" /><div className="split-fields"><input name="price" type="number" min="0" value={form.price} onChange={change} placeholder="가격" /><select name="category" value={form.category} onChange={change}>{CATEGORIES.filter((name) => name !== "All").map((name) => <option key={name} value={name}>{name}</option>)}</select></div>{allowImageUpload ? <label className="file-drop"><input name="imageFiles" type="file" accept="image/*" multiple onChange={change} /><span>{form.imageFiles.length ? `${form.imageFiles.length}개 이미지 선택됨` : "상품 이미지 선택"}</span>{form.imagePreview && <img src={form.imagePreview} alt="" />}</label> : <input name="imageUrl" value={form.imageUrl} onChange={change} placeholder="이미지 URL" />}<textarea name="description" value={form.description} onChange={change} placeholder="상품 설명" /><div className="split-fields"><select name="status" value={form.status} onChange={change}>{STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}</select><button disabled={loading}>저장</button></div></form><button className="text-button" type="button" onClick={cancel}>취소</button></section></main>;
}

export default function App() {
  const [useMock, setUseMock] = useState(false);
  const [route, setRoute] = useState("home");
  const [items, setItems] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [member, setMember] = useState(null);
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ loginId: "asd", name: "", password: "1234", nickname: "" });
  const [itemForm, setItemForm] = useState({ name: "", description: "", price: "", status: "SELLING", category: "ETC", imageUrl: "", imageFiles: [], imagePreview: "" });
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(false);
  const api = useMemo(() => createApi(useMock), [useMock]);

  function go(next) {
    setNotice("");
    if (next === "new" && !member) { setNotice("상품을 등록하려면 로그인해주세요."); setRoute("auth"); return; }
    setRoute(next);
  }

  async function run(fn, message = "") {
    setLoading(true); setNotice("");
    try { await fn(); if (message) setNotice(message); } catch (error) { setNotice(error.message || "요청 중 오류가 발생했습니다."); } finally { setLoading(false); }
  }

  async function loadItems() {
    await run(async () => { const data = await api.listItems(); const list = Array.isArray(data) ? data : data?.items ?? []; setItems(list.map((item, index) => normalizeItem(item, index + 1))); });
  }

  async function openItem(item) {
    const normalized = normalizeItem(item);
    setSelectedItem(normalized); setRoute("detail");
    if (normalized.itemId) await run(async () => { const detail = await api.findItem(normalized.itemId).catch(() => null); if (detail && typeof detail === "object") setSelectedItem(normalizeItem(detail, normalized.itemId)); });
  }

  async function submitAuth(event) {
    event.preventDefault();
    await run(async () => {
      if (!authForm.loginId.trim() || !authForm.password.trim()) throw new Error("아이디와 비밀번호를 입력해주세요.");
      if (authMode === "signup") { if (!authForm.name.trim() || !authForm.nickname.trim()) throw new Error("이름과 닉네임을 입력해주세요."); await api.signup(authForm); setAuthMode("login"); return; }
      const loginMember = await api.login({ loginId: authForm.loginId, password: authForm.password }); setMember(normalizeMember(loginMember)); setRoute("home");
    }, authMode === "signup" ? "회원가입이 완료되었습니다." : "로그인되었습니다.");
  }

  async function submitNew(event) {
    event.preventDefault();
    await run(async () => { if (!itemForm.name.trim() || !itemForm.price) throw new Error("상품명과 가격을 입력해주세요."); if (!itemForm.imageFiles.length) throw new Error("상품 이미지를 1장 이상 선택해주세요."); const created = normalizeItem(await api.createItem({ ...itemForm, price: Number(itemForm.price) })); const createdWithSeller = { ...created, nickName: created.nickName || member.nickName, imageUrl: created.imageUrl === defaultImage() ? itemForm.imagePreview : created.imageUrl }; setItems((current) => [createdWithSeller, ...current]); setSelectedItem(createdWithSeller); setItemForm({ name: "", description: "", price: "", status: "SELLING", category: "ETC", imageUrl: "", imageFiles: [], imagePreview: "" }); setRoute("detail"); }, "상품이 등록되었습니다.");
  }

  function startEdit() {
    setItemForm({ name: selectedItem.name, description: selectedItem.description, price: String(selectedItem.price), status: selectedItem.status, category: selectedItem.category, imageUrl: selectedItem.imageUrl, imageFiles: [], imagePreview: "" }); setRoute("edit");
  }

  function changeItemForm(event) {
    const { name, value, files } = event.target;
    if (name === "imageFiles") {
      const imageFiles = Array.from(files ?? []);
      setItemForm((current) => {
        if (current.imagePreview?.startsWith("blob:")) URL.revokeObjectURL(current.imagePreview);
        return { ...current, imageFiles, imagePreview: imageFiles[0] ? URL.createObjectURL(imageFiles[0]) : "" };
      });
      return;
    }
    setItemForm((current) => ({ ...current, [name]: value }));
  }

  async function submitEdit(event) {
    event.preventDefault();
    await run(async () => { const updated = normalizeItem(await api.updateItem(selectedItem.itemId, { ...itemForm, price: Number(itemForm.price) }), selectedItem.itemId); setSelectedItem(updated); setItems((current) => current.map((item) => item.itemId === selectedItem.itemId ? updated : item)); setRoute("detail"); }, "상품이 수정되었습니다.");
  }

  async function removeItem() {
    await run(async () => { await api.deleteItem(selectedItem.itemId); setItems((current) => current.filter((item) => item.itemId !== selectedItem.itemId)); setSelectedItem(null); setRoute("home"); }, "상품이 삭제되었습니다.");
  }

  useEffect(() => { loadItems(); }, [api]);

  return <><Nav member={member} useMock={useMock} setUseMock={setUseMock} go={go} logout={() => { setMember(null); setRoute("home"); }} />{notice && <div className="toast">{notice}</div>}{route === "home" && <Home items={items} category={category} search={search} setCategory={setCategory} setSearch={setSearch} openItem={openItem} />}{route === "detail" && <Detail item={selectedItem} member={member} go={go} edit={startEdit} remove={removeItem} loading={loading} />}{route === "auth" && <Auth mode={authMode} setMode={setAuthMode} form={authForm} change={(event) => setAuthForm((current) => ({ ...current, [event.target.name]: event.target.value }))} submit={submitAuth} loading={loading} />}{route === "new" && <Editor title="상품 등록" form={itemForm} change={changeItemForm} submit={submitNew} cancel={() => go("home")} loading={loading} allowImageUpload />}{route === "edit" && <Editor title="상품 수정" form={itemForm} change={changeItemForm} submit={submitEdit} cancel={() => go("detail")} loading={loading} />}</>;
}




