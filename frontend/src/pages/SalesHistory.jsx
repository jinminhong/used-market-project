import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, Package } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizePurchase } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";
import OrderStatusPill from "../components/OrderStatusPill.jsx";
import { Button } from "../components/ui/button.jsx";

const PAGE_SIZE = 10;

export default function SalesHistory() {
  const { api, run, setNotice, loading, setLoading } = useSession();
  const [orders, setOrders] = useState(null);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const apiRef = useRef(null);
  const inFlightRequestRef = useRef("");
  const loadedPagesRef = useRef(new Set());

  async function loadSales(targetPage = 0, append = false) {
    const requestKey = `${append ? "append" : "replace"}:${targetPage}`;

    if (append && !hasNext) return;
    if (loadedPagesRef.current.has(targetPage)) return;
    if (inFlightRequestRef.current === requestKey) return;
    inFlightRequestRef.current = requestKey;

    if (append) setLoadingMore(true);
    else setLoading(true);

    try {
      const data = await api.listSales(targetPage, PAGE_SIZE);
      const list = data?.list ?? [];
      const normalizedOrders = list.map(normalizePurchase);

      loadedPagesRef.current.add(targetPage);
      setOrders((current) => (append ? [...(current ?? []), ...normalizedOrders] : normalizedOrders));
      setHasNext(Boolean(data?.hasNext));
      setPage(targetPage + 1);
    } catch (error) {
      if (!append) setOrders([]);
      setNotice(error.message || "판매내역을 불러오지 못했습니다.");
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
    loadSales(0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  useEffect(() => {
    function handleScroll() {
      const scrollBottom = window.innerHeight + window.scrollY;
      const documentHeight = document.documentElement.scrollHeight;
      const shouldLoadMore = documentHeight - scrollBottom < 500;

      if (shouldLoadMore && hasNext && !loadingMore && !loading) {
        loadSales(page, true);
      }
    }

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasNext, loadingMore, loading, page, api]);

  async function handleAction(orderId, action, message) {
    await run(async () => {
      const result = await api.changeOrderStatus(orderId, action);
      setOrders((current) =>
        current.map((order) =>
          order.orderId === orderId
            ? { ...order, orderStatus: result.status, item: { ...order.item, status: result.itemStatus } }
            : order
        )
      );
    }, message);
  }

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
                <span>{(order.agreedPrice ?? order.item.price).toLocaleString()}원</span>
                <em>{order.purchaseDate ? new Date(order.purchaseDate).toLocaleDateString() : ""}</em>
              </div>
              <div className="order-row-status">
                <StatusPill status={order.item.status} />
                <OrderStatusPill status={order.orderStatus} />
              </div>
              <div className="order-row-actions">
                {order.orderStatus === "REQUESTED" && (
                  <>
                    <Button size="sm" disabled={loading} onClick={() => handleAction(order.orderId, "ACCEPT", "주문을 승인했습니다.")}>승인</Button>
                    <Button size="sm" variant="outline" disabled={loading} onClick={() => handleAction(order.orderId, "CANCEL", "주문을 거절했습니다.")}>거절</Button>
                  </>
                )}
                {order.orderStatus === "ACCEPTED" && <span className="quiet-message">구매자 결제 대기 중</span>}
                {order.orderStatus === "PAY_COMPLETED" && (
                  <>
                    <Button size="sm" disabled={loading} onClick={() => handleAction(order.orderId, "SHIP", "발송 처리했습니다.")}>발송 처리</Button>
                    <Button size="sm" variant="outline" disabled={loading} onClick={() => handleAction(order.orderId, "CANCEL", "주문을 취소했습니다.")}>취소</Button>
                  </>
                )}
                {order.orderStatus === "SHIPPING" && (
                  <Button size="sm" variant="outline" disabled={loading} onClick={() => handleAction(order.orderId, "CANCEL", "주문을 취소했습니다.")}>취소(귀책사유)</Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
      {loadingMore && <p className="quiet-message">판매내역을 더 불러오는 중입니다.</p>}
      {orders && orders.length > 0 && !hasNext && <p className="quiet-message">마지막 판매내역까지 모두 봤습니다.</p>}
    </main>
  );
}
