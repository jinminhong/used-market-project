import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, Heart } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeItem } from "../api/normalize.js";
import ItemCard from "../components/ItemCard.jsx";

export default function Wishlist() {
  const { api, setNotice } = useSession();
  const [items, setItems] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setItems(null);

    (async () => {
      try {
        const data = await api.listWishlist();
        if (cancelled) return;
        const list = Array.isArray(data) ? data : data?.list ?? [];
        setItems(list.map((item, index) => normalizeItem(item, item.itemId ?? index + 1)));
      } catch (error) {
        if (cancelled) return;
        setItems([]);
        setNotice(error.message || "위시리스트를 불러오지 못했습니다.");
      }
    })();

    return () => { cancelled = true; };
  }, [api]);

  return (
    <main className="page-shell">
      <section className="shop-hero">
        <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
        <div className="shop-avatar"><Heart size={20} /></div>
        <p>My wishlist</p>
        <h1>위시리스트</h1>
      </section>
      {items && items.length > 0 && (
        <section className="product-grid">
          {items.map((item, index) => (
            <ItemCard key={`${item.itemId ?? item.name}-${index}`} item={item} />
          ))}
        </section>
      )}
      {items && items.length === 0 && <p className="quiet-message">찜한 상품이 없습니다.</p>}
    </main>
  );
}
