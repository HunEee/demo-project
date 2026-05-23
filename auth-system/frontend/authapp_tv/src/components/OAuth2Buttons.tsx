import { Button } from "./ui/button";
import { Chrome, MessageCircle } from "lucide-react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const OAUTH2_BASE_URL = API_BASE_URL.replace(/\/api\/v\d+\/?$/, "");

function OAuth2Buttons() {
  return (
    <div className="space-y-3">
      {/* Naver */}
      <a
        href={`${OAUTH2_BASE_URL}/oauth2/authorization/naver`}
        className="block"
      >
        <Button
          type="button"
          className="w-full flex cursor-pointer items-center justify-center gap-3 rounded-2xl bg-green-500 hover:bg-green-600 text-white"
        >
          Naver로 계속하기
        </Button>
      </a>

      {/* Kakao */}
      <a
        href={`${OAUTH2_BASE_URL}/oauth2/authorization/kakao`}
        className="block"
      >
        <Button
          type="button"
          className="w-full flex cursor-pointer items-center justify-center gap-3 rounded-2xl bg-yellow-300 hover:bg-yellow-400 text-neutral-950"
        >
          <MessageCircle className="w-5 h-5" /> Kakao로 계속하기
        </Button>
      </a>

      {/* Google */}
      <a
        href={`${OAUTH2_BASE_URL}/oauth2/authorization/google`}
        className="block"
      >
        <Button
          type="button"
          variant="outline"
          className="w-full cursor-pointer flex items-center gap-3 rounded-2xl"
        >
          <Chrome className="w-5 h-5" /> Google로 계속하기
        </Button>
      </a>
    </div>
  );
}

export default OAuth2Buttons;
