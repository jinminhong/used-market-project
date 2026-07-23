import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ChevronLeft, Tag } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { useChatSocket } from "../context/ChatSocketContext.jsx";
import { normalizeChatMessage, normalizeChatRoom } from "../api/normalize.js";
import ChatBubble from "../components/ChatBubble.jsx";
import { Button } from "../components/ui/button.jsx";
import { Input } from "../components/ui/input.jsx";

export default function ChatRoom() {
  const { roomId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { api, member } = useSession();
  const { connected, connectionError, subscribeRoom, sendMessage, acceptOffer, rejectOffer } = useChatSocket();
  const [roomMeta, setRoomMeta] = useState(null);
  const [isSeller, setIsSeller] = useState(false);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [showOfferForm, setShowOfferForm] = useState(false);
  const [offerPrice, setOfferPrice] = useState("");
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const bottomRef = useRef(null);
  const autoOfferSentRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setRoomMeta(null);
    setIsSeller(false);
    if (!member?.memberId) return undefined;
    (async () => {
      try {
        const response = await api.listChatRooms();
        const rooms = (response?.chatRooms ?? []).map(normalizeChatRoom);
        const room = rooms.find((r) => String(r.roomId) === String(roomId)) ?? null;
        if (cancelled) return;
        setRoomMeta(room);
        setIsSeller(room?.sellerId === member.memberId);
      } catch {
        if (!cancelled) {
          setRoomMeta(null);
          setIsSeller(false);
        }
      }
    })();
    return () => { cancelled = true; };
  }, [api, member?.memberId, roomId]);

  useEffect(() => {
    let cancelled = false;
    setMessages([]);
    setHistoryLoaded(false);
    (async () => {
      try {
        const response = await api.getChatMessages(roomId);
        const history = (response?.list ?? []).map(normalizeChatMessage).reverse();
        if (!cancelled) setMessages(history);
      } catch {
        if (!cancelled) setMessages([]);
      } finally {
        if (!cancelled) setHistoryLoaded(true);
      }
    })();
    return () => { cancelled = true; };
  }, [api, roomId]);

  useEffect(() => {
    function handleIncoming(raw) {
      const incoming = normalizeChatMessage(raw);
      setMessages((current) => {
        const pendingIndex = current.findIndex((message) => {
          if (!message.pending || message.senderId !== incoming.senderId) return false;
          if (incoming.type === "OFFER") return message.type === "OFFER" && message.offeredPrice === incoming.offeredPrice;
          return message.content === incoming.content;
        });
        if (pendingIndex !== -1) {
          const next = [...current];
          next[pendingIndex] = incoming;
          return next;
        }
        return [...current, incoming];
      });
    }

    const unsubscribe = subscribeRoom(roomId, handleIncoming);
    return unsubscribe;
  }, [roomId, subscribeRoom, member?.memberId, connected]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function handleSend(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content) return;

    const tempId = `pending-${Date.now()}`;
    setMessages((current) => [
      ...current,
      {
        messageId: tempId,
        roomId,
        senderId: member.memberId,
        senderNickname: member.nickName,
        content,
        sentAt: new Date().toISOString(),
        pending: true,
      },
    ]);
    setDraft("");

    try {
      await sendMessage(roomId, content);
    } catch {
      setMessages((current) =>
        current.map((message) => (message.messageId === tempId ? { ...message, pending: false, failed: true } : message))
      );
    }
  }

  async function submitOffer(price) {
    if (!roomMeta?.itemId) return;
    const content = `${price.toLocaleString()}원에 제안합니다`;
    const tempId = `pending-offer-${Date.now()}`;
    setMessages((current) => [
      ...current,
      {
        messageId: tempId,
        roomId,
        senderId: member.memberId,
        senderNickname: member.nickName,
        content,
        sentAt: new Date().toISOString(),
        type: "OFFER",
        offeredPrice: price,
        offerStatus: "PENDING",
        orderId: null,
        pending: true,
      },
    ]);

    try {
      // 서버가 메시지를 저장하고 /topic/chat/rooms/{roomId} 브로드캐스트로 최종 상태를 내려준다 —
      // 위의 낙관적 항목은 handleIncoming의 pending 매칭 로직으로 교체된다.
      await api.createOffer(roomMeta.itemId, content, price);
    } catch {
      setMessages((current) =>
        current.map((message) => (message.messageId === tempId ? { ...message, pending: false, failed: true } : message))
      );
    }
  }

  async function handleSendOffer(event) {
    event.preventDefault();
    const price = Number(offerPrice);
    if (!price || price <= 0) return;
    setOfferPrice("");
    setShowOfferForm(false);
    await submitOffer(price);
  }

  // Detail.jsx의 "가격 제안하기" 다이얼로그에서 넘어온 경우: 채팅방 진입과 동시에 해당 가격으로 제안을 자동 전송한다.
  useEffect(() => {
    const pendingPrice = location.state?.pendingOfferPrice;
    if (!pendingPrice || autoOfferSentRef.current || !connected || !member || !historyLoaded || !roomMeta?.itemId) return;
    autoOfferSentRef.current = true;
    navigate(location.pathname, { replace: true, state: {} });
    submitOffer(pendingPrice);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected, member, historyLoaded, location.state, roomId, roomMeta]);

  // 서버(백엔드 미구현)의 승인 응답을 아직 받을 수 없으므로 우선 낙관적으로 로컬 상태만 갱신한다.
  // 백엔드 구현 후에는 /topic/chat/rooms/{roomId} 브로드캐스트로 최종 상태가 다시 내려온다.
  async function handleAcceptOffer(messageId) {
    setMessages((current) =>
      current.map((message) => (message.messageId === messageId ? { ...message, offerStatus: "ACCEPTED" } : message))
    );
    try {
      await acceptOffer(roomId, messageId);
    } catch {
      // 소켓 미연결 등으로 발행 자체가 실패해도 로컬 표시는 유지(백엔드 없이는 재확인 불가)
    }
  }

  async function handleRejectOffer(messageId) {
    setMessages((current) =>
      current.map((message) => (message.messageId === messageId ? { ...message, offerStatus: "REJECTED" } : message))
    );
    try {
      await rejectOffer(roomId, messageId);
    } catch {
      // 소켓 미연결 등으로 발행 자체가 실패해도 로컬 표시는 유지(백엔드 없이는 재확인 불가)
    }
  }

  return (
    <main className="chat-room-page">
      <section className="chat-room-header">
        <Link className="text-button" to="/chat"><ChevronLeft size={16} /> 채팅 목록</Link>
        <div className="chat-room-header-info">
          <strong>{roomMeta?.itemName || "채팅방"}</strong>
          <span>@{roomMeta?.counterpart?.nickName || "상대방"}</span>
        </div>
      </section>
      {connectionError && <p className="chat-room-error">{connectionError}</p>}
      <section className="chat-message-list">
        {messages.map((message) => (
          <ChatBubble
            key={message.messageId}
            message={message}
            isMine={message.senderId === member?.memberId}
            isSeller={isSeller}
            onAcceptOffer={handleAcceptOffer}
            onRejectOffer={handleRejectOffer}
          />
        ))}
        <div ref={bottomRef} />
      </section>
      <div className="chat-composer">
        {showOfferForm && !isSeller && (
          <form className="chat-offer-form" onSubmit={handleSendOffer}>
            <Input
              type="number"
              min="0"
              value={offerPrice}
              onChange={(event) => setOfferPrice(event.target.value)}
              placeholder="제안 가격을 입력하세요"
            />
            <Button type="submit" disabled={!connected || !offerPrice}>제안 전송</Button>
            <Button type="button" variant="ghost" onClick={() => setShowOfferForm(false)}>취소</Button>
          </form>
        )}
        <form className="chat-input-row" onSubmit={handleSend}>
          {!isSeller && (
            <Button type="button" variant="outline" size="icon" onClick={() => setShowOfferForm((current) => !current)} aria-label="가격 제안하기">
              <Tag size={16} />
            </Button>
          )}
          <Input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="메시지를 입력하세요"
            maxLength={1000}
          />
          <Button type="submit" disabled={!connected || !draft.trim()}>전송</Button>
        </form>
      </div>
    </main>
  );
}
