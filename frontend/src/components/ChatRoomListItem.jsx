import { Link } from "react-router-dom";
import { defaultImage } from "../api/normalize.js";

function formatRelativeTime(iso) {
  if (!iso) return "";
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}

export default function ChatRoomListItem({ room }) {
  return (
    <Link to={`/chat/${room.roomId}`} className="chat-room-item">
      <span className="chat-room-thumb">
        <img src={room.itemImageUrl || defaultImage()} alt="" loading="lazy" />
      </span>
      <span className="chat-room-body">
        <span className="chat-room-top">
          <strong className="chat-room-item-name">{room.itemName || "상품 정보 없음"}</strong>
          <span className="chat-room-time">{formatRelativeTime(room.lastMessageAt)}</span>
        </span>
        <em className="chat-room-counterpart">@{room.counterpart?.nickName || "상대방"}</em>
        <span className="chat-room-preview">{room.lastMessage || "대화를 시작해보세요."}</span>
      </span>
    </Link>
  );
}
