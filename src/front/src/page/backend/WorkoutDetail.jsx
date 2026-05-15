import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle2, Clock, Heart, Loader2, MapPin, Save, Timer, Zap } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { api } from '@/lib/api';

const STATUS_OPTIONS = [
  {
    value: 'CONCLUIDO',
    label: 'Concluí como planejado',
    description: 'Completei a sessão e quero registrar as métricas principais.',
  },
  {
    value: 'PARCIAL',
    label: 'Concluí parcialmente',
    description: 'Fiz parte do treino e preciso registrar desvios ou limitações.',
  },
  {
    value: 'PERDIDO',
    label: 'Não consegui fazer',
    description: 'Quero registrar o motivo para manter o histórico e evitar lacunas.',
  },
];

const READABLE_STATUS = {
  CONCLUIDO: 'Concluído',
  PARCIAL: 'Parcial',
  PERDIDO: 'Não realizado',
};

const parsePaceToSeconds = (value) => {
  if (!value) return null;
  const normalized = String(value).trim();
  if (!normalized) return null;

  if (/^\d+$/.test(normalized)) {
    return Number(normalized);
  }

  const match = normalized.match(/^(\d{1,2}):([0-5]\d)$/);
  if (!match) return null;

  const minutes = Number(match[1]);
  const seconds = Number(match[2]);
  return (minutes * 60) + seconds;
};

const formatPaceFromSeconds = (value) => {
  if (!value || Number(value) <= 0) return '-';
  const totalSeconds = Number(value);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')} min/km`;
};

export default function WorkoutDetail() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { planoId, sessionIdx } = useParams();
  const [form, setForm] = useState({
    status: 'CONCLUIDO',
    distanciaRealKm: '',
    duracaoRealMin: '',
    paceMedioReal: '',
    esforcoPercebido: '',
    fcMedia: '',
    fcMaxima: '',
    sentiuDor: false,
    localDor: '',
    doente: false,
    viagem: false,
    observacao: '',
  });

  const { data: plan, isLoading } = useQuery({
    queryKey: ['plano', planoId],
    queryFn: () => api.planos.buscar(planoId),
    enabled: Boolean(planoId),
  });

  const session = plan?.sessoes?.[Number(sessionIdx)];
  const alreadyRegistered = Boolean(session?.registro);
  const shouldShowMetrics = form.status !== 'PERDIDO';
  const parsedPace = parsePaceToSeconds(form.paceMedioReal);

  const validationMessage = useMemo(() => {
    if (!session) return '';
    if (form.status === 'PERDIDO') {
      if (!form.doente && !form.viagem && !form.observacao.trim()) {
        return 'Explique por que o treino não aconteceu para manter o histórico útil.';
      }
      return '';
    }

    if (!form.distanciaRealKm) {
      return 'Informe a distância realizada para registrar a sessão.';
    }

    if (form.paceMedioReal && parsedPace == null) {
      return 'Use o formato de pace mm:ss, por exemplo 5:30.';
    }

    if (form.sentiuDor && !form.localDor.trim()) {
      return 'Se marcou dor, informe onde ela aconteceu.';
    }

    return '';
  }, [form, parsedPace, session]);

  const mutation = useMutation({
    mutationFn: () => api.treinos.registrar(session.id, {
      status: form.status,
      distanciaRealKm: shouldShowMetrics && form.distanciaRealKm ? Number(form.distanciaRealKm) : null,
      fcMedia: shouldShowMetrics && form.fcMedia ? Number(form.fcMedia) : null,
      fcMaxima: shouldShowMetrics && form.fcMaxima ? Number(form.fcMaxima) : null,
      paceMedioReal: shouldShowMetrics ? parsedPace : null,
      esforcoPercebido: shouldShowMetrics && form.esforcoPercebido ? Number(form.esforcoPercebido) : null,
      sentiuDor: form.sentiuDor,
      localDor: form.sentiuDor ? form.localDor.trim() || null : null,
      doente: form.doente,
      viagem: form.viagem,
      observacao: form.observacao.trim() || null,
      // TODO: conectar duracao real quando o backend aceitar esse campo no endpoint de registro.
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
      queryClient.invalidateQueries({ queryKey: ['plano', planoId] });
      queryClient.invalidateQueries({ queryKey: ['progresso'] });
      queryClient.invalidateQueries({ queryKey: ['historico-treinos'] });
      navigate('/plano');
    },
  });

  const metrics = useMemo(() => [
    { icon: Clock, label: 'Duração planejada', value: `${session?.duracao_min || '-'} min` },
    { icon: MapPin, label: 'Distância planejada', value: `${session?.distancia_km || 0} km` },
    { icon: Zap, label: 'Pace alvo', value: session?.pace_alvo_min || '-' },
    { icon: Heart, label: 'Zona alvo', value: session?.zona_fc || '-' },
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

      <div className="grid grid-cols-2 gap-3 mb-6">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <div key={metric.label} className="bg-card rounded-2xl p-4 border border-border/50">
              <Icon className="w-4 h-4 text-muted-foreground mb-2" />
              <p className="text-sm font-bold text-foreground break-words">{metric.value}</p>
              <p className="text-[10px] text-muted-foreground">{metric.label}</p>
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

      {alreadyRegistered ? (
        <section className="rounded-2xl border border-border bg-card p-5 space-y-4">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="w-5 h-5 text-primary mt-0.5" />
            <div>
              <h2 className="text-sm font-semibold text-foreground">Treino já registrado</h2>
              <p className="text-xs text-muted-foreground mt-1">
                Esta sessão já foi salva. Se precisar corrigir dados, o backend ainda não expõe edição de registro.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl border border-border p-3">
              <p className="text-[10px] text-muted-foreground mb-1">Status</p>
              <p className="text-sm font-semibold text-foreground">{READABLE_STATUS[session.registro?.status] || session.registro?.status || '-'}</p>
            </div>
            <div className="rounded-xl border border-border p-3">
              <p className="text-[10px] text-muted-foreground mb-1">Distância real</p>
              <p className="text-sm font-semibold text-foreground">{session.registro?.distanciaRealKm ? `${session.registro.distanciaRealKm} km` : '-'}</p>
            </div>
            <div className="rounded-xl border border-border p-3">
              <p className="text-[10px] text-muted-foreground mb-1">Pace médio</p>
              <p className="text-sm font-semibold text-foreground">{formatPaceFromSeconds(session.registro?.paceMedioReal)}</p>
            </div>
            <div className="rounded-xl border border-border p-3">
              <p className="text-[10px] text-muted-foreground mb-1">Esforço percebido</p>
              <p className="text-sm font-semibold text-foreground">{session.registro?.esforcoPercebido ?? '-'}</p>
            </div>
          </div>

          {session.registro?.observacao && (
            <div className="rounded-xl border border-border p-3">
              <p className="text-[10px] text-muted-foreground mb-1">Observação</p>
              <p className="text-sm text-foreground">{session.registro.observacao}</p>
            </div>
          )}
        </section>
      ) : (
        <form onSubmit={(event) => { event.preventDefault(); if (!validationMessage) mutation.mutate(); }} className="space-y-5">
          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Como foi essa sessão?</h2>
              <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
            </div>
            <div className="space-y-2">
              {STATUS_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setForm((current) => ({ ...current, status: option.value }))}
                  className={`w-full rounded-2xl border p-4 text-left ${form.status === option.value ? 'bg-primary/10 border-primary/40 ring-1 ring-primary/20' : 'bg-card border-border'}`}
                >
                  <p className={`text-sm font-semibold ${form.status === option.value ? 'text-primary' : 'text-foreground'}`}>{option.label}</p>
                  <p className="text-xs text-muted-foreground mt-1">{option.description}</p>
                </button>
              ))}
            </div>
          </section>

          {shouldShowMetrics && (
            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Métricas da execução</h2>
                <span className="text-[10px] font-semibold text-primary">Obrigatório em parte</span>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Input type="number" step="0.1" placeholder="Distância real km" value={form.distanciaRealKm} onChange={(event) => setForm({ ...form, distanciaRealKm: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="text" inputMode="numeric" placeholder="Pace médio mm:ss" value={form.paceMedioReal} onChange={(event) => setForm({ ...form, paceMedioReal: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="Esforço 0-10" value={form.esforcoPercebido} onChange={(event) => setForm({ ...form, esforcoPercebido: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="FC média" value={form.fcMedia} onChange={(event) => setForm({ ...form, fcMedia: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="FC máxima" value={form.fcMaxima} onChange={(event) => setForm({ ...form, fcMaxima: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="Duração real min" value={form.duracaoRealMin} onChange={(event) => setForm({ ...form, duracaoRealMin: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
              </div>
              <div className="rounded-xl border border-border bg-card p-3 flex items-start gap-3">
                <Timer className="w-4 h-4 text-muted-foreground mt-0.5" />
                <p className="text-xs text-muted-foreground">
                  A duração real já aparece no formulário para o fluxo ficar completo no app.
                  Ela ainda não é persistida pela API atual.
                </p>
              </div>
            </section>
          )}

          <section className="space-y-3">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Contexto e alertas</h2>
            <div className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div className="flex items-center space-x-3">
                <Checkbox id="sentiuDor" checked={form.sentiuDor} onCheckedChange={(checked) => setForm((current) => ({ ...current, sentiuDor: Boolean(checked), localDor: checked ? current.localDor : '' }))} />
                <Label htmlFor="sentiuDor" className="text-sm text-foreground">Senti dor durante ou depois do treino</Label>
              </div>
              {form.sentiuDor && (
                <Input placeholder="Onde foi a dor? Ex.: joelho, panturrilha" value={form.localDor} onChange={(event) => setForm({ ...form, localDor: event.target.value })} className="bg-background border-border h-12 rounded-xl" />
              )}

              <div className="flex items-center space-x-3">
                <Checkbox id="doente" checked={form.doente} onCheckedChange={(checked) => setForm((current) => ({ ...current, doente: Boolean(checked) }))} />
                <Label htmlFor="doente" className="text-sm text-foreground">Estava doente ou sem condições físicas ideais</Label>
              </div>

              <div className="flex items-center space-x-3">
                <Checkbox id="viagem" checked={form.viagem} onCheckedChange={(checked) => setForm((current) => ({ ...current, viagem: Boolean(checked) }))} />
                <Label htmlFor="viagem" className="text-sm text-foreground">Viagem ou logística atrapalhou o treino</Label>
              </div>
            </div>
          </section>

          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Observações</h2>
              <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
            </div>
            <Textarea
              placeholder="Ex.: reduzi o treino por dor, fiz na esteira, clima muito quente, viagem de trabalho..."
              value={form.observacao}
              onChange={(event) => setForm({ ...form, observacao: event.target.value })}
              className="min-h-[120px] rounded-xl border-border bg-card"
            />
          </section>

          {(validationMessage || mutation.error) && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4">
              <div className="flex items-start gap-3">
                <AlertTriangle className="w-4 h-4 text-destructive mt-0.5" />
                <div>
                  {validationMessage && <p className="text-sm text-destructive">{validationMessage}</p>}
                  {mutation.error && <p className="text-sm text-destructive">{mutation.error.message}</p>}
                  <p className="text-xs text-destructive/80 mt-1">Revise os campos e tente novamente.</p>
                </div>
              </div>
            </div>
          )}

          <Button type="submit" disabled={mutation.isPending || Boolean(validationMessage)} className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-semibold gap-2">
            {mutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
            Registrar treino
          </Button>
        </form>
      )}
    </div>
  );
}
