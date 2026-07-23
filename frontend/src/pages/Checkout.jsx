import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeItem, normalizeMemberInfo } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";

export default function Checkout() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const { api, run, loading, setNotice } = useSession();
  const [item, setItem] = useState(null);
  const [buyerInfo, setBuyerInfo] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const checkoutFetchKeyRef = useRef("");

  useEffect(() => {
    const key = String(itemId);
    if (checkoutFetchKeyRef.current === key) return; // StrictMode 개발 모드 재실행 스킵(중복 요청 방지)
    checkoutFetchKeyRef.current = key;

    setItem(null);
    setBuyerInfo(null);
    setNotFound(false);

    (async () => {
      try {
        const [itemDetail, myInfo] = await Promise.all([api.findItem(Number(itemId)), api.getMyInfo()]);
        if (checkoutFetchKeyRef.current !== key) return;
        if (!itemDetail || typeof itemDetail !== "object") {
          setNotFound(true);
          return;
        }
        setItem(normalizeItem(itemDetail, Number(itemId)));
        setBuyerInfo(normalizeMemberInfo(myInfo));
      } catch (error) {
        if (checkoutFetchKeyRef.current !== key) return;
        setNotFound(true);
        setNotice(error.message || "구매 정보를 불러오지 못했습니다.");
      }
    })();
  }, [api, itemId]);

  if (notFound) {
    return (
      <main className="page-shell narrow-page">
        <Link className="text-button" to={`/items/${itemId}`}><ChevronLeft size={16} /> Back to item</Link>
        <p className="quiet-message">구매 정보를 불러올 수 없습니다.</p>
      </main>
    );
  }

  if (!item || !buyerInfo) return null;

  const addressText = [buyerInfo.address.city, buyerInfo.address.street, buyerInfo.address.zipcode].filter(Boolean).join(" ");
  const hasAddress = Boolean(addressText);
  const alreadyTaken = item.status !== "SELLING";
  const canConfirm = hasAddress && !alreadyTaken;

  async function handleConfirm() {
    await run(async () => {
      await api.buyItem(item.itemId);
      navigate("/profile/purchases");
    }, "구매가 완료되었습니다.");
  }

  return (
    <main className="page-shell narrow-page">
      <Link className="text-button" to={`/items/${itemId}`}><ChevronLeft size={16} /> Back to item</Link>
      <section className="shop-hero">
        <p>Checkout</p>
        <h1>구매 확인</h1>
      </section>
      <section className="checkout-summary">
        <div className="checkout-item">
          <img src={item.imageUrl} alt={item.name} />
          <div className="checkout-item-body">
            <strong>{item.name}</strong>
            <span>{item.price.toLocaleString()}원</span>
            <em>@{item.nickName}</em>
          </div>
          <StatusPill status={item.status} />
        </div>
        <div className="checkout-address">
          <h2>배송지</h2>
          {hasAddress ? <p>{addressText}</p> : <p className="quiet-message">등록된 배송지가 없습니다. 구매를 진행하려면 배송지를 먼저 등록해주세요.</p>}
          {!hasAddress && (
            <button type="button" onClick={() => navigate(`/profile/edit?next=${encodeURIComponent(`/items/${itemId}/checkout`)}`)}>배송지 등록하러 가기</button>
          )}
        </div>
        {alreadyTaken && <p className="quiet-message">이미 예약되었거나 판매 완료된 상품입니다.</p>}
        <button type="button" className="shop-cta" onClick={handleConfirm} disabled={!canConfirm || loading}>
          구매 확정하기
        </button>
      </section>
    </main>
  );
}
