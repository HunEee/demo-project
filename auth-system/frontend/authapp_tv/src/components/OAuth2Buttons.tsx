import { Button } from "./ui/button";
import { Chrome, Github } from "lucide-react";
import { NavLink } from "react-router";

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

function OAuth2Buttons() {
  return (
    <div className="space-y-3">
      {/* Google */}
      <NavLink
        to={`${BASE_URL}/oauth2/authorization/google`}
        className="block"
      >
        <Button
          type="button"
          variant="outline"
          className="w-full cursor-pointer flex items-center gap-3 rounded-2xl"
        >
          <Chrome className="w-5 h-5" /> Google로 계속하기
        </Button>
      </NavLink>

      {/* GitHub */}
      <NavLink
        to={`${BASE_URL}/oauth2/authorization/github`}
        className="block"
      >
        <Button
          type="button"
          variant="outline"
          className="w-full flex cursor-pointer items-center gap-3 rounded-2xl"
        >
          <Github className="w-5 h-5" /> GitHub로 계속하기
        </Button>
      </NavLink>

      {/* Naver */}
      <NavLink
        to={`${BASE_URL}/oauth2/authorization/naver`}
        className="block"
      >
        <Button
          type="button"
          className="w-full flex cursor-pointer items-center justify-center gap-3 rounded-2xl bg-green-500 hover:bg-green-600 text-white"
        >
          Naver로 계속하기
        </Button>
      </NavLink>
    </div>
  );
}

export default OAuth2Buttons;