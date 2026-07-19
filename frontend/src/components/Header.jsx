import { Link } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { Switch } from "./ui/switch.jsx";
import { Label } from "./ui/label.jsx";
import { Avatar, AvatarFallback } from "./ui/avatar.jsx";
import { Button } from "./ui/button.jsx";

export default function Header() {
  const { member, useMock, setUseMock } = useSession();

  return (
    <header className="site-header">
      <Link to="/" className="brand">Fruits Market</Link>
      <div className="nav-actions">
        {import.meta.env.DEV && (
          <div className="flex items-center gap-2">
            <Switch id="mock-toggle" checked={useMock} onCheckedChange={setUseMock} />
            <Label htmlFor="mock-toggle">Mock</Label>
          </div>
        )}
        {member ? (
          <Link to="/profile" aria-label={`${member.nickName}의 내 정보`}>
            <Avatar>
              <AvatarFallback>{member.nickName.slice(0, 1).toUpperCase()}</AvatarFallback>
            </Avatar>
          </Link>
        ) : (
          <Button asChild variant="outline" size="sm">
            <Link to="/auth">로그인</Link>
          </Button>
        )}
      </div>
    </header>
  );
}
