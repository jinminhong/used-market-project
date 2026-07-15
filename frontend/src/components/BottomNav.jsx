import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { Home, Search, Store, LogIn, LogOut, Plus } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";

export default function BottomNav() {
  const { member, logout, setNotice } = useSession();
  const navigate = useNavigate();
  const location = useLocation();

  const shopPath = member ? `/shop/${member.memberId}` : "/auth";
  const shopActive = Boolean(member) && location.pathname.startsWith(`/shop/${member.memberId}`);

  function handleAccountClick() {
    logout();
    navigate("/");
  }

  function handleSell() {
    if (!member) {
      setNotice("상품을 등록하려면 로그인해주세요.");
      navigate("/auth");
      return;
    }
    navigate("/items/new");
  }

  return (
    <div className="bottom-nav-wrap">
      <nav className="bottom-nav">
        <NavLink to="/" end className={({ isActive }) => `bottom-nav-item${isActive ? " active" : ""}`}>
          <Home size={20} />
          <span>홈</span>
        </NavLink>
        <Link to="/?focus=search" className="bottom-nav-item">
          <Search size={20} />
          <span>검색</span>
        </Link>
        <Link to={shopPath} className={`bottom-nav-item${shopActive ? " active" : ""}`}>
          <Store size={20} />
          <span>내 상점</span>
        </Link>
        {member ? (
          <button type="button" className="bottom-nav-item" onClick={handleAccountClick}>
            <LogOut size={20} />
            <span>로그아웃</span>
          </button>
        ) : (
          <NavLink to="/auth" className={({ isActive }) => `bottom-nav-item${isActive ? " active" : ""}`}>
            <LogIn size={20} />
            <span>로그인</span>
          </NavLink>
        )}
      </nav>
      <button type="button" className="bottom-nav-fab" onClick={handleSell} aria-label="상품 등록">
        <Plus size={24} />
      </button>
    </div>
  );
}
