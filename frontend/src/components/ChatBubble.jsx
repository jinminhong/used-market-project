import { Link } from "react-router-dom";
import { Button } from "./ui/button.jsx";

const OFFER_STATUS_LABELS = {
  PENDING: "제안중",
  ACCEPTED: "수락됨",
  REJECTED: "거절됨",
};

function formatTime(iso) {
  if (!iso) return "";
  const date = new Date(iso);
  return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export default function ChatBubble({ message, isMine, isSeller, sellerId, onAcceptOffer, onRejectOffer, onGoToOrderCheckout }) {
  if (message.type === "OFFER") {
    // 원본 제안 카드는 항상 구매자가 작성하고, 수락/거절 확인 메시지는 판매자가 작성한 별도의 OFFER 메시지다.
    const isConfirmationMessage = message.senderId != null && sellerId != null && message.senderId === sellerId;

    if (isConfirmationMessage) {
      const canPay = !isSeller && message.offerStatus === "ACCEPTED";
      return (
        <div className={`chat-bubble-row ${isMine ? "mine" : "theirs"}`}>
          {!isMine && <em className="chat-bubble-sender">{message.senderNickname}</em>}
          <div className="chat-bubble chat-bubble-system">
            <p>{message.content}</p>
            {canPay && (
              message.orderId ? (
                <Button size="sm" onClick={() => onGoToOrderCheckout?.(message)}>결제하러 가기</Button>
              ) : (
                <Link className="text-button" to="/profile/purchases?tab=agreed">구매내역에서 결제하기</Link>
              )
            )}
          </div>
          <span className="chat-bubble-meta">{formatTime(message.sentAt)}</span>
        </div>
      );
    }

    const canRespond = isSeller && message.offerStatus === "PENDING";
    return (
      <div className={`chat-bubble-row ${isMine ? "mine" : "theirs"}`}>
        {!isMine && <em className="chat-bubble-sender">{message.senderNickname}</em>}
        <div className="chat-offer-card">
          <span className="chat-offer-label">가격 제안</span>
          <strong className="chat-offer-price">{(message.offeredPrice ?? 0).toLocaleString()}원</strong>
          <span className={`chat-offer-status ${(message.offerStatus ?? "PENDING").toLowerCase()}`}>
            {OFFER_STATUS_LABELS[message.offerStatus] ?? OFFER_STATUS_LABELS.PENDING}
          </span>
          {canRespond && (
            <div className="chat-offer-actions">
              <Button size="sm" onClick={() => onAcceptOffer?.(message.messageId)}>수락</Button>
              <Button size="sm" variant="outline" onClick={() => onRejectOffer?.(message.messageId)}>거절</Button>
            </div>
          )}
        </div>
        <span className="chat-bubble-meta">{formatTime(message.sentAt)}</span>
      </div>
    );
  }

  return (
    <div className={`chat-bubble-row ${isMine ? "mine" : "theirs"}`}>
      {!isMine && <em className="chat-bubble-sender">{message.senderNickname}</em>}
      <div className={`chat-bubble ${message.pending ? "pending" : ""} ${message.failed ? "failed" : ""}`}>
        <p>{message.content}</p>
      </div>
      <span className="chat-bubble-meta">
        {message.failed ? "전송 실패" : message.pending ? "전송 중..." : formatTime(message.sentAt)}
      </span>
    </div>
  );
}
