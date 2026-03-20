import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from "react-router";
import './index.css';
import RootLayout from "./pages/RootLayout.tsx";
import App from './App.tsx';
import Login from "./pages/Login.tsx";
import Signup from "./pages/Signup.tsx";
import Services from "./pages/Services.tsx";
import About from "./pages/About.tsx";



createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<RootLayout />}>
        <Route index element={<App />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/services" element={<Services />} />
        <Route path="/about" element={<About />} />

      </Route>
    </Routes>
  </BrowserRouter>
)
