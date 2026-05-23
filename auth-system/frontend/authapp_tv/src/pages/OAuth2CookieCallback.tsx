import { useEffect } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import useAuth from "@/auth/store";
import { exchangeOAuthCookie } from "@/services/AuthService";

function OAuth2CookieCallback() {
  const navigate = useNavigate();
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);

  useEffect(() => {
    let cancelled = false;

    const completeOAuthLogin = async () => {
      try {
        const { accessToken, user } = await exchangeOAuthCookie();
        if (!accessToken || !user) {
          throw new Error("OAuth2 login response was incomplete.");
        }

        if (cancelled) {
          return;
        }

        changeLocalLoginData(accessToken, user, true);
        navigate("/dashboard", { replace: true });
      } catch (error) {
        console.error("OAuth2 login callback failed:", error);
        toast.error("소셜 로그인 처리에 실패했습니다. 다시 시도해주세요.");
        navigate("/login", { replace: true });
      }
    };

    completeOAuthLogin();

    return () => {
      cancelled = true;
    };
  }, [changeLocalLoginData, navigate]);

  return (
    <div className="min-h-[60vh] flex items-center justify-center text-sm text-muted-foreground">
      소셜 로그인 처리 중...
    </div>
  );
}

export default OAuth2CookieCallback;
