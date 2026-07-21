import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, MessageCircle } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { getChatRooms } from "../lib/chatStorage.js";
import ChatRoomListItem from "../components/ChatRoomListItem.jsx";

export default function ChatList() {
  const { member, useMock } = useSession();
  const [rooms, setRooms] = useState([]);

  useEffect(() => {
    if (!member || useMock) {
      setRooms([]);
      return;
    }
    setRooms(getChatRooms(member.memberId));
  }, [member, useMock]);

  return (
    <main className="page-shell">
      <section className="shop-hero">
        <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
        <div className="shop-avatar"><MessageCircle size={20} /></div>
        <p>My chats</p>
        <h1>채팅</h1>
      </section>
      {useMock && <p className="quiet-message">Mock 모드에서는 채팅 기능을 사용할 수 없습니다.</p>}
      {!useMock && rooms.length === 0 && (
        <p className="quiet-message">아직 시작한 채팅이 없습니다. 상품 상세에서 "구매 문의하기"로 채팅을 시작해보세요.</p>
      )}
      {!useMock && rooms.length > 0 && (
        <section className="chat-room-list">
          {rooms.map((room) => (
            <ChatRoomListItem key={room.roomId} room={room} />
          ))}
        </section>
      )}
    </main>
  );
}
