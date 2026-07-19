import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft, ChevronRight, Heart } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeItem, imageUrlFromUploadFile, defaultImage } from "../api/normalize.js";
import StatusPill from "../components/StatusPill.jsx";
import { Button } from "../components/ui/button.jsx";

export default function Detail() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { api, member, run, loading, setNotice } = useSession();
  const [item, setItem] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [wished, setWished] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setItem(null);
    setNotFound(false);
    setActiveImageIndex(0);
    setWished(false);

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

  function redirectToAuth() {
    navigate(`/auth?next=${encodeURIComponent(location.pathname + location.search)}`);
  }

  function handleBuy() {
    if (!member) {
      redirectToAuth();
      return;
    }
    navigate(`/items/${item.itemId}/checkout`);
  }

  function handleInquire() {
    if (!member) {
      redirectToAuth();
      return;
    }
    setNotice("준비 중인 기능입니다.");
  }

  async function handleAddWishlist() {
    if (!member || wished) {
      if (!member) redirectToAuth();
      return;
    }
    await run(async () => {
      await api.addWishlist(item.itemId);
      setWished(true);
    });
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
                <Button
                  variant="ghost"
                  size="icon"
                  className="slide-button prev rounded-full bg-white/90 hover:bg-white"
                  onClick={() => moveSlide(-1)}
                  aria-label="이전 이미지"
                >
                  <ChevronLeft />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="slide-button next rounded-full bg-white/90 hover:bg-white"
                  onClick={() => moveSlide(1)}
                  aria-label="다음 이미지"
                >
                  <ChevronRight />
                </Button>
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
              <Button size="lg" variant="secondary" onClick={() => navigate(`/items/${item.itemId}/edit`)} disabled={!canMutate || loading}>수정하기</Button>
              <Button size="lg" variant="destructive" onClick={handleRemove} disabled={!canMutate || loading}>삭제하기</Button>
            </div>
          ) : (
            <div className="buyer-actions">
              <Button size="lg" onClick={handleBuy}>{member ? "구매하기" : "로그인하고 구매하기"}</Button>
              <Button size="lg" variant="outline" onClick={handleInquire}>{member ? "구매 문의하기" : "로그인하고 문의하기"}</Button>
              <Button
                size="lg"
                variant={wished ? "secondary" : "ghost"}
                onClick={handleAddWishlist}
                disabled={loading || wished}
                aria-pressed={wished}
                aria-label={wished ? "찜한 상품" : "찜하기"}
                className={wished ? "text-red-600" : undefined}
              >
                <Heart size={18} fill={wished ? "currentColor" : "none"} />
                <span>{wished ? "찜 완료" : "찜하기"}</span>
              </Button>
            </div>
          )}
        </aside>
      </section>
    </main>
  );
}
