import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Bell, ChevronRight, Plus } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import { latestCheckin } from '@/lib/checkin';
import { currentWeekDates, localIsoDate } from '@/lib/date';
import CheckinInsightCard from '@/components/home/CheckinInsightCard';
import TodayWorkoutCard from '@/components/home/TodayWorkoutCard';
import WeekStrip from '@/components/home/WeekStrip';

export default function Home() {
  const { hasCompletedOnboarding, user } = useAuth();
  const [selectedDay, setSelectedDay] = useState(new Date().getDay());

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ['meus-planos'],
    queryFn: api.planos.listarMeus,
  });
  const { data: checkins = [] } = useQuery({
    queryKey: ['checkins', 'historico'],
    queryFn: api.checkins.historico,
  });

  const plan = plans.find((item) => item.status === 'ativo') || plans[0];
  const sessions = plan?.sessoes || [];
  const currentWeek = plan?.semana_atual || 1;
  const currentWeekSessions = sessions.filter((session) => session.semana === currentWeek);
  const selectedDate = localIsoDate(currentWeekDates()[selectedDay]);
  const selectedSession = sessions.find((session) => session.dataPrevista === selectedDate);
  const nextSession = sessions.find((session) => session.status === 'pendente' && session.dataPrevista >= localIsoDate());
  const highlightedSession = selectedSession || nextSession || null;
  const highlightedSessionIndex = sessions.findIndex((session) => session.id === highlightedSession?.id);
  const completedThisWeek = currentWeekSessions.filter((session) => session.status === 'concluido').length;
  const weekProgress = currentWeekSessions.length ? completedThisWeek / currentWeekSessions.length : 0;

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    const firstName = (user?.nome || '').split(' ')[0] || 'atleta';
    if (hour < 12) return `Bom dia, ${firstName}`;
    if (hour < 18) return `Boa tarde, ${firstName}`;
    return `Boa noite, ${firstName}`;
  }, [user?.nome]);

  if (!hasCompletedOnboarding) {
    return null;
  }

  return (
    <div className="px-5 pt-14 pb-28">
      <header className="flex items-center justify-between mb-6">
        <div>
          <p className="text-sm text-muted-foreground">{greeting}</p>
          <h1 className="text-3xl font-bold text-foreground">Hoje</h1>
        </div>
        <Link to="/notificacoes" aria-label="Notificações" className="w-10 h-10 rounded-full bg-card border border-border flex items-center justify-center transition-transform active:scale-95">
          <Bell className="w-4 h-4 text-muted-foreground" />
        </Link>
      </header>

      {isLoading ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-sm text-muted-foreground">Carregando plano...</div>
      ) : !plan ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-center">
          <p className="text-sm font-semibold text-foreground mb-2">Você ainda não tem um plano</p>
          <p className="text-xs text-muted-foreground mb-4">Gere seu plano de treino para começar.</p>
          <Link to="/gerar-plano" className="inline-flex items-center gap-2 bg-primary text-primary-foreground rounded-xl px-4 py-3 text-sm font-semibold transition-transform active:scale-95">
            <Plus className="w-4 h-4" /> Gerar plano
          </Link>
        </div>
      ) : (
        <>
          <div className="mb-6">
            <WeekStrip sessions={sessions} selectedDay={selectedDay} onSelectDay={setSelectedDay} />
          </div>

          <section className="mb-6">
            {highlightedSession ? (
              <TodayWorkoutCard session={highlightedSession} planoId={plan.id} sessionIndex={highlightedSessionIndex} />
            ) : (
              <div className="rounded-3xl bg-card border border-border p-8 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Nenhum treino pendente</p>
                <p className="text-xs text-muted-foreground">Você concluiu os treinos do plano ou ele está pausado.</p>
              </div>
            )}
          </section>

          <section className="mb-6">
            <CheckinInsightCard checkin={latestCheckin(checkins)} currentWeek={currentWeek} />
          </section>

          <Link to="/plano" className="block rounded-2xl bg-card border border-border p-4 transition-transform active:scale-[0.98]">
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-semibold text-foreground">Semana {currentWeek} de {plan.total_semanas}</p>
              <span className="inline-flex items-center gap-1 text-xs text-muted-foreground tabular-nums">
                {completedThisWeek} de {currentWeekSessions.length} treinos
                <ChevronRight className="w-4 h-4" />
              </span>
            </div>
            <div className="h-1.5 rounded-full bg-secondary overflow-hidden">
              <div className="h-full rounded-full bg-primary transition-[width] duration-500" style={{ width: `${weekProgress * 100}%` }} />
            </div>
          </Link>
        </>
      )}
    </div>
  );
}
