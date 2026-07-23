import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronLeft, Truck } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizePurchase } from "../api/normalize.js";
import { Button } from "../components/ui/button.jsx";

const PAGE_SIZE = 10;

export default function ShippingManagement() {
  const { api, setNotice, loading, setLoading } = useSession();
  const navigate = useNavigate();
  const [orders, setOrders] = useState(null);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const apiRef = useRef(null);
  const inFlightRequestRef = useRef("");
  const loadedPagesRef = useRef(new Set());

  async function loadOrders(targetPage = 0, append = false) {
    const requestKey = `${append ? "append" : "replace"}:${targetPage}`;

    if (append && !hasNext) return;
    if (loadedPagesRef.current.has(targetPage)) return;
    if (inFlightRequestRef.current === requestKey) return;
    inFlightRequestRef.current = requestKey;

    if (append) setLoadingMore(true);
    else setLoading(true);

    try {
      const data = await api.listSales(targetPage, PAGE_SIZE, { status: "PAY_COMPLETED" });
      const list = data?.list ?? [];
      const normalizedOrders = list.map(normalizePurchase);

      loadedPagesRef.current.add(targetPage);
      setOrders((current) => (append ? [...(current ?? []), ...normalizedOrders] : normalizedOrders));
      setHasNext(Boolean(data?.hasNext));
      setPage(targetPage + 1);
    } catch (error) {
      if (!append) setOrders([]);
      setNotice(error.message || "발송 대기 목록을 불러오지 못했습니다.");
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
    loadOrders(0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  useEffect(() => {
    function handleScroll() {
      const scrollBottom = window.innerHeight + window.scrollY;
      const documentHeight = document.documentElement.scrollHeight;
      const shouldLoadMore = documentHeight - scrollBottom < 500;

      if (shouldLoadMore && hasNext && !loadingMore && !loading) {
        loadOrders(page, true);
      }
    }

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasNext, loadingMore, loading, page, api]);

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to="/profile/sales"><ChevronLeft size={16} /> Back to sales</Link>
      <section className="shop-hero">
        <div className="shop-avatar"><Truck size={20} /></div>
        <p>Shipping</p>
        <h1>발송 관리</h1>
      </section>
      {orders && orders.length === 0 && <p className="quiet-message">결제완료 후 발송을 기다리는 상품이 없습니다.</p>}
      {orders && orders.length > 0 && (
        <ul className="order-list">
          {orders.map((order) => (
            <li key={order.orderId} className="order-row">
              <Link to={`/items/${order.item.itemId}`} className="order-row-thumb">
                <img src={order.item.imageUrl} alt={order.item.name} />
              </Link>
              <div className="order-row-body">
                <strong>{order.item.name}</strong>
                <span>{(order.agreedPrice ?? order.item.price).toLocaleString()}원</span>
                {order.trackingNumber ? (
                  <em>{order.trackingCompany} {order.trackingNumber}</em>
                ) : (
                  <em>운송장 미등록</em>
                )}
              </div>
              <div className="order-row-actions">
                <Button size="sm" onClick={() => navigate(`/profile/sales/${order.orderId}/shipping`)}>
                  {order.trackingNumber ? "운송장 수정" : "운송장 등록"}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {loadingMore && <p className="quiet-message">더 불러오는 중입니다.</p>}
      {orders && orders.length > 0 && !hasNext && <p className="quiet-message">마지막까지 모두 봤습니다.</p>}
    </main>
  );
}
