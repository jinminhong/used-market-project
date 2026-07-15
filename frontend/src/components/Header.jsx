import { Link } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";

export default function Header() {
  const { member, useMock, setUseMock } = useSession();

  return (
    <header className="site-header">
      <Link to="/" className="brand">Fruits Market</Link>
      <div className="nav-actions">
        {import.meta.env.DEV && (
          <label className="mock-toggle">
            <input type="checkbox" checked={useMock} onChange={(event) => setUseMock(event.target.checked)} /> Mock
          </label>
        )}
        {member ? (
          <Link to="/profile" className="avatar-chip" aria-label={`${member.nickName}의 내 정보`}>
            {member.nickName.slice(0, 1).toUpperCase()}
          </Link>
        ) : (
          <Link to="/auth" className="auth-pill">로그인</Link>
        )}
      </div>
    </header>
  );
}
