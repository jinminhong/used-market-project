import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, MessageCircle } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { useChatSocket } from "../context/ChatSocketContext.jsx";
import { normalizeChatMessage, normalizeChatRoom } from "../api/normalize.js";
import ChatRoomListItem from "../components/ChatRoomListItem.jsx";

export default function ChatList() {
  const { api, member, useMock } = useSession();
  const { subscribeRoom } = useChatSocket();
  const [rooms, setRooms] = useState([]);
  const [unreadCounts, setUnreadCounts] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!member || useMock) {
      setRooms([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    (async () => {
      try {
        const response = await api.listChatRooms();
        const list = (response?.chatRooms ?? []).map(normalizeChatRoom);
        if (!cancelled) {
          setRooms(list);
          setUnreadCounts({});
        }
      } catch {
        if (!cancelled) setRooms([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [api, member, useMock]);

  const roomIds = rooms.map((room) => room.roomId).join(",");

  useEffect(() => {
    if (!member || useMock || !roomIds) return undefined;

    const unsubscribers = roomIds.split(",").map((roomId) =>
      subscribeRoom(roomId, (raw) => {
        const incoming = normalizeChatMessage(raw);
        if (incoming.senderId === member.memberId) return;

        setRooms((current) =>
          current.map((room) =>
            String(room.roomId) === String(incoming.roomId)
              ? { ...room, lastMessage: incoming.content, lastMessageAt: incoming.sentAt }
              : room
          )
        );
        setUnreadCounts((current) => ({
          ...current,
          [incoming.roomId]: (current[incoming.roomId] ?? 0) + 1,
        }));
      })
    );

    return () => unsubscribers.forEach((unsubscribe) => unsubscribe());
  }, [roomIds, member, useMock, subscribeRoom]);

  return (
    <main className="page-shell">
      <section className="shop-hero">
        <Link className="text-button" to="/profile"><ChevronLeft size={16} /> Back to profile</Link>
        <div className="shop-avatar"><MessageCircle size={20} /></div>
        <p>My chats</p>
        <h1>채팅</h1>
      </section>
      {useMock && <p className="quiet-message">Mock 모드에서는 채팅 기능을 사용할 수 없습니다.</p>}
      {!useMock && loading && <p className="quiet-message">불러오는 중...</p>}
      {!useMock && !loading && rooms.length === 0 && (
        <p className="quiet-message">아직 시작한 채팅이 없습니다. 상품 상세에서 "구매 문의하기"로 채팅을 시작해보세요.</p>
      )}
      {!useMock && !loading && rooms.length > 0 && (
        <section className="chat-room-list">
          {rooms.map((room) => (
            <ChatRoomListItem key={room.roomId} room={room} unreadCount={unreadCounts[room.roomId] ?? 0} />
          ))}
        </section>
      )}
    </main>
  );
}
