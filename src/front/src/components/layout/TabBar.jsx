import { Link, useLocation } from 'react-router-dom';
import { Home, Calendar, TrendingUp, User, Sparkles } from 'lucide-react';

const tabs = [
  { path: '/', icon: Home, label: 'Início' },
  { path: '/plano', icon: Calendar, label: 'Plano' },
  { path: '/ia', icon: Sparkles, label: 'IA Coach' },
  { path: '/progresso', icon: TrendingUp, label: 'Progresso' },
  { path: '/perfil', icon: User, label: 'Perfil' },
];

export default function TabBar() {
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 safe-bottom">
      <div className="mx-auto max-w-lg">
        <div className="mx-3 mb-2 rounded-2xl bg-card/90 backdrop-blur-xl border border-border/50 px-2 py-1.5">
          <div className="flex items-center justify-around">
            {tabs.map((tab) => {
              const isActive = location.pathname === tab.path;
              const Icon = tab.icon;
              return (
                <Link
                  key={tab.path}
                  to={tab.path}
                  className={`flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-xl transition-all duration-200 ${
                    isActive
                      ? 'text-primary'
                      : 'text-muted-foreground'
                  }`}
                >
                  <div className={`p-1.5 rounded-xl transition-all duration-200 ${
                    isActive ? 'bg-primary/15' : ''
                  }`}>
                    <Icon className="w-5 h-5" strokeWidth={isActive ? 2.5 : 1.8} />
                  </div>
                  <span className="text-[10px] font-medium">{tab.label}</span>
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}