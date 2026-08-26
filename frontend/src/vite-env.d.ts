/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly DEV: boolean;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_PORTONE_STORE_ID?: string;
  readonly VITE_PORTONE_CHANNEL_KEY?: string;
  readonly VITE_PORTONE_REDIRECT_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
