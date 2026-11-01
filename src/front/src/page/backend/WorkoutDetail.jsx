import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, Calendar, CheckCircle2, ChevronDown, Flag, Loader2, Save } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { api } from '@/lib/api';
import BackButton from '@/components/layout/BackButton';
import { WORKOUT_TYPE_COLORS, formatKm } from '@/lib/workoutTypes';

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

const Field = ({ label, error, children }) => (
  <label className="block space-y-1.5">
    <span className="text-xs font-medium text-muted-foreground">{label}</span>
    {children}
    {error && <span className="block text-xs text-destructive">{error}</span>}
  </label>
);

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
  const totalSeconds = Math.round(Number(value));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
};

const Stat = ({ label, value, unit, hint }) => (
  <div className="min-w-0">
    <p className="text-[0.625rem] font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
    <p className="mt-1 flex flex-wrap items-baseline text-xl font-bold text-foreground tabular-nums">
      {value}
      {unit && <span className="ml-0.5 text-sm font-semibold text-muted-foreground">{unit}</span>}
    </p>
    {hint && <p className="text-[0.625rem] text-muted-foreground">{hint}</p>}
  </div>
);

const plannedStats = (session) => [
  { label: 'Distância', value: formatKm(Number(session.distancia_km || 0)), unit: 'km' },
  { label: 'Duração', value: session.duracao_min || '-', unit: session.duracao_min ? 'min' : null },
  { label: 'Zona', value: session.zona_fc || '-' },
  session.paceAlvo ? { label: 'Pace alvo', value: session.paceAlvo, unit: '/km' } : null,
].filter(Boolean);

const doneStats = (session) => {
  const registro = session.registro || {};
  const distance = Number(registro.distanciaRealKm || 0);
  const duration = Number(registro.duracaoRealMin || 0);
  const pace = registro.paceMedioReal || (distance > 0 && duration > 0 ? (duration * 60) / distance : null);
  return [
    distance > 0 ? { label: 'Distância', value: formatKm(distance), unit: 'km', hint: `de ${formatKm(Number(session.distancia_km || 0))} km` } : null,
    duration > 0 ? { label: 'Tempo', value: duration, unit: 'min' } : null,
    pace ? { label: 'Pace médio', value: formatPaceFromSeconds(pace), unit: '/km' } : null,
    registro.esforcoPercebido != null ? { label: 'Esforço', value: registro.esforcoPercebido, unit: '/10' } : null,
    registro.fcMedia ? { label: 'FC média', value: registro.fcMedia, unit: 'bpm' } : null,
    registro.fcMaxima ? { label: 'FC máxima', value: registro.fcMaxima, unit: 'bpm' } : null,
  ].filter(Boolean);
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
  const [triedSubmit, setTriedSubmit] = useState(false);

  const { data: plan, isLoading } = useQuery({
    queryKey: ['plano', planoId],
    queryFn: () => api.planos.buscar(planoId),
    enabled: Boolean(planoId),
  });

  const session = plan?.sessoes?.[Number(sessionIdx)];

  useEffect(() => {
    if (!session) return;
    setForm((current) => ({
      ...current,
      distanciaRealKm: current.distanciaRealKm || (session.distancia_km ? String(session.distancia_km) : ''),
      duracaoRealMin: current.duracaoRealMin || (session.duracao_min ? String(session.duracao_min) : ''),
    }));
  }, [session?.id]);
  const alreadyRegistered = Boolean(session?.registro);
  const shouldShowMetrics = form.status !== 'PERDIDO';
  const parsedPace = parsePaceToSeconds(form.paceMedioReal);

  const errors = useMemo(() => {
    const found = {};
    if (form.status === 'PERDIDO') {
      if (!form.doente && !form.viagem && !form.observacao.trim()) {
        found.observacao = 'Conte por que o treino não aconteceu.';
      }
      return found;
    }
    if (!form.distanciaRealKm) {
      found.distanciaRealKm = 'Informe a distância que você correu.';
    }
    if (form.paceMedioReal && parsedPace == null) {
      found.paceMedioReal = 'Use o formato m:ss, por exemplo 5:30.';
    }
    if (form.sentiuDor && !form.localDor.trim()) {
      found.localDor = 'Diga onde foi a dor.';
    }
    return found;
  }, [form, parsedPace]);
  const visibleErrors = triedSubmit ? errors : {};
  const hasErrors = Object.keys(errors).length > 0;

  const mutation = useMutation({
    mutationFn: () => api.treinos.registrar(session.id, {
      status: form.status,
      distanciaRealKm: shouldShowMetrics && form.distanciaRealKm ? Number(form.distanciaRealKm) : null,
      duracaoRealMin: shouldShowMetrics && form.duracaoRealMin ? Number(form.duracaoRealMin) : null,
      fcMedia: shouldShowMetrics && form.fcMedia ? Number(form.fcMedia) : null,
      fcMaxima: shouldShowMetrics && form.fcMaxima ? Number(form.fcMaxima) : null,
      paceMedioReal: shouldShowMetrics ? parsedPace : null,
      esforcoPercebido: shouldShowMetrics && form.esforcoPercebido ? Number(form.esforcoPercebido) : null,
      sentiuDor: form.sentiuDor,
      localDor: form.sentiuDor ? form.localDor.trim() || null : null,
      doente: form.doente,
      viagem: form.viagem,
      observacao: form.observacao.trim() || null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
      queryClient.invalidateQueries({ queryKey: ['plano', planoId] });
      queryClient.invalidateQueries({ queryKey: ['progresso'] });
      queryClient.invalidateQueries({ queryKey: ['historico-treinos'] });
      navigate('/plano');
    },
  });

  if (isLoading || !session) {
    return <div className="min-h-screen flex items-center justify-center text-sm text-muted-foreground">Carregando treino...</div>;
  }

  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto px-5 pt-14 pb-10">
      <BackButton fallback="/plano" />

      <header className="mb-6">
        <h1 className="flex min-w-0 items-center gap-2 text-3xl font-bold text-foreground [overflow-wrap:anywhere]">
          <span className={`h-3 w-3 shrink-0 rounded-full ${WORKOUT_TYPE_COLORS[session.tipo] || WORKOUT_TYPE_COLORS.easy_run}`} aria-hidden="true" />
          {session.tipoLabel}
        </h1>
        <ul className="mt-3 space-y-1.5 text-sm text-muted-foreground">
          {session.dataPrevista && (
            <li className="flex items-center gap-2">
              <Calendar className="h-4 w-4" aria-hidden="true" />
              {new Date(`${session.dataPrevista}T00:00:00`).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}
            </li>
          )}
          <li className="flex items-center gap-2">
            <Flag className="h-4 w-4" aria-hidden="true" />
            Semana {session.semana} do plano
          </li>
          {alreadyRegistered && (
            <li className="flex items-center gap-2 text-primary">
              <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
              {READABLE_STATUS[session.registro?.status] || session.registro?.status}
            </li>
          )}
        </ul>
        {session.descricao && <p className="mt-3 text-sm text-foreground">{session.descricao}</p>}
      </header>

      <section className="mb-6 rounded-3xl border border-border bg-card p-5">
        <h2 className="mb-4 text-xs font-medium uppercase tracking-wide text-muted-foreground">{alreadyRegistered ? 'Seu treino' : 'Planejado'}</h2>
        <div className="grid grid-cols-[repeat(auto-fit,minmax(5.5rem,1fr))] gap-x-3 gap-y-5">
          {(alreadyRegistered ? doneStats(session) : plannedStats(session)).map((stat) => (
            <Stat key={stat.label} {...stat} />
          ))}
        </div>
        {alreadyRegistered && doneStats(session).length === 0 && (
          <p className="text-sm text-muted-foreground">Sem métricas registradas.</p>
        )}
      </section>

      {!alreadyRegistered && (
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
      )}

      {alreadyRegistered ? (
        (session.registro?.observacao || session.registro?.localDor) && (
          <section className="rounded-3xl border border-border bg-card p-5 space-y-3">
            {session.registro?.localDor && (
              <div>
                <p className="text-[0.625rem] font-medium uppercase tracking-wide text-muted-foreground">Dor</p>
                <p className="mt-1 text-sm text-foreground">{session.registro.localDor}</p>
              </div>
            )}
            {session.registro?.observacao && (
              <div>
                <p className="text-[0.625rem] font-medium uppercase tracking-wide text-muted-foreground">Observação</p>
                <p className="mt-1 text-sm text-foreground">{session.registro.observacao}</p>
              </div>
            )}
          </section>
        )
      ) : (
        <form onSubmit={(event) => { event.preventDefault(); setTriedSubmit(true); if (!hasErrors) mutation.mutate(); }} noValidate className="space-y-6">
          <section className="space-y-3">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Como foi o treino?</h2>
            <div className="space-y-2">
              {STATUS_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setForm((current) => ({ ...current, status: option.value }))}
                  className={`w-full rounded-2xl border p-4 text-left transition-transform active:scale-[0.98] ${form.status === option.value ? 'bg-primary/10 border-primary/40 ring-1 ring-primary/20' : 'bg-card border-border'}`}
                >
                  <p className={`text-sm font-semibold ${form.status === option.value ? 'text-primary' : 'text-foreground'}`}>{option.label}</p>
                  <p className="text-xs text-muted-foreground mt-1">{option.description}</p>
                </button>
              ))}
            </div>
          </section>

          {shouldShowMetrics && (
            <section className="space-y-3">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">O que você fez</h2>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Distância (km)" error={visibleErrors.distanciaRealKm}>
                  <Input type="number" inputMode="decimal" step="0.1" value={form.distanciaRealKm} onChange={(event) => setForm({ ...form, distanciaRealKm: event.target.value })} aria-invalid={Boolean(visibleErrors.distanciaRealKm)} className={`bg-card h-12 rounded-xl ${visibleErrors.distanciaRealKm ? 'border-destructive' : 'border-border'}`} />
                </Field>
                <Field label="Duração (min, opcional)">
                  <Input type="number" inputMode="numeric" value={form.duracaoRealMin} onChange={(event) => setForm({ ...form, duracaoRealMin: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                </Field>
                <Field label="Esforço de 0 a 10 (opcional)">
                  <Input type="number" inputMode="numeric" min="0" max="10" value={form.esforcoPercebido} onChange={(event) => setForm({ ...form, esforcoPercebido: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                </Field>
              </div>
              <details className="group rounded-2xl border border-border bg-card">
                <summary className="flex cursor-pointer list-none items-center justify-between px-4 py-3 text-sm font-medium text-foreground">
                  Mais detalhes (pace e frequência cardíaca)
                  <ChevronDown className="w-4 h-4 text-muted-foreground transition-transform group-open:rotate-180" />
                </summary>
                <div className="grid grid-cols-2 gap-3 px-4 pb-4">
                  <Field label="Pace médio (m:ss)" error={visibleErrors.paceMedioReal}>
                    <Input type="text" inputMode="numeric" placeholder="5:30" value={form.paceMedioReal} onChange={(event) => setForm({ ...form, paceMedioReal: event.target.value })} className={`bg-background h-12 rounded-xl ${visibleErrors.paceMedioReal ? 'border-destructive' : 'border-border'}`} />
                  </Field>
                  <Field label="FC média (bpm)">
                    <Input type="number" inputMode="numeric" value={form.fcMedia} onChange={(event) => setForm({ ...form, fcMedia: event.target.value })} className="bg-background border-border h-12 rounded-xl" />
                  </Field>
                  <Field label="FC máxima (bpm)">
                    <Input type="number" inputMode="numeric" value={form.fcMaxima} onChange={(event) => setForm({ ...form, fcMaxima: event.target.value })} className="bg-background border-border h-12 rounded-xl" />
                  </Field>
                </div>
              </details>
            </section>
          )}

          <section className="space-y-3">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Algo atrapalhou?</h2>
            <div className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div className="flex items-center space-x-3">
                <Checkbox id="sentiuDor" checked={form.sentiuDor} onCheckedChange={(checked) => setForm((current) => ({ ...current, sentiuDor: Boolean(checked), localDor: checked ? current.localDor : '' }))} />
                <Label htmlFor="sentiuDor" className="text-sm text-foreground">Senti dor durante ou depois do treino</Label>
              </div>
              {form.sentiuDor && (
                <Field label="Onde foi a dor?" error={visibleErrors.localDor}>
                  <Input placeholder="Ex.: joelho, panturrilha" value={form.localDor} onChange={(event) => setForm({ ...form, localDor: event.target.value })} className={`bg-background h-12 rounded-xl ${visibleErrors.localDor ? 'border-destructive' : 'border-border'}`} />
                </Field>
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
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">{form.status === 'PERDIDO' ? 'O que aconteceu?' : 'Observações (opcional)'}</h2>
            <Textarea
              placeholder="Ex.: reduzi o treino por dor, fiz na esteira, clima muito quente, viagem de trabalho..."
              value={form.observacao}
              onChange={(event) => setForm({ ...form, observacao: event.target.value })}
              className={`min-h-[120px] rounded-xl bg-card ${visibleErrors.observacao ? 'border-destructive' : 'border-border'}`}
            />
            {visibleErrors.observacao && <p className="text-xs text-destructive">{visibleErrors.observacao}</p>}
          </section>

          {mutation.error && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 flex items-start gap-3">
              <AlertTriangle className="w-4 h-4 text-destructive mt-0.5" />
              <p className="text-sm text-destructive">{mutation.error.message}</p>
            </div>
          )}

          <Button type="submit" disabled={mutation.isPending} className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-semibold gap-2 transition-transform active:scale-[0.98]">
            {mutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
            Registrar treino
          </Button>
        </form>
      )}
    </div>
  );
}
