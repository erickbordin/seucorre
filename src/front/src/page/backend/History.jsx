import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ArrowRight, CheckCircle2, Clock3, MapPin, XCircle } from 'lucide-react';
import { api } from '@/lib/api';

const STATUS_META = {
  concluido: { label: 'Concluído', className: 'bg-primary/10 text-primary', icon: CheckCircle2 },
  parcial: { label: 'Parcial', className: 'bg-orange-400/10 text-orange-400', icon: AlertTriangle },
  perdido: { label: 'Perdido', className: 'bg-destructive/10 text-destructive', icon: XCircle },
  pendente: { label: 'Pendente', className: 'bg-card text-muted-foreground', icon: Clock3 },
};

const formatPace = (seconds) => {
  if (!Number.isFinite(Number(seconds))) return null;
  const total = Number(seconds);
  const minutes = Math.floor(total / 60);
  const remainder = total % 60;
  return `${minutes}:${String(remainder).padStart(2, '0')} min/km`;
};

export default function History() {
  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ['historico-treinos'],
    queryFn: api.treinos.listarHistorico,
  });

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <p className="text-xs text-muted-foreground mb-1">Histórico de treinos</p>
          <h1 className="text-2xl font-bold text-foreground">Tudo o que já aconteceu no plano</h1>
        </div>
        <Link to="/plano" className="text-xs text-primary font-semibold inline-flex items-center gap-1 mt-1">
          Voltar ao plano
          <ArrowRight className="w-3 h-3" />
        </Link>
      </div>

      {isLoading ? (
        <div className="rounded-2xl bg-card border border-border p-6 text-sm text-muted-foreground">Carregando histórico...</div>
      ) : sessions.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-6 text-center">
          <p className="text-sm font-semibold text-foreground mb-1">Ainda não há treinos registrados</p>
          <p className="text-xs text-muted-foreground">Quando você concluir, perder ou registrar parcialmente uma sessão, ela aparece aqui.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {sessions.map((session, index) => {
            const meta = STATUS_META[session.status] || STATUS_META.pendente;
            const Icon = meta.icon;
            const healthAlert = Boolean(session.registro?.alertaSaude);
            return (
              <div key={session.id} className="rounded-2xl bg-card border border-border p-4">
                <div className="flex items-start justify-between gap-4 mb-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <span className={`px-2 py-1 rounded-full text-[10px] font-semibold ${meta.className}`}>{meta.label}</span>
                      <span className="px-2 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">{session.tipoLabel || session.tipoOriginal}</span>
                    </div>
                    <h2 className="text-sm font-semibold text-foreground">{session.titulo}</h2>
                    <p className="text-xs text-muted-foreground">{session.dataLabel || session.dataPrevista || `Sessão ${index + 1}`}</p>
                  </div>
                  <Icon className={`w-5 h-5 shrink-0 ${session.status === 'concluido' ? 'text-primary' : session.status === 'perdido' ? 'text-destructive' : 'text-orange-400'}`} />
                </div>

                <div className="flex flex-wrap gap-3 text-xs text-muted-foreground mb-3">
                  <span className="inline-flex items-center gap-1"><Clock3 className="w-3.5 h-3.5" /> {session.duracao_min || '-'} min</span>
                  <span className="inline-flex items-center gap-1"><MapPin className="w-3.5 h-3.5" /> {session.registro?.distanciaRealKm || session.distancia_km || 0} km</span>
                  {session.registro?.paceMedioReal && <span>{formatPace(session.registro.paceMedioReal)}</span>}
                </div>

                {(session.registro?.observacao || session.registro?.localDor) && (
                  <p className="text-xs text-muted-foreground mb-3">
                    {session.registro.observacao || `Dor relatada em ${session.registro.localDor}.`}
                  </p>
                )}

                <div className="flex flex-wrap gap-2">
                  {session.registro?.sentiuDor && <span className="px-2 py-1 rounded-full bg-orange-400/10 text-orange-400 text-[10px] font-semibold">Dor relatada</span>}
                  {session.registro?.doente && <span className="px-2 py-1 rounded-full bg-destructive/10 text-destructive text-[10px] font-semibold">Doente</span>}
                  {session.registro?.viagem && <span className="px-2 py-1 rounded-full bg-card border border-border text-[10px] font-semibold text-muted-foreground">Viagem</span>}
                  {healthAlert && <span className="px-2 py-1 rounded-full bg-destructive/10 text-destructive text-[10px] font-semibold">Alerta de saúde</span>}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
