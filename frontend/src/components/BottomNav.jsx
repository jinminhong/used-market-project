import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { Home, Search, MessageCircle, Store, LogIn, LogOut } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";

const FOCUS_RING_CLASS =
  "transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2";

export default function BottomNav() {
  const { member, logout } = useSession();
  const navigate = useNavigate();
  const location = useLocation();

  const shopPath = member ? `/shop/${member.memberId}` : "/auth";
  const shopActive = Boolean(member) && location.pathname.startsWith(`/shop/${member.memberId}`);
  const chatPath = member ? "/chat" : "/auth";
  const chatActive = location.pathname.startsWith("/chat");

  async function handleAccountClick() {
    await logout();
    navigate("/");
  }

  return (
    <div className="bottom-nav-wrap">
      <nav className="bottom-nav">
        <NavLink
          to="/"
          end
          className={({ isActive }) => `bottom-nav-item${isActive ? " active" : ""} ${FOCUS_RING_CLASS}`}
        >
          <Home size={20} />
          <span>홈</span>
        </NavLink>
        <Link to="/?focus=search" className={`bottom-nav-item ${FOCUS_RING_CLASS}`}>
          <Search size={20} />
          <span>검색</span>
        </Link>
        <Link to={chatPath} className={`bottom-nav-item${chatActive ? " active" : ""} ${FOCUS_RING_CLASS}`}>
          <MessageCircle size={20} />
          <span>채팅</span>
        </Link>
        <Link to={shopPath} className={`bottom-nav-item${shopActive ? " active" : ""} ${FOCUS_RING_CLASS}`}>
          <Store size={20} />
          <span>내 상점</span>
        </Link>
        {member ? (
          <button type="button" className={`bottom-nav-item ${FOCUS_RING_CLASS}`} onClick={handleAccountClick}>
            <LogOut size={20} />
            <span>로그아웃</span>
          </button>
        ) : (
          <NavLink
            to="/auth"
            className={({ isActive }) => `bottom-nav-item${isActive ? " active" : ""} ${FOCUS_RING_CLASS}`}
          >
            <LogIn size={20} />
            <span>로그인</span>
          </NavLink>
        )}
      </nav>
    </div>
  );
}
