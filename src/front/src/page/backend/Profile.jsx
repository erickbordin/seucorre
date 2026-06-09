import { Link, useNavigate } from 'react-router-dom';
import { Activity, Bell, ChevronRight, Heart, Link2, LogOut, Ruler, Scale, Settings, Shield, Target, User } from 'lucide-react';
import { useAuth } from '@/lib/AuthContext';
import { GOAL_LABELS, LEVEL_LABELS, parseTrainingDays } from '@/lib/user';

const DAY_LABELS = {
  seg: 'Seg',
  ter: 'Ter',
  qua: 'Qua',
  qui: 'Qui',
  sex: 'Sex',
  sab: 'Sáb',
  dom: 'Dom',
};

const HEALTH_LABELS = {
  DOR_RECORRENTE: 'Dor recorrente',
  ASMA: 'Asma',
  HIPERTENSAO: 'Hipertensão',
  LESAO_ANTERIOR: 'Lesão anterior',
};

const PLATFORM_LABELS = {
  GARMIN: 'Garmin',
  POLAR: 'Polar',
  APPLE_WATCH: 'Apple Watch',
  STRAVA: 'Strava',
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

      <div className="rounded-2xl bg-card border border-border/50 p-4 mb-8">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-xs text-muted-foreground mb-1">Status para treinar</p>
            <p className="text-sm font-semibold text-foreground">{user?.aptoParaTreinar ? 'Apto para treinar' : 'Revisar condições antes de treinar'}</p>
          </div>
          <div className={`px-3 py-1 rounded-full text-[10px] font-semibold ${user?.aptoParaTreinar ? 'bg-primary/10 text-primary' : 'bg-destructive/10 text-destructive'}`}>
            {user?.aptoParaTreinar ? 'OK' : 'Atenção'}
          </div>
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
            {parseTrainingDays(user.diasSemanaTreino).map((day) => (
              <span key={day} className="px-3 py-1.5 rounded-xl bg-primary/10 text-primary text-xs font-semibold">
                {DAY_LABELS[day] || day}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="mb-8">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Saúde e contexto</h2>
        {(user?.condicoesSaude || []).length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {user.condicoesSaude.filter((item) => item.ativa !== false).map((item) => (
              <span key={`${item.tipo}-${item.descricao || ''}`} className="px-3 py-2 rounded-xl bg-card border border-border text-xs font-medium text-foreground">
                {HEALTH_LABELS[item.tipo] || item.tipo}
              </span>
            ))}
          </div>
        ) : (
          <div className="bg-card rounded-2xl border border-border/50 p-4">
            <p className="text-sm font-medium text-foreground mb-1">Nenhuma condição registrada</p>
            <p className="text-xs text-muted-foreground">Você pode completar isso no perfil para orientar melhor o plano e os alertas.</p>
          </div>
        )}
      </div>

      <div className="mb-8">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Integrações</h2>
        <div className="space-y-3">
          <div className="flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
            <div className="w-9 h-9 rounded-xl bg-secondary flex items-center justify-center">
              <Link2 className="w-4 h-4 text-muted-foreground" />
            </div>
            <div className="flex-1">
              <p className="text-[10px] text-muted-foreground">Wearables conectados</p>
              <p className="text-sm font-medium text-foreground">
                {(user?.dispositivos || []).length > 0
                  ? user.dispositivos.map((item) => PLATFORM_LABELS[item.plataforma] || item.plataforma).join(', ')
                  : 'Nenhum conectado'}
              </p>
            </div>
          </div>
          {user?.perfilCorrida && (
            <div className="flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
              <div className="w-9 h-9 rounded-xl bg-secondary flex items-center justify-center">
                <Shield className="w-4 h-4 text-muted-foreground" />
              </div>
              <div className="flex-1">
                <p className="text-[10px] text-muted-foreground">VO₂ estimado e zonas</p>
                <p className="text-sm font-medium text-foreground">
                  VO₂ {user.perfilCorrida.vo2Estimado || '-'} • {(user.perfilCorrida.zonasFc || []).length} zonas disponíveis
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">Configurações</h2>
        <Link to="/onboarding" className="w-full flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
          <Settings className="w-4 h-4 text-muted-foreground" />
          <span className="text-sm text-foreground flex-1 text-left">Atualizar dados do perfil</span>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
        </Link>
        <Link to="/wearables" className="w-full flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
          <Link2 className="w-4 h-4 text-muted-foreground" />
          <span className="text-sm text-foreground flex-1 text-left">Gerenciar wearables</span>
          <ChevronRight className="w-4 h-4 text-muted-foreground" />
        </Link>
        <Link to="/notificacoes" className="w-full flex items-center gap-3 bg-card rounded-2xl p-4 border border-border/50">
          <Bell className="w-4 h-4 text-muted-foreground" />
          <span className="text-sm text-foreground flex-1 text-left">Notificações</span>
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
