import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Bell, Plus } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import AIInsightCard from '@/components/home/AlInsightCard';
import QuickStats from '@/components/home/QuickStats';
import TodayWorkoutCard from '@/components/home/TodayWorkoutCard';
import WeekStrip from '@/components/home/WeekStrip';

const DAY_KEYS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sab'];

export default function Home() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [selectedDay, setSelectedDay] = useState(new Date().getDay());

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ['meus-planos'],
    queryFn: api.planos.listarMeus,
  });

  const plan = plans.find((item) => item.status === 'ativo') || plans[0];
  const sessions = plan?.sessoes || [];
  const currentWeek = plan?.semana_atual || 1;
  const currentWeekSessions = sessions.filter((session) => session.semana === currentWeek);
  const todaySession = currentWeekSessions.find((session) => session.dia_semana === DAY_KEYS[selectedDay]);
  const todaySessionIndex = sessions.findIndex((session) => session.id === todaySession?.id);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bom dia';
    if (hour < 18) return 'Boa tarde';
    return 'Boa noite';
  }, []);

  if (!user?.objetivo) {
    navigate('/onboarding');
    return null;
  }

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-center justify-between mb-6">
        <div>
          <p className="text-xs text-muted-foreground">{greeting}, {user?.nome || 'atleta'}</p>
          <h1 className="text-2xl font-bold text-foreground">SeuCorre</h1>
        </div>
        <button className="w-10 h-10 rounded-full bg-card border border-border flex items-center justify-center">
          <Bell className="w-4 h-4 text-muted-foreground" />
        </button>
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
          <div className="mb-6">
            <WeekStrip sessions={currentWeekSessions} selectedDay={selectedDay} onSelectDay={setSelectedDay} />
          </div>

          <section className="mb-6">
            <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Treino do dia</h2>
            {todaySession ? (
              <TodayWorkoutCard session={todaySession} planoId={plan.id} sessionIndex={todaySessionIndex} />
            ) : (
              <div className="rounded-2xl bg-card border border-border p-8 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Dia sem treino planejado</p>
                <p className="text-xs text-muted-foreground">Use o descanso para recuperar bem.</p>
              </div>
            )}
          </section>

          <section className="mb-6">
            <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Seu progresso</h2>
            <QuickStats sessions={sessions} />
          </section>

          {plan.ia_insights && <AIInsightCard insight={plan.ia_insights} />}
        </>
      )}
    </div>
  );
}
