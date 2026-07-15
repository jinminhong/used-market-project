import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeShop } from "../api/normalize.js";
import ItemCard from "../components/ItemCard.jsx";

export default function Shop() {
  const { memberId } = useParams();
  const { api, setNotice } = useSession();
  const [shop, setShop] = useState(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setShop(null);
    setNotFound(false);

    (async () => {
      try {
        const data = await api.findShop(Number(memberId));
        if (cancelled) return;
        setShop(normalizeShop({ ...data, memberId: Number(memberId) }));
      } catch (error) {
        if (cancelled) return;
        setNotFound(true);
        setNotice(error.message || "상점 정보를 불러오지 못했습니다.");
      }
    })();

    return () => { cancelled = true; };
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
      <section className="product-grid">
        {shop.items.map((item, index) => (
          <ItemCard key={`${item.itemId ?? item.name}-${index}`} item={item} />
        ))}
      </section>
      {shop.items.length === 0 && <p className="quiet-message">등록된 상품이 없습니다.</p>}
    </main>
  );
}
