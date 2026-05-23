import useAuth from "@/auth/store";
import { Navigate, Outlet } from "react-router";

const UserLayout = () => {

  const checkLogin = useAuth((state) => state.checkLogin);
  const authLoading = useAuth((state) => state.authLoading);

  if (authLoading) return null;

  if (checkLogin())
    return (
      <div>
        <Outlet />
      </div>
    );
  else return <Navigate to={"/login"} />;

}

export default UserLayout
