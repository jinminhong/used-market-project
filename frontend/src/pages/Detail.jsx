import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeItem, imageUrlFromUploadFile, defaultImage } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";

export default function Detail() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const { api, member, run, loading, setNotice } = useSession();
  const [item, setItem] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setItem(null);
    setNotFound(false);
    setActiveImageIndex(0);

    (async () => {
      try {
        const detail = await api.findItem(Number(itemId));
        if (cancelled) return;
        if (detail && typeof detail === "object") setItem(normalizeItem(detail, Number(itemId)));
        else setNotFound(true);
      } catch (error) {
        if (cancelled) return;
        setNotFound(true);
        setNotice(error.message || "상품을 불러오지 못했습니다.");
      }
    })();

    return () => { cancelled = true; };
  }, [api, itemId]);

  if (notFound) {
    return (
      <main className="page-shell narrow-page">
        <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
        <p className="quiet-message">상품을 찾을 수 없습니다.</p>
      </main>
    );
  }

  if (!item) return null;

  const imageUrls = (item.itemImages ?? []).map(imageUrlFromUploadFile).filter(Boolean);
  const slideImages = imageUrls.length ? imageUrls : [item.imageUrl || defaultImage()];
  const isOwner = Boolean(member?.memberId) && member.memberId === item.memberId;
  const canMutate = isOwner && item.itemId;

  function moveSlide(step) {
    setActiveImageIndex((current) => (current + step + slideImages.length) % slideImages.length);
  }

  async function handleRemove() {
    await run(async () => {
      await api.deleteItem(item.itemId);
      navigate("/");
    }, "상품이 삭제되었습니다.");
  }

  function handleBuyerAction() {
    if (!member) {
      navigate("/auth");
      return;
    }
    setNotice("준비 중인 기능입니다.");
  }

  return (
    <main className="detail-page">
      <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
      <section className="detail-layout">
        <div className="detail-gallery">
          <div className="detail-image">
            <img src={slideImages[activeImageIndex]} alt="" />
            {slideImages.length > 1 && (
              <>
                <button className="slide-button prev" type="button" onClick={() => moveSlide(-1)} aria-label="이전 이미지">‹</button>
                <button className="slide-button next" type="button" onClick={() => moveSlide(1)} aria-label="다음 이미지">›</button>
              </>
            )}
          </div>
          {slideImages.length > 1 && (
            <div className="thumbnail-row">
              {slideImages.map((imageUrl, index) => (
                <button
                  key={`${imageUrl}-${index}`}
                  className={activeImageIndex === index ? "active" : ""}
                  type="button"
                  onClick={() => setActiveImageIndex(index)}
                  aria-label={`${index + 1}번째 이미지 보기`}
                >
                  <img src={imageUrl} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>
        <aside className="detail-info">
          <div className="seller-line">
            {item.memberId ? (
              <Link className="seller-link" to={`/shop/${item.memberId}`}>@{item.nickName}</Link>
            ) : (
              <span className="seller-link disabled">@{item.nickName}</span>
            )}
            <StatusPill status={item.status} />
          </div>
          <h1>{item.name}</h1>
          <strong className="detail-price">{item.price.toLocaleString()}원</strong>
          <p>{item.description || "등록된 설명이 없습니다."}</p>
          {isOwner ? (
            <div className="owner-actions">
              <button type="button" onClick={() => navigate(`/items/${item.itemId}/edit`)} disabled={!canMutate || loading}>수정하기</button>
              <button className="danger" type="button" onClick={handleRemove} disabled={!canMutate || loading}>삭제하기</button>
            </div>
          ) : (
            <div className="buyer-actions">
              <button type="button" onClick={handleBuyerAction}>{member ? "구매하기" : "로그인하고 구매하기"}</button>
              <button type="button" onClick={handleBuyerAction}>{member ? "구매 문의하기" : "로그인하고 문의하기"}</button>
            </div>
          )}
        </aside>
      </section>
    </main>
  );
}
