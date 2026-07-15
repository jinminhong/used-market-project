import { Navigate, useLocation } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";

export default function RequireAuth({ children }) {
  const { member } = useSession();
  const location = useLocation();

  if (!member) {
    return <Navigate to={`/auth?next=${encodeURIComponent(location.pathname)}`} replace />;
  }

  return children;
}
