import { Link, useNavigate } from 'react-router-dom';
import { Activity, ChevronRight, Heart, LogOut, Ruler, Scale, Settings, Target, User } from 'lucide-react';
import { useAuth } from '@/lib/AuthContext';

const LEVEL_LABELS = { INICIANTE: 'Iniciante', INTERMEDIARIO: 'Intermediário', AVANCADO: 'Avançado' };
const GOAL_LABELS = {
  SAUDE_GERAL: 'Saúde',
  EMAGRECER: 'Emagrecer',
  COMPLETAR_5K: 'Completar 5 km',
  COMPLETAR_10K: 'Completar 10 km',
  COMPLETAR_MEIA_MARATONA: 'Meia maratona',
  COMPLETAR_MARATONA: 'Maratona',
  MELHORAR_5K: 'Melhorar 5 km',
  MELHORAR_10K: 'Melhorar 10 km',
};

export default function Profile() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const rows = [
    { icon: Scale, label: 'Peso', value: user?.pesoKg ? `${user.pesoKg} kg` : '-' },
    { icon: Ruler, label: 'Altura', value: user?.alturaCm ? `${user.alturaCm} cm` : '-' },
    { icon: Heart, label: 'FC repouso', value: user?.fcRepouso ? `${user.fcRepouso} bpm` : '-' },
    { icon: Activity, label: 'Nível', value: LEVEL_LABELS[user?.nivelCondicionamento] || '-' },
    { icon: Target, label: 'Objetivo', value: GOAL_LABELS[user?.objetivo] || '-' },
  ];

  const handleLogout = () => {
    logout();
    navigate('/entrar');
  };

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-center gap-4 mb-8">
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
          <User className="w-7 h-7 text-primary" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-foreground">{user?.nome || 'Meu Perfil'}</h1>
          <p className="text-xs text-muted-foreground">{user?.email}</p>
        </div>
      </div>

      <div className="space-y-3 mb-8">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">Dados do atleta</h2>
        {rows.map((row) => {
          const Icon = row.icon;
          return (
            <div key={row.label} className="flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
              <div className="w-9 h-9 rounded-xl bg-secondary flex items-center justify-center">
                <Icon className="w-4 h-4 text-muted-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-[10px] text-muted-foreground">{row.label}</p>
                <p className="text-sm font-medium text-foreground">{row.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      {user?.diasSemanaTreino && (
        <div className="mb-8">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Dias de treino</h2>
          <div className="flex flex-wrap gap-2">
            {user.diasSemanaTreino.split(',').map((day) => (
              <span key={day} className="px-3 py-1.5 rounded-xl bg-primary/10 text-primary text-xs font-semibold capitalize">{day}</span>
            ))}
          </div>
        </div>
      )}

      <div className="space-y-2">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">Configurações</h2>
        <Link to="/onboarding" className="w-full flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
          <Settings className="w-4 h-4 text-muted-foreground" />
          <span className="text-sm text-foreground flex-1 text-left">Atualizar onboarding</span>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
        </Link>
        <button onClick={handleLogout} className="w-full flex items-center gap-3 bg-card rounded-2xl p-4 border border-destructive/20">
          <LogOut className="w-4 h-4 text-destructive" />
          <span className="text-sm text-destructive flex-1 text-left">Sair</span>
        </button>
      </div>
    </div>
  );
}
