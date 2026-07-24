import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMemberInfo, formatAddress } from "../api/normalize.js";

export default function OrderCheckout() {
  const { orderId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { api, run, loading, setNotice } = useSession();
  const [buyerInfo, setBuyerInfo] = useState(null);
  const [notFound, setNotFound] = useState(false);

  const order = location.state?.orderId ? location.state : null;

  useEffect(() => {
    if (!order) {
      setNotFound(true);
      return;
    }

    (async () => {
      try {
        const myInfo = await api.getMyInfo();
        setBuyerInfo(normalizeMemberInfo(myInfo));
      } catch (error) {
        setNotFound(true);
        setNotice(error.message || "구매자 정보를 불러오지 못했습니다.");
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  if (notFound) {
    return (
      <main className="page-shell narrow-page">
        <Link className="text-button" to="/profile/purchases?tab=agreed"><ChevronLeft size={16} /> 구매내역으로</Link>
        <p className="quiet-message">주문 정보를 확인할 수 없습니다. 채팅방 또는 구매내역에서 다시 시도해주세요.</p>
      </main>
    );
  }

  if (!buyerInfo) return null;

  const addressText = formatAddress(buyerInfo.address);
  const hasAddress = Boolean(addressText);

  async function handlePay() {
    await run(async () => {
      await api.changeOrderStatus(order.orderId, "PAY");
      navigate("/profile/purchases");
    }, "결제가 완료되었습니다.");
  }

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to="/chat"><ChevronLeft size={16} /> 채팅 목록</Link>
      <section className="shop-hero">
        <p>Checkout</p>
        <h1>주문 결제</h1>
      </section>
      <section className="checkout-summary">
        <div className="checkout-item">
          <img src={order.item.imageUrl} alt={order.item.name} />
          <div className="checkout-item-body">
            <strong>{order.item.name}</strong>
            <span>{Number(order.agreedPrice ?? 0).toLocaleString()}원 (협의된 가격)</span>
            <em>@{order.item.nickName}</em>
          </div>
        </div>
        <div className="checkout-address">
          <h2>배송지</h2>
          {hasAddress ? <p>{addressText}</p> : <p className="quiet-message">등록된 배송지가 없습니다. 결제를 진행하려면 배송지를 먼저 등록해주세요.</p>}
          {!hasAddress && (
            <button type="button" onClick={() => navigate(`/profile/edit?next=${encodeURIComponent(`/orders/${order.orderId}/checkout`)}`)}>배송지 등록하러 가기</button>
          )}
        </div>
        <button type="button" className="shop-cta" onClick={handlePay} disabled={!hasAddress || loading}>
          결제하기
        </button>
      </section>
    </main>
  );
}
