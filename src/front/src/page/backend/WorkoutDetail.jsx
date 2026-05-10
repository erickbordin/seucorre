import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Clock, Heart, Loader2, MapPin, Save, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { api } from '@/lib/api';

export default function WorkoutDetail() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { planoId, sessionIdx } = useParams();
  const [form, setForm] = useState({
    status: 'CONCLUIDO',
    distanciaRealKm: '',
    paceMedioReal: '',
    esforcoPercebido: '',
    fcMedia: '',
    fcMaxima: '',
    observacao: '',
  });

  const { data: plan, isLoading } = useQuery({
    queryKey: ['plano', planoId],
    queryFn: () => api.planos.buscar(planoId),
    enabled: Boolean(planoId),
  });

  const session = plan?.sessoes?.[Number(sessionIdx)];
  const mutation = useMutation({
    mutationFn: () => api.treinos.registrar(session.id, {
      status: form.status,
      distanciaRealKm: form.distanciaRealKm ? Number(form.distanciaRealKm) : session.distancia_km || null,
      paceMedioReal: form.paceMedioReal ? Number(form.paceMedioReal) : null,
      esforcoPercebido: form.esforcoPercebido ? Number(form.esforcoPercebido) : null,
      fcMedia: form.fcMedia ? Number(form.fcMedia) : null,
      fcMaxima: form.fcMaxima ? Number(form.fcMaxima) : null,
      sentiuDor: false,
      doente: false,
      viagem: false,
      observacao: form.observacao || null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
      queryClient.invalidateQueries({ queryKey: ['plano', planoId] });
      queryClient.invalidateQueries({ queryKey: ['progresso'] });
      navigate('/plano');
    },
  });

  const metrics = useMemo(() => [
    { icon: Clock, label: 'Duração', value: `${session?.duracao_min || '-'} min` },
    { icon: MapPin, label: 'Distância', value: `${session?.distancia_km || 0} km` },
    { icon: Zap, label: 'Pace', value: session?.pace_alvo_min || '-' },
    { icon: Heart, label: 'Zona FC', value: session?.zona_fc || '-' },
  ], [session]);

  if (isLoading || !session) {
    return <div className="min-h-screen flex items-center justify-center text-sm text-muted-foreground">Carregando treino...</div>;
  }

  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto px-5 pt-14 pb-10">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
        <ArrowLeft className="w-4 h-4" /> Voltar
      </button>

      <div className="mb-6">
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-primary/10 text-primary">Semana {session.semana}</span>
        <h1 className="text-2xl font-bold text-foreground mt-3 mb-2">{session.titulo}</h1>
        <p className="text-sm text-muted-foreground">{session.descricao}</p>
      </div>

      <div className="grid grid-cols-4 gap-3 mb-6">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <div key={metric.label} className="bg-card rounded-2xl p-3 border border-border/50 text-center">
              <Icon className="w-4 h-4 text-muted-foreground mx-auto mb-1" />
              <p className="text-xs font-bold text-foreground break-words">{metric.value}</p>
              <p className="text-[9px] text-muted-foreground">{metric.label}</p>
            </div>
          );
        })}
      </div>

      <section className="mb-8">
        <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Estrutura</h2>
        <div className="space-y-3">
          {session.estrutura.map((fase) => (
            <div key={fase.fase} className="rounded-2xl bg-card border border-border p-4">
              <p className="text-xs text-primary font-semibold capitalize mb-1">{fase.fase.replace('_', ' ')}</p>
              <p className="text-sm text-foreground">{fase.descricao}</p>
              <p className="text-xs text-muted-foreground mt-2">{fase.duracao}</p>
            </div>
          ))}
        </div>
      </section>

      <form onSubmit={(event) => { event.preventDefault(); mutation.mutate(); }} className="space-y-3">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Registrar execução</h2>
        <div className="grid grid-cols-2 gap-3">
          <Input type="number" step="0.1" placeholder="Distância real km" value={form.distanciaRealKm} onChange={(event) => setForm({ ...form, distanciaRealKm: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
          <Input type="number" placeholder="Pace médio min/km" value={form.paceMedioReal} onChange={(event) => setForm({ ...form, paceMedioReal: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
          <Input type="number" placeholder="Esforço 0-10" value={form.esforcoPercebido} onChange={(event) => setForm({ ...form, esforcoPercebido: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
          <Input type="number" placeholder="FC média" value={form.fcMedia} onChange={(event) => setForm({ ...form, fcMedia: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
        </div>
        {mutation.error && <p className="text-sm text-destructive">{mutation.error.message}</p>}
        <Button type="submit" disabled={mutation.isPending} className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-semibold gap-2">
          {mutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
          Registrar treino
        </Button>
      </form>
    </div>
  );
}
