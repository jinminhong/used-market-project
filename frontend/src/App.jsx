import { Route, Routes } from "react-router-dom";
import Header from "./components/Header.jsx";
import BottomNav from "./components/BottomNav.jsx";
import RequireAuth from "./components/RequireAuth.jsx";
import NotFound from "./components/NotFound.jsx";
import Home from "./pages/Home.jsx";
import Detail from "./pages/Detail.jsx";
import Checkout from "./pages/Checkout.jsx";
import Shop from "./pages/Shop.jsx";
import Auth from "./pages/Auth.jsx";
import Editor from "./pages/Editor.jsx";
import Profile from "./pages/Profile.jsx";
import ProfileEdit from "./pages/ProfileEdit.jsx";
import Wishlist from "./pages/Wishlist.jsx";
import PurchaseHistory from "./pages/PurchaseHistory.jsx";
import SalesHistory from "./pages/SalesHistory.jsx";
import ShippingManagement from "./pages/ShippingManagement.jsx";
import ShippingForm from "./pages/ShippingForm.jsx";
import ChatList from "./pages/ChatList.jsx";
import ChatRoom from "./pages/ChatRoom.jsx";
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
          <Route path="/items/:itemId/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
          <Route path="/shop/:memberId" element={<Shop />} />
          <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
          <Route path="/profile/edit" element={<RequireAuth><ProfileEdit /></RequireAuth>} />
          <Route path="/profile/wishlist" element={<RequireAuth><Wishlist /></RequireAuth>} />
          <Route path="/profile/purchases" element={<RequireAuth><PurchaseHistory /></RequireAuth>} />
          <Route path="/profile/sales" element={<RequireAuth><SalesHistory /></RequireAuth>} />
          <Route path="/profile/sales/shipping" element={<RequireAuth><ShippingManagement /></RequireAuth>} />
          <Route path="/profile/sales/:orderId/shipping" element={<RequireAuth><ShippingForm /></RequireAuth>} />
          <Route path="/chat" element={<RequireAuth><ChatList /></RequireAuth>} />
          <Route path="/chat/:roomId" element={<RequireAuth><ChatRoom /></RequireAuth>} />
          <Route path="/auth" element={<Auth />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </div>
      <BottomNav />
    </div>
  );
}
