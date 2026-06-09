import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calendar, CheckCircle2, ChevronRight, Circle, ClipboardList, Clock, History, MapPin, Pause, Play } from 'lucide-react';
import { api } from '@/lib/api';
import AIInsightCard from '@/components/home/AlInsightCard';

const STATUS_LABELS = {
  concluido: 'Concluído',
  parcial: 'Parcial',
  perdido: 'Perdido',
  pendente: 'Pendente',
};

export default function Plan() {
  const queryClient = useQueryClient();
  const [selectedWeek, setSelectedWeek] = useState(1);
  const { data: plans = [], isLoading } = useQuery({ queryKey: ['meus-planos'], queryFn: api.planos.listarMeus });
  const plan = plans.find((item) => item.status === 'ativo') || plans[0];
  const sessions = plan?.sessoes || [];
  const totalWeeks = plan?.total_semanas || 4;
  const weekSessions = useMemo(
    () => sessions.filter((session) => session.semana === selectedWeek),
    [selectedWeek, sessions],
  );

  useEffect(() => {
    if (plan?.semana_atual) {
      setSelectedWeek(plan.semana_atual);
    }
  }, [plan?.id, plan?.semana_atual]);

  const weekDistance = weekSessions.reduce((acc, session) => acc + Number(session.distancia_km || 0), 0);
  const completedWeekSessions = weekSessions.filter((session) => session.status === 'concluido').length;

  const statusMutation = useMutation({
    mutationFn: () => plan?.status === 'ativo' ? api.planos.pausar(plan.id) : api.planos.reativar(plan.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['meus-planos'] }),
  });

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <p className="text-xs text-muted-foreground mb-1">Seu plano de treino</p>
          <h1 className="text-2xl font-bold text-foreground">{plan?.nome || 'Plano de Corrida'}</h1>
        </div>
        {plan && (
          <button onClick={() => statusMutation.mutate()} className="h-10 rounded-xl bg-card border border-border flex items-center justify-center gap-2 px-3">
            {plan.status === 'ativo' ? <Pause className="w-4 h-4 text-muted-foreground" /> : <Play className="w-4 h-4 text-primary" />}
            <span className="text-xs font-semibold text-foreground">{plan.status === 'ativo' ? 'Pausar' : 'Reativar'}</span>
          </button>
        )}
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando plano...</p>
      ) : !plan ? (
        <div className="text-center py-16">
          <Calendar className="w-10 h-10 text-muted-foreground mx-auto mb-3" />
          <p className="text-sm text-muted-foreground mb-4">Nenhum plano encontrado.</p>
          <Link to="/gerar-plano" className="text-sm text-primary font-semibold">Gerar plano</Link>
        </div>
      ) : (
        <>
          <div className="flex gap-2 mb-6 overflow-x-auto no-scrollbar">
            {Array.from({ length: totalWeeks }, (_, index) => index + 1).map((week) => (
              <button key={week} onClick={() => setSelectedWeek(week)} className={`px-4 py-2 rounded-xl text-sm font-medium shrink-0 ${selectedWeek === week ? 'bg-primary text-primary-foreground' : 'bg-card border border-border text-muted-foreground'}`}>
                Sem {week}
              </button>
            ))}
          </div>

          <section className="rounded-3xl bg-card border border-border p-5 mb-6">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div>
                <p className="text-xs text-muted-foreground mb-1">Semana selecionada</p>
                <h2 className="text-lg font-bold text-foreground">Semana {selectedWeek}</h2>
              </div>
              {selectedWeek === plan.semana_atual && (
                <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">Semana atual</span>
              )}
            </div>

            <div className="grid grid-cols-3 gap-3 mb-4">
              <div className="rounded-2xl bg-background border border-border px-3 py-4 text-center">
                <p className="text-lg font-bold text-foreground">{completedWeekSessions}/{weekSessions.length || 0}</p>
                <p className="text-[10px] text-muted-foreground">Sessões</p>
              </div>
              <div className="rounded-2xl bg-background border border-border px-3 py-4 text-center">
                <p className="text-lg font-bold text-foreground">{weekDistance.toFixed(1)} km</p>
                <p className="text-[10px] text-muted-foreground">Volume</p>
              </div>
              <div className="rounded-2xl bg-background border border-border px-3 py-4 text-center">
                <p className="text-lg font-bold text-foreground">{plan.total_semanas}</p>
                <p className="text-[10px] text-muted-foreground">Semanas</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <Link to="/check-in" className="rounded-2xl border border-border bg-background p-4">
                <div className="flex items-center gap-2 mb-1">
                  <ClipboardList className="w-4 h-4 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Check-in semanal</span>
                </div>
                <p className="text-xs text-muted-foreground">Atualize a IA com esforço, dor e recuperação.</p>
              </Link>
              <Link to="/historico" className="rounded-2xl border border-border bg-background p-4">
                <div className="flex items-center gap-2 mb-1">
                  <History className="w-4 h-4 text-primary" />
                  <span className="text-sm font-semibold text-foreground">Histórico</span>
                </div>
                <p className="text-xs text-muted-foreground">Revise o que já foi executado no ciclo.</p>
              </Link>
            </div>
          </section>

          <div className="space-y-3">
            {weekSessions.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-border p-6 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Sem sessões nesta semana</p>
                <p className="text-xs text-muted-foreground">Escolha outra semana do plano para revisar os treinos.</p>
              </div>
            ) : weekSessions.map((session) => {
              const globalIdx = sessions.findIndex((item) => item.id === session.id);
              const done = session.status === 'concluido';
              return (
                <Link key={session.id} to={`/treino/${plan.id}/${globalIdx}`} className="block">
                  <div className="flex items-center gap-4 p-4 rounded-2xl bg-card border border-border">
                    {done ? <CheckCircle2 className="w-5 h-5 text-green-400" /> : <Circle className="w-5 h-5 text-muted-foreground" />}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary">{session.tipoLabel || session.tipoOriginal}</span>
                        <span className="text-[10px] text-muted-foreground">{session.dataLabel || session.dataPrevista}</span>
                        <span className="text-[10px] text-muted-foreground">{STATUS_LABELS[session.status] || 'Pendente'}</span>
                      </div>
                      <p className="text-sm font-semibold text-foreground truncate">{session.titulo}</p>
                      <div className="flex items-center gap-3 mt-1">
                        <span className="text-[10px] text-muted-foreground flex items-center gap-1"><Clock className="w-3 h-3" /> {session.duracao_min || '-'} min</span>
                        <span className="text-[10px] text-muted-foreground flex items-center gap-1"><MapPin className="w-3 h-3" /> {session.distancia_km || 0} km</span>
                      </div>
                    </div>
                    <ChevronRight className="w-4 h-4 text-muted-foreground" />
                  </div>
                </Link>
              );
            })}
          </div>

          {plan.ia_insights && (
            <div className="mt-6">
              <AIInsightCard insight={plan.ia_insights} />
            </div>
          )}
        </>
      )}
    </div>
  );
}
