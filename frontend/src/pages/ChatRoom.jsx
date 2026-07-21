import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ChevronLeft, Tag } from "lucide-react";
import { useSession } from "../context/SessionContext.jsx";
import { useChatSocket } from "../context/ChatSocketContext.jsx";
import { getChatRoom, updateLastMessage } from "../lib/chatStorage.js";
import { normalizeChatMessage, normalizeItem } from "../api/normalize.js";
import ChatBubble from "../components/ChatBubble.jsx";
import { Button } from "../components/ui/button.jsx";
import { Input } from "../components/ui/input.jsx";

export default function ChatRoom() {
  const { roomId } = useParams();
  const { api, member } = useSession();
  const { connected, connectionError, subscribeRoom, sendMessage, sendOffer, acceptOffer, rejectOffer } = useChatSocket();
  const [roomMeta, setRoomMeta] = useState(null);
  const [isSeller, setIsSeller] = useState(false);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState("");
  const [showOfferForm, setShowOfferForm] = useState(false);
  const [offerPrice, setOfferPrice] = useState("");
  const bottomRef = useRef(null);

  useEffect(() => {
    setRoomMeta(getChatRoom(member?.memberId, roomId));
    setMessages([]);
  }, [member?.memberId, roomId]);

  // 로컬에 저장된 채팅방 요약(itemId)으로 상품을 조회해 내가 판매자인지 구매자인지 판별
  // (백엔드에 채팅방 상세 조회 API가 아직 없어 roomMeta가 없는 경우 판매자 판별은 되지 않음 — docs/api/chat.md 참고)
  useEffect(() => {
    let cancelled = false;
    if (!roomMeta?.itemId || !member?.memberId) {
      setIsSeller(false);
      return undefined;
    }
    (async () => {
      try {
        const detail = await api.findItem(roomMeta.itemId);
        const item = normalizeItem(detail, roomMeta.itemId);
        if (!cancelled) setIsSeller(item.memberId === member.memberId);
      } catch {
        if (!cancelled) setIsSeller(false);
      }
    })();
    return () => { cancelled = true; };
  }, [api, roomMeta?.itemId, member?.memberId]);

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
      if (member?.memberId) {
        updateLastMessage(member.memberId, roomId, { content: incoming.content, sentAt: incoming.sentAt });
      }
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

  async function handleSendOffer(event) {
    event.preventDefault();
    const price = Number(offerPrice);
    if (!price || price <= 0) return;

    const tempId = `pending-offer-${Date.now()}`;
    setMessages((current) => [
      ...current,
      {
        messageId: tempId,
        roomId,
        senderId: member.memberId,
        senderNickname: member.nickName,
        content: `${price.toLocaleString()}원에 제안합니다`,
        sentAt: new Date().toISOString(),
        type: "OFFER",
        offeredPrice: price,
        offerStatus: "PENDING",
        orderId: null,
        pending: true,
      },
    ]);
    setOfferPrice("");
    setShowOfferForm(false);

    try {
      await sendOffer(roomId, price);
    } catch {
      setMessages((current) =>
        current.map((message) => (message.messageId === tempId ? { ...message, pending: false, failed: true } : message))
      );
    }
  }

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
      <p className="chat-room-notice">이전 대화 내용은 새로고침하면 사라집니다.</p>
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
    </main>
  );
}
