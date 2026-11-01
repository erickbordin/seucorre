import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calendar, CheckCircle2, ChevronRight, Circle, Pause, Play, XCircle } from 'lucide-react';
import { api } from '@/lib/api';

const STATUS_ICONS = {
  concluido: <CheckCircle2 className="w-5 h-5 text-primary" />,
  parcial: <CheckCircle2 className="w-5 h-5 text-orange-400" />,
  perdido: <XCircle className="w-5 h-5 text-muted-foreground" />,
  pendente: <Circle className="w-5 h-5 text-muted-foreground" />,
};

const STATUS_LABELS = {
  concluido: 'Concluído',
  parcial: 'Parcial',
  perdido: 'Perdido',
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
      <header className="flex items-start justify-between gap-4 mb-6">
        <div className="min-w-0">
          <h1 className="text-3xl font-bold text-foreground">Plano</h1>
          {plan?.descricao && <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{plan.descricao}</p>}
        </div>
        {plan && (
          <button onClick={() => statusMutation.mutate()} className="h-10 shrink-0 rounded-xl bg-card border border-border flex items-center justify-center gap-2 px-3 transition-transform active:scale-95">
            {plan.status === 'ativo' ? <Pause className="w-4 h-4 text-muted-foreground" /> : <Play className="w-4 h-4 text-primary" />}
            <span className="text-xs font-semibold text-foreground">{plan.status === 'ativo' ? 'Pausar' : 'Reativar'}</span>
          </button>
        )}
      </header>

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

          <p className="mb-4 text-sm text-muted-foreground tabular-nums">
            {selectedWeek === plan.semana_atual && <span className="font-semibold text-primary">Semana atual · </span>}
            {completedWeekSessions} de {weekSessions.length} feitos · {weekDistance.toFixed(1).replace('.', ',')} km
          </p>

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
                  <div className="flex items-center gap-4 p-4 rounded-2xl bg-card border border-border">
                    {STATUS_ICONS[session.status] || STATUS_ICONS.pendente}
                    <div className="flex-1 min-w-0">
                      <p className="text-base font-semibold text-foreground truncate">{session.tipoLabel}</p>
                      <p className="mt-0.5 text-xs text-muted-foreground tabular-nums">
                        {session.dataLabel} · {session.duracao_min || '-'} min · {String(session.distancia_km || 0).replace('.', ',')} km
                        {STATUS_LABELS[session.status] && ` · ${STATUS_LABELS[session.status]}`}
                      </p>
                    </div>
                    <ChevronRight className="w-4 h-4 text-muted-foreground" />
                  </div>
                </Link>
              );
            })}
          </div>

          <Link to="/historico" className="mt-6 inline-flex items-center gap-1 text-sm font-semibold text-primary">
            Ver histórico de treinos
            <ChevronRight className="w-4 h-4" />
          </Link>
        </>
      )}
    </div>
  );
}
