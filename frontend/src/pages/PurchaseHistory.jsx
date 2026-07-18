import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, ShoppingBag } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizePurchase } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";

const PAGE_SIZE = 10;

export default function PurchaseHistory() {
  const { api, setNotice, loading, setLoading } = useSession();
  const [orders, setOrders] = useState(null);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const apiRef = useRef(null);
  const inFlightRequestRef = useRef("");
  const loadedPagesRef = useRef(new Set());

  async function loadPurchases(targetPage = 0, append = false) {
    const requestKey = `${append ? "append" : "replace"}:${targetPage}`;

    if (append && !hasNext) return;
    if (loadedPagesRef.current.has(targetPage)) return;
    if (inFlightRequestRef.current === requestKey) return;
    inFlightRequestRef.current = requestKey;

    if (append) setLoadingMore(true);
    else setLoading(true);

    try {
      const data = await api.listPurchases(targetPage, PAGE_SIZE);
      const list = data?.list ?? [];
      const normalizedOrders = list.map(normalizePurchase);

      loadedPagesRef.current.add(targetPage);
      setOrders((current) => (append ? [...(current ?? []), ...normalizedOrders] : normalizedOrders));
      setHasNext(Boolean(data?.hasNext));
      setPage(targetPage + 1);
    } catch (error) {
      if (!append) setOrders([]);
      setNotice(error.message || "구매내역을 불러오지 못했습니다.");
    } finally {
      if (inFlightRequestRef.current === requestKey) inFlightRequestRef.current = "";
      if (append) setLoadingMore(false);
      else setLoading(false);
    }
  }

  useEffect(() => {
    if (apiRef.current === api) return;
    apiRef.current = api;
    inFlightRequestRef.current = "";
    loadedPagesRef.current = new Set();
    setPage(0);
    setHasNext(true);
    loadPurchases(0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  useEffect(() => {
    function handleScroll() {
      const scrollBottom = window.innerHeight + window.scrollY;
      const documentHeight = document.documentElement.scrollHeight;
      const shouldLoadMore = documentHeight - scrollBottom < 500;

      if (shouldLoadMore && hasNext && !loadingMore && !loading) {
        loadPurchases(page, true);
      }
    }

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasNext, loadingMore, loading, page, api]);

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
      <section className="shop-hero">
        <div className="shop-avatar"><ShoppingBag size={20} /></div>
        <p>My purchases</p>
        <h1>구매내역</h1>
      </section>
      {orders && orders.length === 0 && <p className="quiet-message">아직 구매한 상품이 없습니다.</p>}
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
                <em>@{order.sellerNickName}</em>
              </div>
              <StatusPill status={order.item.status} />
            </li>
          ))}
        </ul>
      )}
      {loadingMore && <p className="quiet-message">구매내역을 더 불러오는 중입니다.</p>}
      {orders && orders.length > 0 && !hasNext && <p className="quiet-message">마지막 구매내역까지 모두 봤습니다.</p>}
    </main>
  );
}
