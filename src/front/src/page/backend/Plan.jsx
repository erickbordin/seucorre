import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calendar, CheckCircle2, ChevronRight, ClipboardCheck, History, Pause, Play, Target, XCircle } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import { GOAL_LABELS } from '@/lib/user';
import { WORKOUT_TYPE_COLORS } from '@/lib/workoutTypes';

const STATUS_ICONS = {
  concluido: <CheckCircle2 className="w-5 h-5 text-primary" />,
  parcial: <CheckCircle2 className="w-5 h-5 text-orange-400" />,
  perdido: <XCircle className="w-5 h-5 text-muted-foreground" />,
  pendente: <ChevronRight className="w-5 h-5 text-muted-foreground" />,
};

const STATUS_LABELS = {
  concluido: 'Concluído',
  parcial: 'Parcial',
  perdido: 'Perdido',
};

const GOAL_BADGES = {
  COMPLETAR_5K: '5K',
  MELHORAR_5K: '5K',
  COMPLETAR_10K: '10K',
  MELHORAR_10K: '10K',
  COMPLETAR_MEIA_MARATONA: '21K',
  MELHORAR_MEIA_MARATONA: '21K',
  COMPLETAR_MARATONA: '42K',
  MELHORAR_MARATONA: '42K',
};

const formatKm = (value) => value.toLocaleString('pt-BR', { maximumFractionDigits: 1 });
const sumDistance = (items) => items.reduce((acc, session) => acc + Number(session.distancia_km || 0), 0);
const doneDistance = (session) => Number(session.registro?.distanciaRealKm ?? session.distancia_km ?? 0);

export default function Plan() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
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

  const finishedWeeks = useMemo(() => {
    const weeks = new Set();
    for (let week = 1; week <= totalWeeks; week += 1) {
      const items = sessions.filter((session) => session.semana === week);
      if (items.every((session) => session.status !== 'pendente') && items.some((session) => session.status === 'concluido' || session.status === 'parcial')) {
        weeks.add(week);
      }
    }
    return weeks;
  }, [sessions, totalWeeks]);

  const totalDistance = sumDistance(sessions);
  const completedDistance = sessions
    .filter((session) => session.status === 'concluido' || session.status === 'parcial')
    .reduce((acc, session) => acc + doneDistance(session), 0);
  const weekDistance = sumDistance(weekSessions);
  const previousWeekDistance = sumDistance(sessions.filter((session) => session.semana === selectedWeek - 1));
  const volumeChange = previousWeekDistance > 0 ? ((weekDistance - previousWeekDistance) / previousWeekDistance) * 100 : null;
  const withinLimit = Math.round(weekDistance * 100) <= Math.round(previousWeekDistance * 110);
  const completedWeekSessions = weekSessions.filter((session) => session.status === 'concluido').length;
  const goalBadge = GOAL_BADGES[user?.objetivo];
  const endDate = plan?.dataFim
    ? new Date(`${plan.dataFim}T00:00:00`).toLocaleDateString('pt-BR', { day: 'numeric', month: 'long' })
    : null;

  const statusMutation = useMutation({
    mutationFn: () => plan?.status === 'ativo' ? api.planos.pausar(plan.id) : api.planos.reativar(plan.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['meus-planos'] }),
  });

  return (
    <div className="px-5 pt-14 pb-28">
      <h1 className="mb-6 text-3xl font-bold text-foreground">Plano</h1>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando plano...</p>
      ) : !plan ? (
        <div className="text-center py-16">
          <Calendar className="w-10 h-10 text-muted-foreground mx-auto mb-3" />
          <p className="text-sm text-muted-foreground mb-4">Você ainda não tem um plano.</p>
          <Link to="/gerar-plano" className="text-sm text-primary font-semibold">Gerar plano</Link>
        </div>
      ) : (
        <>
          <section className="mb-3 rounded-3xl border border-border bg-card p-5">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Seu objetivo</p>
                <h2 className="mt-1 text-xl font-bold text-foreground">{GOAL_LABELS[user?.objetivo] || 'Plano de corrida'}</h2>
                {endDate && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    {plan.status === 'ativo' ? `Termina em ${endDate}` : `Pausado · terminaria em ${endDate}`}
                  </p>
                )}
              </div>
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border-2 border-primary text-sm font-bold text-primary">
                {goalBadge || <Target className="h-5 w-5" />}
              </div>
            </div>

            <div className="mt-5 flex gap-1" aria-hidden="true">
              {Array.from({ length: totalWeeks }, (_, index) => index + 1).map((week) => (
                <span
                  key={week}
                  className={`h-1.5 flex-1 rounded-full ${finishedWeeks.has(week) ? 'bg-primary' : week === plan.semana_atual ? 'bg-primary/40' : 'bg-muted'}`}
                />
              ))}
            </div>

            <div className="mt-4 flex items-end justify-between tabular-nums">
              <div>
                <p className="text-xs text-muted-foreground">Semanas concluídas</p>
                <p className="mt-0.5 text-lg font-bold text-foreground">{finishedWeeks.size}/{totalWeeks}</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-muted-foreground">Distância</p>
                <p className="mt-0.5 text-lg font-bold text-foreground">{formatKm(completedDistance)}/{formatKm(totalDistance)} km</p>
              </div>
            </div>

            {plan.descricao && (
              <p className="mt-4 border-t border-border pt-4 text-xs text-muted-foreground line-clamp-2">{plan.descricao}</p>
            )}
          </section>

          <section className="mb-6 grid grid-cols-3 rounded-3xl border border-border bg-card px-2 py-4">
            <Link to="/check-in" className="flex flex-col items-center gap-2 transition-transform active:scale-95">
              <span className="flex h-11 w-11 items-center justify-center rounded-full bg-secondary"><ClipboardCheck className="h-5 w-5 text-foreground" /></span>
              <span className="text-xs font-medium text-foreground">Check-in</span>
            </Link>
            <Link to="/historico" className="flex flex-col items-center gap-2 transition-transform active:scale-95">
              <span className="flex h-11 w-11 items-center justify-center rounded-full bg-secondary"><History className="h-5 w-5 text-foreground" /></span>
              <span className="text-xs font-medium text-foreground">Histórico</span>
            </Link>
            <button
              type="button"
              onClick={() => statusMutation.mutate()}
              disabled={statusMutation.isPending}
              className="flex flex-col items-center gap-2 transition-transform active:scale-95 disabled:opacity-50"
            >
              <span className="flex h-11 w-11 items-center justify-center rounded-full bg-secondary">
                {plan.status === 'ativo' ? <Pause className="h-5 w-5 text-foreground" /> : <Play className="h-5 w-5 text-primary" />}
              </span>
              <span className="text-xs font-medium text-foreground">{plan.status === 'ativo' ? 'Pausar plano' : 'Reativar plano'}</span>
            </button>
          </section>
          {statusMutation.isError && (
            <p className="-mt-4 mb-6 text-center text-xs text-destructive" role="alert">Não foi possível alterar o plano. Tente de novo.</p>
          )}

          <div className="-mx-5 px-5 flex gap-2 mb-4 overflow-x-auto no-scrollbar">
            {Array.from({ length: totalWeeks }, (_, index) => index + 1).map((week) => (
              <button
                key={week}
                onClick={() => setSelectedWeek(week)}
                className={`relative px-4 py-2 rounded-xl text-sm font-medium shrink-0 transition-transform active:scale-95 ${selectedWeek === week ? 'bg-primary text-primary-foreground' : 'bg-card border border-border text-muted-foreground'}`}
              >
                Semana {week}
                {week === plan.semana_atual && selectedWeek !== week && (
                  <span className="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-primary" />
                )}
              </button>
            ))}
          </div>

          <div className="mb-4 text-sm text-muted-foreground tabular-nums">
            <p>
              {selectedWeek === plan.semana_atual && <span className="font-semibold text-primary">Semana atual · </span>}
              {completedWeekSessions} de {weekSessions.length} feitos · {formatKm(weekDistance)} km
            </p>
            {volumeChange !== null && (
              <p className="mt-1 text-xs">
                Volume {volumeChange > 0 ? '+' : ''}{formatKm(volumeChange)}% vs. semana anterior
                {volumeChange > 0 && (
                  <span className={withinLimit ? 'text-primary' : 'text-orange-400'}>
                    {withinLimit ? ' · dentro do limite de 10%' : ' · acima do limite de 10%'}
                  </span>
                )}
              </p>
            )}
          </div>

          <div className="space-y-2">
            {weekSessions.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-border p-6 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Sem treinos nesta semana</p>
                <p className="text-xs text-muted-foreground">Escolha outra semana do plano.</p>
              </div>
            ) : weekSessions.map((session) => {
              const globalIdx = sessions.findIndex((item) => item.id === session.id);
              return (
                <Link key={session.id} to={`/treino/${plan.id}/${globalIdx}`} className="block transition-transform active:scale-[0.98]">
                  <div className="flex items-center gap-3 p-4 rounded-2xl bg-card border border-border">
                    <span className={`h-2.5 w-2.5 shrink-0 rounded-full ${WORKOUT_TYPE_COLORS[session.tipo] || WORKOUT_TYPE_COLORS.easy_run}`} aria-hidden="true" />
                    <div className="flex-1 min-w-0">
                      <p className="text-base font-semibold text-foreground truncate">{session.tipoLabel}</p>
                      <p className="mt-0.5 text-xs text-muted-foreground tabular-nums">
                        {session.dataLabel} · {session.duracao_min || '-'} min · {formatKm(Number(session.distancia_km || 0))} km
                        {STATUS_LABELS[session.status] && ` · ${STATUS_LABELS[session.status]}`}
                      </p>
                    </div>
                    {STATUS_ICONS[session.status] || STATUS_ICONS.pendente}
                  </div>
                </Link>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
