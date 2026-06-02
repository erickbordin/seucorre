import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calendar, CheckCircle2, ChevronRight, Circle, Clock, MapPin, Pause, Play } from 'lucide-react';
import { api } from '@/lib/api';

const TYPE_LABELS = {
  easy_run: 'Leve',
  intervals: 'Intervalado',
  long_run: 'Longão',
  recovery: 'Recuperação',
  tempo: 'Tempo',
};

export default function Plan() {
  const queryClient = useQueryClient();
  const [selectedWeek, setSelectedWeek] = useState(1);
  const { data: plans = [], isLoading } = useQuery({ queryKey: ['meus-planos'], queryFn: api.planos.listarMeus });
  const plan = plans.find((item) => item.status === 'ativo') || plans[0];
  const sessions = plan?.sessoes || [];
  const totalWeeks = plan?.total_semanas || 4;
  const weekSessions = sessions.filter((session) => session.semana === selectedWeek);

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
          <button onClick={() => statusMutation.mutate()} className="w-10 h-10 rounded-xl bg-card border border-border flex items-center justify-center">
            {plan.status === 'ativo' ? <Pause className="w-4 h-4 text-muted-foreground" /> : <Play className="w-4 h-4 text-primary" />}
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

          <div className="space-y-3">
            {weekSessions.map((session) => {
              const globalIdx = sessions.findIndex((item) => item.id === session.id);
              const done = session.status === 'concluido';
              return (
                <Link key={session.id} to={`/treino/${plan.id}/${globalIdx}`} className="block">
                  <div className="flex items-center gap-4 p-4 rounded-2xl bg-card border border-border">
                    {done ? <CheckCircle2 className="w-5 h-5 text-green-400" /> : <Circle className="w-5 h-5 text-muted-foreground" />}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary/10 text-primary">{TYPE_LABELS[session.tipo] || session.tipoOriginal}</span>
                        <span className="text-[10px] text-muted-foreground">{session.dataPrevista}</span>
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
        </>
      )}
    </div>
  );
}
