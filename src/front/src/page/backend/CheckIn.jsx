import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, Brain, CheckCircle2, ClipboardList, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

const riskStyles = {
  BAIXO: 'bg-primary/10 text-primary',
  MODERADO: 'bg-orange-400/10 text-orange-400',
  ALTO: 'bg-destructive/10 text-destructive',
  CRITICO: 'bg-destructive/10 text-destructive',
};

const buildScale = (label, value, onChange, helper) => (
  <section className="space-y-3">
    <div className="flex items-center justify-between gap-4">
      <div>
        <h2 className="text-sm font-semibold text-foreground">{label}</h2>
        <p className="text-xs text-muted-foreground">{helper}</p>
      </div>
      <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold">{value}</span>
    </div>
    <div className="grid grid-cols-6 gap-2">
      {Array.from({ length: 11 }, (_, index) => index).map((option) => (
        <button
          key={`${label}-${option}`}
          type="button"
          onClick={() => onChange(option)}
          className={`h-11 rounded-xl border text-sm font-semibold ${value === option ? 'bg-primary text-primary-foreground border-primary' : 'bg-card border-border text-foreground'}`}
        >
          {option}
        </button>
      ))}
    </div>
  </section>
);

export default function CheckIn() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [form, setForm] = useState({
    nivelEsforco: 5,
    nivelDor: 0,
    horasSonoSemana: 7,
    avaliacao: '',
  });

  const { data: plans = [], isLoading: isLoadingPlans } = useQuery({
    queryKey: ['meus-planos'],
    queryFn: api.planos.listarMeus,
  });
  const { data: history = [], isLoading: isLoadingHistory } = useQuery({
    queryKey: ['checkins', 'historico'],
    queryFn: api.checkins.historico,
  });

  const activePlan = plans.find((item) => item.status === 'ativo') || plans[0];
  const currentWeek = activePlan?.semana_atual || 1;
  const latestCheckin = history[0] || null;
  const latestWeekCheckin = useMemo(
    () => history.find((item) => Number(item.semana) === Number(currentWeek)) || null,
    [currentWeek, history],
  );

  const mutation = useMutation({
    mutationFn: () => api.checkins.enviar({
      usuarioId: user.id,
      planoId: activePlan.id,
      semana: currentWeek,
      dataCheckin: new Date().toISOString().slice(0, 10),
      nivelEsforco: Number(form.nivelEsforco),
      nivelDor: Number(form.nivelDor),
      horasSonoSemana: Number(form.horasSonoSemana),
      avaliacao: form.avaliacao.trim() || null,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checkins', 'historico'] });
      queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
    },
  });

  const disabled = !activePlan || mutation.isPending;

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="mb-6">
        <p className="text-xs text-muted-foreground mb-1">Check-in semanal</p>
        <h1 className="text-2xl font-bold text-foreground">Como seu corpo respondeu nesta semana?</h1>
      </div>

      {isLoadingPlans ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-sm text-muted-foreground">Carregando plano ativo...</div>
      ) : !activePlan ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-center">
          <p className="text-sm font-semibold text-foreground mb-2">Nenhum plano ativo para avaliar</p>
          <p className="text-xs text-muted-foreground mb-4">Gere um plano antes de enviar check-ins para a IA.</p>
          <Link to="/gerar-plano" className="inline-flex items-center gap-2 bg-primary text-primary-foreground rounded-xl px-4 py-3 text-sm font-semibold">
            Gerar plano
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      ) : (
        <>
          <section className="rounded-3xl bg-card border border-border p-5 mb-6">
            <div className="flex items-start justify-between gap-4 mb-3">
              <div>
                <p className="text-xs text-muted-foreground mb-1">Contexto atual</p>
                <h2 className="text-lg font-bold text-foreground">Semana {currentWeek} de {activePlan.total_semanas}</h2>
              </div>
              {latestWeekCheckin && (
                <span className={`px-3 py-1 rounded-full text-[10px] font-semibold ${riskStyles[latestWeekCheckin.nivelRisco] || 'bg-primary/10 text-primary'}`}>
                  {latestWeekCheckin.nivelRiscoLabel || latestWeekCheckin.nivelRisco}
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              O check-in ajuda a IA a decidir se a carga está segura ou se o plano precisa ser reescrito.
            </p>
          </section>

          <div className="space-y-6 mb-6">
            {buildScale('Esforço da semana', form.nivelEsforco, (value) => setForm((current) => ({ ...current, nivelEsforco: value })), '0 = muito leve, 10 = exaustivo')}
            {buildScale('Dor ou desconforto', form.nivelDor, (value) => setForm((current) => ({ ...current, nivelDor: value })), '0 = sem dor, 10 = dor muito forte')}
            <section className="space-y-3">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <h2 className="text-sm font-semibold text-foreground">Sono médio por noite</h2>
                  <p className="text-xs text-muted-foreground">Informe quantas horas, em média, você dormiu nesta semana.</p>
                </div>
                <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold">{form.horasSonoSemana} h</span>
              </div>
              <div className="grid grid-cols-6 gap-2">
                {Array.from({ length: 13 }, (_, index) => index).filter((item) => item >= 4).map((option) => (
                  <button
                    key={`sono-${option}`}
                    type="button"
                    onClick={() => setForm((current) => ({ ...current, horasSonoSemana: option }))}
                    className={`h-11 rounded-xl border text-sm font-semibold ${form.horasSonoSemana === option ? 'bg-primary text-primary-foreground border-primary' : 'bg-card border-border text-foreground'}`}
                  >
                    {option}
                  </button>
                ))}
              </div>
            </section>

            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-foreground">Resumo subjetivo</h2>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <Textarea
                value={form.avaliacao}
                onChange={(event) => setForm((current) => ({ ...current, avaliacao: event.target.value }))}
                placeholder="Ex.: senti as pernas pesadas no longo, dormi mal em dois dias, recuperei melhor no fim da semana..."
                className="min-h-[120px] rounded-2xl border-border bg-card"
              />
            </section>
          </div>

          <Button disabled={disabled} onClick={() => mutation.mutate()} className="w-full h-14 rounded-2xl font-semibold text-base mb-6">
            {mutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <ClipboardList className="w-5 h-5" />}
            Enviar check-in
          </Button>

          {mutation.data && (
            <section className="rounded-3xl bg-card border border-border p-5 mb-6">
              <div className="flex items-start justify-between gap-4 mb-3">
                <div className="flex items-start gap-3">
                  <Brain className="w-5 h-5 text-primary mt-0.5" />
                  <div>
                    <h2 className="text-sm font-semibold text-foreground">Análise da IA</h2>
                    <p className="text-xs text-muted-foreground">Semana {mutation.data.semana} • {mutation.data.dataCheckin}</p>
                  </div>
                </div>
                <span className={`px-3 py-1 rounded-full text-[10px] font-semibold ${riskStyles[mutation.data.nivelRisco] || 'bg-primary/10 text-primary'}`}>
                  {mutation.data.nivelRiscoLabel || mutation.data.nivelRisco}
                </span>
              </div>
              <div className="space-y-2 text-xs text-muted-foreground mb-4">
                <p>{mutation.data.analiseIA || 'A análise detalhada ainda não foi retornada pela IA.'}</p>
                {mutation.data.precisaReescreverPlano && (
                  <p className="text-foreground font-medium">A IA sinalizou necessidade de reescrever o plano.</p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {mutation.data.sobrecarga && <span className="px-3 py-1 rounded-full bg-orange-400/10 text-orange-400 text-[10px] font-semibold">Sobrecarga detectada</span>}
                {mutation.data.recuperacaoInsuficiente && <span className="px-3 py-1 rounded-full bg-destructive/10 text-destructive text-[10px] font-semibold">Recuperação insuficiente</span>}
                {mutation.data.planoReescrito && <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">Plano reescrito</span>}
              </div>
            </section>
          )}

          {mutation.isError && (
            <div className="rounded-2xl border border-destructive/20 bg-card p-4 mb-6">
              <div className="flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 text-destructive mt-0.5" />
                <div>
                  <p className="text-sm font-semibold text-foreground mb-1">Não foi possível registrar o check-in</p>
                  <p className="text-xs text-muted-foreground">{mutation.error.message}</p>
                </div>
              </div>
            </div>
          )}

          <section className="space-y-3">
            <div className="flex items-center justify-between gap-4">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Histórico recente</h2>
              <Link to="/plano" className="text-xs text-primary font-semibold inline-flex items-center gap-1">
                Ver plano
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>
            {isLoadingHistory ? (
              <div className="rounded-2xl bg-card border border-border p-5 text-sm text-muted-foreground">Carregando histórico...</div>
            ) : history.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-border p-6 text-center">
                <p className="text-sm font-semibold text-foreground mb-1">Ainda sem check-ins enviados</p>
                <p className="text-xs text-muted-foreground">Seu primeiro envio vai aparecer aqui com a análise da IA.</p>
              </div>
            ) : history.slice(0, 4).map((item) => (
              <div key={item.checkinId} className="rounded-2xl bg-card border border-border p-4">
                <div className="flex items-start justify-between gap-4 mb-2">
                  <div>
                    <p className="text-sm font-semibold text-foreground">Semana {item.semana}</p>
                    <p className="text-xs text-muted-foreground">{item.dataCheckin}</p>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-[10px] font-semibold ${riskStyles[item.nivelRisco] || 'bg-primary/10 text-primary'}`}>
                    {item.nivelRiscoLabel || item.nivelRisco}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground mb-3">{item.analiseIA || item.avaliacao || 'Sem comentário adicional nesta semana.'}</p>
                <div className="flex flex-wrap gap-2">
                  {item.planoReescrito && <span className="px-2 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold inline-flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> Plano reescrito</span>}
                </div>
              </div>
            ))}
          </section>
        </>
      )}
    </div>
  );
}
