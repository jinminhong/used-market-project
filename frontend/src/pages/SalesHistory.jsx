import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, Package } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeOrder } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";

export default function SalesHistory() {
  const { api, setNotice } = useSession();
  const [orders, setOrders] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setOrders(null);

    (async () => {
      try {
        const data = await api.listSales();
        if (cancelled) return;
        setOrders((data ?? []).map((order, index) => normalizeOrder(order, index + 1)));
      } catch (error) {
        if (cancelled) return;
        setOrders([]);
        setNotice(error.message || "판매내역을 불러오지 못했습니다.");
      }
    })();

    return () => { cancelled = true; };
  }, [api]);

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
      <section className="shop-hero">
        <div className="shop-avatar"><Package size={20} /></div>
        <p>My sales</p>
        <h1>판매내역</h1>
      </section>
      {orders && orders.length === 0 && <p className="quiet-message">아직 판매한 상품이 없습니다.</p>}
      {orders && orders.length > 0 && (
        <ul className="order-list">
          {orders.map((order) => (
            <li key={order.orderId} className="order-row">
              <Link to={`/items/${order.item.itemId}`} className="order-row-thumb">
                <img src={order.item.imageUrl} alt={order.item.name} />
              </Link>
              <div className="order-row-body">
                <strong>{order.item.name}</strong>
                <span>{order.item.price.toLocaleString()}원</span>
                <em>구매자 @{order.buyerNickName || "알 수 없음"}</em>
              </div>
              <StatusPill status={order.item.status} />
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
