import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronLeft, Store, Package, ShoppingBag, Heart, MessageCircle } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { normalizeMemberInfo } from "../api/normalize.js";

export default function Profile() {
  const { api, setNotice } = useSession();
  const navigate = useNavigate();
  const [info, setInfo] = useState(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setInfo(null);
    setNotFound(false);

    (async () => {
      try {
        const data = await api.getMyInfo();
        if (cancelled) return;
        setInfo(normalizeMemberInfo(data));
      } catch (error) {
        if (cancelled) return;
        setNotFound(true);
        setNotice(error.message || "회원정보를 불러오지 못했습니다.");
      }
    })();

    return () => { cancelled = true; };
  }, [api]);

  if (notFound) {
    return (
      <main className="page-shell narrow-page">
        <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
        <p className="quiet-message">회원정보를 불러올 수 없습니다.</p>
      </main>
    );
  }

  if (!info) return null;

  const addressText = [info.address.city, info.address.street, info.address.zipcode].filter(Boolean).join(" ");

  return (
    <main className="page-shell narrow-page profile-page">
      <Link className="text-button" to="/"><ChevronLeft size={16} /> Back to shop</Link>
      <section className="shop-hero">
        <div className="shop-avatar">{info.nickName.slice(0, 1).toUpperCase()}</div>
        <p>My account</p>
        <h1>@{info.nickName}</h1>
      </section>
      <Link to={`/shop/${info.memberId}`} className="shop-cta">
        <Store size={20} />
        <span>내 상점으로 이동</span>
      </Link>
      <nav className="profile-quick-links">
        <Link to="/profile/sales">
          <Package size={20} />
          <span>판매내역</span>
        </Link>
        <Link to="/profile/purchases">
          <ShoppingBag size={20} />
          <span>구매내역</span>
        </Link>
        <Link to="/profile/wishlist">
          <Heart size={20} />
          <span>위시리스트</span>
        </Link>
        <Link to="/chat">
          <MessageCircle size={20} />
          <span>채팅</span>
        </Link>
      </nav>
      <section className="profile-detail-card">
        <dl>
          <div><dt>이름</dt><dd>{info.name}</dd></div>
          <div><dt>아이디</dt><dd>{info.loginId}</dd></div>
          <div><dt>닉네임</dt><dd>{info.nickName}</dd></div>
          <div><dt>주소</dt><dd>{addressText || "등록된 주소가 없습니다."}</dd></div>
        </dl>
        <div className="profile-actions">
          <button type="button" onClick={() => navigate("/profile/edit")}>정보 수정하기</button>
        </div>
      </section>
    </main>
  );
}
