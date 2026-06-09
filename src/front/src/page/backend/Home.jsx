import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Bell, CalendarDays, ChevronRight, ClipboardList, History, Plus } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import AIInsightCard from '@/components/home/AlInsightCard';
import QuickStats from '@/components/home/QuickStats';
import TodayWorkoutCard from '@/components/home/TodayWorkoutCard';
import WeekStrip from '@/components/home/WeekStrip';

const DAY_KEYS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sab'];

const formatWeekRange = (sessions = []) => {
  const withDate = sessions.filter((session) => session?.dataPrevista);
  if (withDate.length === 0) return 'Semana atual do plano';

  const first = withDate[0].dataLabel;
  const last = withDate[withDate.length - 1].dataLabel;
  return first && last ? `${first} - ${last}` : 'Semana atual do plano';
};

export default function Home() {
  const { hasCompletedOnboarding, user } = useAuth();
  const [selectedDay, setSelectedDay] = useState(new Date().getDay());

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ['meus-planos'],
    queryFn: api.planos.listarMeus,
  });

  const plan = plans.find((item) => item.status === 'ativo') || plans[0];
  const sessions = plan?.sessoes || [];
  const currentWeek = plan?.semana_atual || 1;
  const currentWeekSessions = sessions.filter((session) => session.semana === currentWeek);
  const selectedDayKey = DAY_KEYS[selectedDay];
  const todaySession = currentWeekSessions.find((session) => session.dia_semana === selectedDayKey);
  const nextWeekSession = currentWeekSessions.find((session) => session.status !== 'concluido');
  const nextPlanSession = sessions.find((session) => session.status !== 'concluido');
  const highlightedSession = todaySession || nextWeekSession || nextPlanSession || null;
  const highlightedSessionIndex = sessions.findIndex((session) => session.id === highlightedSession?.id);
  const completedThisWeek = currentWeekSessions.filter((session) => session.status === 'concluido').length;

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bom dia';
    if (hour < 18) return 'Boa tarde';
    return 'Boa noite';
  }, []);

  const spotlightLabel = todaySession ? 'Treino do dia' : 'Próximo treino';

  if (!hasCompletedOnboarding) {
    return null;
  }

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-center justify-between mb-6">
        <div>
          <p className="text-xs text-muted-foreground">{greeting}, {user?.nome || 'atleta'}</p>
          <h1 className="text-2xl font-bold text-foreground">SeuCorre</h1>
        </div>
        <Link to="/notificacoes" className="w-10 h-10 rounded-full bg-card border border-border flex items-center justify-center">
          <Bell className="w-4 h-4 text-muted-foreground" />
        </Link>
      </div>

      {isLoading ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-sm text-muted-foreground">Carregando plano...</div>
      ) : !plan ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-center">
          <p className="text-sm font-semibold text-foreground mb-2">Nenhum plano ativo</p>
          <p className="text-xs text-muted-foreground mb-4">Gere um plano conectado ao backend para começar.</p>
          <Link to="/gerar-plano" className="inline-flex items-center gap-2 bg-primary text-primary-foreground rounded-xl px-4 py-3 text-sm font-semibold">
            <Plus className="w-4 h-4" /> Gerar plano
          </Link>
        </div>
      ) : (
        <>
          <section className="rounded-3xl bg-card border border-border p-5 mb-6">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div>
                <p className="text-xs text-muted-foreground mb-1">Semana {currentWeek} de {plan.total_semanas}</p>
                <h2 className="text-lg font-bold text-foreground">Seu foco agora é manter consistência</h2>
              </div>
              <div className="px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold shrink-0">
                {completedThisWeek}/{currentWeekSessions.length || 0} feitos
              </div>
            </div>

            <div className="flex items-center gap-2 text-xs text-muted-foreground mb-4">
              <CalendarDays className="w-4 h-4" />
              <span>{formatWeekRange(currentWeekSessions)}</span>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <Link to="/check-in" className="rounded-2xl border border-border bg-background px-4 py-4">
                <div className="flex items-center gap-2 mb-1">
                  <ClipboardList className="w-4 h-4 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Check-in semanal</span>
                </div>
                <p className="text-xs text-muted-foreground">Registre esforço, dor e sono para adaptar o plano.</p>
              </Link>
              <Link to="/historico" className="rounded-2xl border border-border bg-background px-4 py-4">
                <div className="flex items-center gap-2 mb-1">
                  <History className="w-4 h-4 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Histórico</span>
                </div>
                <p className="text-xs text-muted-foreground">Revise treinos concluídos, parciais e perdidos.</p>
              </Link>
            </div>
          </section>

          <div className="mb-6">
            <WeekStrip sessions={currentWeekSessions} selectedDay={selectedDay} onSelectDay={setSelectedDay} />
          </div>

          <section className="mb-6">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">{spotlightLabel}</h2>
              <Link to="/plano" className="text-xs text-primary font-semibold inline-flex items-center gap-1">
                Ver semana
                <ChevronRight className="w-3 h-3" />
              </Link>
            </div>
            {highlightedSession ? (
              <TodayWorkoutCard session={highlightedSession} planoId={plan.id} sessionIndex={highlightedSessionIndex} />
            ) : (
              <div className="rounded-2xl bg-card border border-border p-8 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Nenhum treino pendente agora</p>
                <p className="text-xs text-muted-foreground">Seu plano desta semana já foi concluído ou ainda não foi gerado.</p>
              </div>
            )}
          </section>

          <section className="mb-6">
            <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Seu progresso</h2>
            <QuickStats currentWeek={currentWeek} sessions={sessions} />
          </section>

          {plan.ia_insights && <AIInsightCard insight={plan.ia_insights} />}
        </>
      )}
    </div>
  );
}
