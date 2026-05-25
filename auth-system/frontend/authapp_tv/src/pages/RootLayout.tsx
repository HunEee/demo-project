import { Outlet } from "react-router";
import { Toaster } from "react-hot-toast";
import { useEffect } from "react";
import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import useAuth from "@/auth/store";


function RootLayout() {
  const restoreSession = useAuth((state) => state.restoreSession);
  const authLoading = useAuth((state) => state.authLoading);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  return (
    <div>
      <Toaster />
      <Navbar />
      {/* restoreSession() 중에는 Outlet을 렌더하지 않음 -> useEffect가 access token 복원 전 먼저 API를 때리지 않음 */}
      {    
        authLoading ? (
          <main className="min-h-[60vh] flex items-center justify-center text-sm text-muted-foreground">
            세션 확인 중...
          </main>
        ) : (<Outlet />)
      }
      <Footer />
    </div>
  );
  
}

export default RootLayout;
