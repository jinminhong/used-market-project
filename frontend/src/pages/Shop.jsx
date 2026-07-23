import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft, Plus } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeShop } from "../api/normalize.js";
import ItemCard from "../components/ItemCard.jsx";

export default function Shop() {
  const { memberId } = useParams();
  const { api, member, setNotice } = useSession();
  const navigate = useNavigate();
  const [shop, setShop] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const isOwnShop = Boolean(member) && member.memberId === Number(memberId);
  const shopFetchKeyRef = useRef("");

  useEffect(() => {
    const key = String(memberId);
    if (shopFetchKeyRef.current === key) return; // StrictMode 개발 모드 재실행 스킵(중복 요청 방지)
    shopFetchKeyRef.current = key;

    setShop(null);
    setNotFound(false);

    (async () => {
      try {
        const data = await api.findShop(Number(memberId));
        if (shopFetchKeyRef.current !== key) return;
        setShop(normalizeShop({ ...data, memberId: Number(memberId) }));
      } catch (error) {
        if (shopFetchKeyRef.current !== key) return;
        setNotFound(true);
        setNotice(error.message || "상점 정보를 불러오지 못했습니다.");
      }
    })();
  }, [api, memberId]);

  if (notFound) {
    return (
      <main className="page-shell narrow-page">
        <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
        <p className="quiet-message">상점 정보를 찾을 수 없습니다.</p>
      </main>
    );
  }

  if (!shop) return null;

  return (
    <main className="page-shell">
      <section className="shop-hero">
        <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
        <div className="shop-avatar">{shop.nickName.slice(0, 1).toUpperCase()}</div>
        <p>Seller shop</p>
        <h1>@{shop.nickName}</h1>
        <span>{shop.items.length} items</span>
      </section>
      {isOwnShop && (
        <button type="button" className="shop-cta" onClick={() => navigate("/items/new")}>
          <Plus size={20} />
          <span>상품 등록</span>
        </button>
      )}
      <section className="product-grid">
        {shop.items.map((item, index) => (
          <ItemCard key={`${item.itemId ?? item.name}-${index}`} item={item} />
        ))}
      </section>
      {shop.items.length === 0 && <p className="quiet-message">등록된 상품이 없습니다.</p>}
    </main>
  );
}
