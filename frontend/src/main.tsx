import React from "react";
import { createRoot } from "react-dom/client";
import AdminApp from "./admin/AdminApp";
import App from "./App";
import "./styles.css";

const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("Root element not found");
}

createRoot(rootElement).render(
  <React.StrictMode>
    {window.location.pathname.startsWith("/admin") ? <AdminApp /> : <App />}
  </React.StrictMode>
);
