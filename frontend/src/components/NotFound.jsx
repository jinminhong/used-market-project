import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <main className="page-shell narrow-page">
      <p className="quiet-message">페이지를 찾을 수 없습니다.</p>
      <Link className="text-button" to="/">홈으로</Link>
    </main>
  );
}
