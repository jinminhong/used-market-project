import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ChevronLeft, ShoppingBag } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizePurchase } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";
import OrderStatusPill from "../components/OrderStatusPill.jsx";
import { Button } from "../components/ui/button.jsx";
import { Tabs, TabsList, TabsTrigger } from "../components/ui/tabs.jsx";

const PAGE_SIZE = 10;
const TAB_STATUSES = {
  pending: ["REQUESTED"],
  agreed: ["RESERVED"],
  completed: ["PAY_COMPLETED", "SHIPPING", "COMPLETED"],
};

export default function PurchaseHistory() {
  const { api, run, setNotice, loading, setLoading } = useSession();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get("tab");
  const tab = tabParam === "pending" || tabParam === "agreed" ? tabParam : "completed";
  const [orders, setOrders] = useState(null);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const requestKeyRef = useRef("");
  const inFlightRequestRef = useRef("");
  const loadedPagesRef = useRef(new Set());

  async function loadPurchases(targetPage = 0, append = false) {
    const requestKey = `${append ? "append" : "replace"}:${tab}:${targetPage}`;

    if (append && !hasNext) return;
    if (loadedPagesRef.current.has(targetPage)) return;
    if (inFlightRequestRef.current === requestKey) return;
    inFlightRequestRef.current = requestKey;

    if (append) setLoadingMore(true);
    else setLoading(true);

    try {
      const condition = { status: TAB_STATUSES[tab] };
      const data = await api.listPurchases(targetPage, PAGE_SIZE, condition);
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
    const key = `${api ? "api" : "no-api"}:${tab}`;
    if (requestKeyRef.current === key) return; // StrictMode 개발 모드 재실행 스킵(중복 요청 방지)
    requestKeyRef.current = key;
    inFlightRequestRef.current = "";
    loadedPagesRef.current = new Set();
    setPage(0);
    setHasNext(true);
    loadPurchases(0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api, tab]);

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
  }, [hasNext, loadingMore, loading, page, api, tab]);

  function changeTab(nextTab) {
    const next = new URLSearchParams(searchParams);
    if (nextTab === "completed") next.delete("tab");
    else next.set("tab", nextTab);
    setSearchParams(next);
  }

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

  function goToOrderCheckout(order) {
    navigate(`/orders/${order.orderId}/checkout`, {
      state: {
        orderId: order.orderId,
        agreedPrice: order.agreedPrice,
        item: {
          itemId: order.item.itemId,
          name: order.item.name,
          imageUrl: order.item.imageUrl,
          nickName: order.sellerNickName,
        },
      },
    });
  }

  const emptyMessage =
    tab === "pending" ? "제안 중인 내역이 없습니다." : tab === "agreed" ? "합의된 내역이 없습니다." : "아직 구매한 상품이 없습니다.";

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
      <section className="shop-hero">
        <div className="shop-avatar"><ShoppingBag size={20} /></div>
        <p>My purchases</p>
        <h1>구매내역</h1>
      </section>
      <Tabs value={tab} onValueChange={changeTab}>
        <TabsList className="w-full">
          <TabsTrigger value="completed" className="flex-1">구매완료</TabsTrigger>
          <TabsTrigger value="pending" className="flex-1">제안 중인 내역</TabsTrigger>
          <TabsTrigger value="agreed" className="flex-1">합의된 내역</TabsTrigger>
        </TabsList>
      </Tabs>
      {orders && orders.length === 0 && <p className="quiet-message">{emptyMessage}</p>}
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
                <em>@{order.sellerNickName}</em>
              </div>
              <div className="order-row-status">
                <StatusPill status={order.item.status} />
                <OrderStatusPill status={order.orderStatus} />
              </div>
              <div className="order-row-actions">
                {order.orderStatus === "REQUESTED" && (
                  <Button size="sm" variant="outline" disabled={loading} onClick={() => handleAction(order.orderId, "CANCEL", "구매 요청을 취소했습니다.")}>취소</Button>
                )}
                {order.orderStatus === "RESERVED" && (
                  <>
                    <Button size="sm" disabled={loading} onClick={() => goToOrderCheckout(order)}>구매하기</Button>
                    <Button size="sm" variant="outline" disabled={loading} onClick={() => handleAction(order.orderId, "CANCEL", "주문을 취소했습니다.")}>취소</Button>
                  </>
                )}
                {order.orderStatus === "PAY_COMPLETED" && <span className="quiet-message">판매자 동의 대기 중</span>}
                {order.orderStatus === "SHIPPING" && (
                  <Button size="sm" disabled={loading} onClick={() => handleAction(order.orderId, "CONFIRM", "구매를 확정했습니다.")}>구매확정</Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
      {loadingMore && <p className="quiet-message">구매내역을 더 불러오는 중입니다.</p>}
      {orders && orders.length > 0 && !hasNext && <p className="quiet-message">마지막 구매내역까지 모두 봤습니다.</p>}
    </main>
  );
}
