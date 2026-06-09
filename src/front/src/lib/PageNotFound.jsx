import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';

export default function PageNotFound() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-6 max-w-lg mx-auto">
      <div className="text-center">
        <div className="w-20 h-20 rounded-3xl bg-card border border-border flex items-center justify-center mx-auto mb-6">
          <span className="text-4xl">🏃</span>
        </div>
        <h1 className="text-2xl font-bold text-foreground mb-2">Página não encontrada</h1>
        <p className="text-sm text-muted-foreground mb-8">Parece que você saiu da rota!</p>
        <Link
          to="/"
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground px-6 py-3 rounded-2xl font-semibold text-sm"
        >
          <Home className="w-4 h-4" />
          Voltar ao Início
        </Link>
      </div>
    </div>
  );
}