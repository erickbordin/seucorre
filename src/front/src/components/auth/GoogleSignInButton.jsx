import { useEffect, useRef } from 'react';

const SCRIPT_URL = 'https://accounts.google.com/gsi/client';
const MAX_BUTTON_WIDTH = 400;

export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

let scriptPromise = null;

const loadGoogleScript = () => {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = SCRIPT_URL;
      script.async = true;
      script.onload = resolve;
      script.onerror = () => {
        scriptPromise = null;
        reject(new Error('Não foi possível carregar o login do Google. Verifique a conexão.'));
      };
      document.head.appendChild(script);
    });
  }
  return scriptPromise;
};

export default function GoogleSignInButton({ text, onCredential, onError }) {
  const containerRef = useRef(null);
  const handlersRef = useRef({ onCredential, onError });
  handlersRef.current = { onCredential, onError };

  useEffect(() => {
    let cancelled = false;
    loadGoogleScript()
      .then(() => {
        const container = containerRef.current;
        if (cancelled || !container) return;
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: (response) => handlersRef.current.onCredential(response.credential),
        });
        window.google.accounts.id.renderButton(container, {
          theme: 'filled_black',
          size: 'large',
          shape: 'pill',
          text,
          locale: 'pt-BR',
          width: Math.min(container.offsetWidth || MAX_BUTTON_WIDTH, MAX_BUTTON_WIDTH),
        });
      })
      .catch((error) => {
        if (!cancelled) handlersRef.current.onError?.(error.message);
      });
    return () => {
      cancelled = true;
    };
  }, [text]);

  return <div ref={containerRef} className="flex min-h-11 w-full justify-center" />;
}
