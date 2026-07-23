import { Navigate, useLocation } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";

export default function RequireAuth({ children }) {
  const { member, initializing } = useSession();
  const location = useLocation();

  if (initializing) return null;

  if (!member) {
    return <Navigate to={`/auth?next=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }

  return children;
}
