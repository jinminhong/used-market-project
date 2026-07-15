import { Route, Routes } from "react-router-dom";
import Header from "./components/Header.jsx";
import BottomNav from "./components/BottomNav.jsx";
import RequireAuth from "./components/RequireAuth.jsx";
import NotFound from "./components/NotFound.jsx";
import Home from "./pages/Home.jsx";
import Detail from "./pages/Detail.jsx";
import Shop from "./pages/Shop.jsx";
import Auth from "./pages/Auth.jsx";
import Editor from "./pages/Editor.jsx";
import Profile from "./pages/Profile.jsx";
import ProfileEdit from "./pages/ProfileEdit.jsx";
import { useSession } from "./context/SessionContext.jsx";

export default function App() {
  const { notice } = useSession();

  return (
    <div className="app-shell">
      <Header />
      {notice && <div className="toast">{notice}</div>}
      <div className="app-main">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/items/new" element={<RequireAuth><Editor /></RequireAuth>} />
          <Route path="/items/:itemId/edit" element={<RequireAuth><Editor /></RequireAuth>} />
          <Route path="/items/:itemId" element={<Detail />} />
          <Route path="/shop/:memberId" element={<Shop />} />
          <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
          <Route path="/profile/edit" element={<RequireAuth><ProfileEdit /></RequireAuth>} />
          <Route path="/auth" element={<Auth />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
      <BottomNav />
    </div>
  );
}
