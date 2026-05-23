import { Outlet } from "react-router";
import { Toaster } from "react-hot-toast";
import { useEffect } from "react";
import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import useAuth from "@/auth/store";


function RootLayout() {
  const restoreSession = useAuth((state) => state.restoreSession);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  return (
    <div>
      <Toaster />
      <Navbar />
      <Outlet />
      <Footer />
    </div>
  );
  
}

export default RootLayout;
