import { useLocation, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';

export default function BackButton({ fallback = '/' }) {
  const navigate = useNavigate();
  const location = useLocation();

  const goBack = () => {
    if (location.key === 'default') {
      navigate(fallback, { replace: true });
      return;
    }
    navigate(-1);
  };

  return (
    <button type="button" onClick={goBack} className="-ml-2 mb-4 inline-flex items-center gap-1 rounded-xl px-2 py-1.5 text-sm font-medium text-primary transition-transform active:scale-95">
      <ChevronLeft className="h-5 w-5" />
      Voltar
    </button>
  );
}
