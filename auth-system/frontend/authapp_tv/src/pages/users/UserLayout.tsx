import useAuth from "@/auth/store";
import { Navigate, Outlet } from "react-router";

const UserLayout = () => {

  const checkLogin = useAuth((state) => state.checkLogin);

  if (checkLogin())
    return (
      <div>
        <Outlet />
      </div>
    );
  else return <Navigate to={"/login"} />;

}

export default UserLayout