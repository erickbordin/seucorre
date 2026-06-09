import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, Link2, Loader2, RefreshCw, ShieldCheck, Watch } from 'lucide-react';
import { useAuth } from '@/lib/AuthContext';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const PLATFORMS = [
  { value: 'GARMIN', label: 'Garmin', helper: 'Importar treinos, FC e métricas de execução.', available: true },
  { value: 'STRAVA', label: 'Strava', helper: 'Aproveitar histórico recente para atualizar o plano.', available: true },
  { value: 'POLAR', label: 'Polar', helper: 'Sincronização planejada para uma próxima entrega.', available: false },
  { value: 'APPLE_WATCH', label: 'Apple Watch', helper: 'Integração futura via ecossistema Apple Health.', available: false },
];

const AVAILABLE_PLATFORM = PLATFORMS.find((platform) => platform.available)?.value || 'GARMIN';

const createDefaultExpiry = () => {
  const date = new Date();
  date.setDate(date.getDate() + 30);
  date.setSeconds(0, 0);
  const timezoneOffset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 16);
};

const formatExpiry = (value) => {
  if (!value) return 'Sem validade informada';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return 'Sem validade informada';
  return parsed.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function Wearables() {
  const { user, reloadUser } = useAuth();
  const queryClient = useQueryClient();
  const [selectedPlatform, setSelectedPlatform] = useState(user?.dispositivos?.[0]?.plataforma || AVAILABLE_PLATFORM);
  const [token, setToken] = useState('');
  const [tokenExpiresAt, setTokenExpiresAt] = useState(createDefaultExpiry);
  const [feedback, setFeedback] = useState(null);

  const { data: dispositivos = [], isLoading, error } = useQuery({
    queryKey: ['dispositivos'],
    queryFn: api.dispositivos.listar,
  });

  const connectedByPlatform = useMemo(
    () => Object.fromEntries(dispositivos.map((dispositivo) => [dispositivo.plataforma, dispositivo])),
    [dispositivos],
  );

  const selectedMeta = PLATFORMS.find((platform) => platform.value === selectedPlatform) || PLATFORMS[0];
  const selectedDevice = connectedByPlatform[selectedPlatform];

  const connectMutation = useMutation({
    mutationFn: () => api.dispositivos.conectar({
      plataforma: selectedPlatform,
      tokenAcesso: token.trim(),
      tokenExpiresAt,
    }),
    onSuccess: async (response) => {
      await queryClient.invalidateQueries({ queryKey: ['dispositivos'] });
      await reloadUser();
      setToken('');
      setFeedback({
        type: 'success',
        title: `${response.descricaoPlataforma || selectedMeta.label} conectado`,
        description: 'O dispositivo já pode sincronizar atividades com o SeuCorre.',
      });
    },
    onError: (mutationError) => {
      setFeedback({
        type: 'error',
        title: 'Não foi possível conectar o dispositivo',
        description: mutationError.message,
      });
    },
  });

  const syncMutation = useMutation({
    mutationFn: (plataforma) => api.dispositivos.sincronizar(plataforma),
    onSuccess: async (response) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dispositivos'] }),
        queryClient.invalidateQueries({ queryKey: ['historico-treinos'] }),
        queryClient.invalidateQueries({ queryKey: ['meus-planos'] }),
        queryClient.invalidateQueries({ queryKey: ['progresso'] }),
      ]);
      const imported = Number(response?.registrosImportados || 0);
      setFeedback({
        type: 'success',
        title: imported > 0 ? `${imported} atividade${imported > 1 ? 's' : ''} importada${imported > 1 ? 's' : ''}` : 'Nenhuma atividade nova encontrada',
        description: imported > 0
          ? 'Os treinos importados já entraram no histórico e no cálculo de progresso.'
          : 'A conta está vinculada, mas não houve atividade pendente para importar agora.',
      });
    },
    onError: (mutationError) => {
      setFeedback({
        type: 'error',
        title: 'Não foi possível sincronizar agora',
        description: mutationError.message,
      });
    },
  });

  const handleSelectPlatform = (platform) => {
    setSelectedPlatform(platform.value);
    setFeedback(null);
  };

  const handleConnect = (event) => {
    event.preventDefault();
    setFeedback(null);
    connectMutation.mutate();
  };

  const canConnect = Boolean(selectedPlatform && selectedMeta?.available && token.trim() && tokenExpiresAt);

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="mb-6">
        <p className="text-xs text-muted-foreground mb-1">Wearables</p>
        <h1 className="text-2xl font-bold text-foreground">Conecte suas fontes de treino</h1>
      </div>

      <section className="rounded-3xl bg-card border border-border p-5 mb-6">
        <div className="flex items-start gap-3 mb-3">
          <Watch className="w-5 h-5 text-primary mt-0.5" />
          <div>
            <h2 className="text-sm font-semibold text-foreground">Por que conectar?</h2>
            <p className="text-xs text-muted-foreground">A sincronização reduz digitação manual e mantém volume, pace e sinais de recuperação mais próximos do que aconteceu de verdade.</p>
          </div>
        </div>

        {isLoading ? (
          <div className="rounded-2xl border border-border bg-background px-4 py-5 text-sm text-muted-foreground">
            Carregando dispositivos conectados...
          </div>
        ) : dispositivos.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {dispositivos.map((item) => (
              <span key={item.plataforma} className="px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">
                {item.plataforma} {item.tokenValido ? 'conectado' : 'reconectar'}
              </span>
            ))}
          </div>
        ) : (
          <span className="px-3 py-1 rounded-full bg-card border border-border text-[10px] font-semibold text-muted-foreground">
            Nenhum wearable conectado
          </span>
        )}
      </section>

      <section className="space-y-3 mb-6">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Plataformas</h2>
        {PLATFORMS.map((platform) => {
          const connectedDevice = connectedByPlatform[platform.value];
          const isSelected = selectedPlatform === platform.value;
          const isSyncing = syncMutation.isPending && syncMutation.variables === platform.value;
          return (
            <div
              key={platform.value}
              className={`rounded-2xl border p-4 ${isSelected ? 'bg-primary/10 border-primary/30' : 'bg-card border-border'}`}
            >
              <div className="flex items-start gap-3">
                <button type="button" onClick={() => handleSelectPlatform(platform)} className="flex-1 text-left min-w-0">
                  <div className="flex items-start gap-3">
                    <Link2 className={`w-4 h-4 mt-0.5 ${isSelected ? 'text-primary' : 'text-muted-foreground'}`} />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2 mb-1">
                        <p className="text-sm font-semibold text-foreground">{platform.label}</p>
                        <span className={`px-2 py-1 rounded-full text-[10px] font-semibold ${platform.available ? 'bg-primary/10 text-primary' : 'bg-card border border-border text-muted-foreground'}`}>
                          {platform.available ? 'Disponível agora' : 'Em breve'}
                        </span>
                        {connectedDevice && (
                          <span className={`px-2 py-1 rounded-full text-[10px] font-semibold ${connectedDevice.tokenValido ? 'bg-primary/10 text-primary' : 'bg-orange-400/10 text-orange-400'}`}>
                            {connectedDevice.tokenValido ? 'Conectado' : 'Token expirado'}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-muted-foreground">{platform.helper}</p>
                      {connectedDevice && (
                        <p className="text-[11px] text-muted-foreground mt-2">
                          Token válido até {formatExpiry(connectedDevice.tokenExpiresAt)}.
                        </p>
                      )}
                    </div>
                  </div>
                </button>

                {platform.available && connectedDevice && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="rounded-xl shrink-0"
                    disabled={isSyncing}
                    onClick={() => {
                      setFeedback(null);
                      syncMutation.mutate(platform.value);
                    }}
                  >
                    {isSyncing ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
                    Sincronizar
                  </Button>
                )}
              </div>
            </div>
          );
        })}
      </section>

      <form onSubmit={handleConnect} className="rounded-3xl bg-card border border-border p-5 mb-6">
        <div className="flex items-start gap-3 mb-4">
          <ShieldCheck className="w-5 h-5 text-primary mt-0.5" />
          <div>
            <h2 className="text-sm font-semibold text-foreground">Vincular dispositivo</h2>
            <p className="text-xs text-muted-foreground">Cole o token de acesso da plataforma escolhida para salvar a conexão e liberar a sincronização.</p>
          </div>
        </div>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="wearable-platform">Plataforma selecionada</Label>
            <Input id="wearable-platform" value={selectedMeta?.label || ''} readOnly className="bg-background border-border h-12 rounded-xl" />
          </div>

          <div className="space-y-2">
            <Label htmlFor="wearable-token">Token de acesso</Label>
            <Input
              id="wearable-token"
              value={token}
              onChange={(event) => setToken(event.target.value)}
              placeholder="Cole o token emitido pela plataforma"
              className="bg-background border-border h-12 rounded-xl"
              disabled={!selectedMeta?.available}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="wearable-expiry">Validade do token</Label>
            <Input
              id="wearable-expiry"
              type="datetime-local"
              value={tokenExpiresAt}
              onChange={(event) => setTokenExpiresAt(event.target.value)}
              className="bg-background border-border h-12 rounded-xl"
              disabled={!selectedMeta?.available}
            />
          </div>
        </div>

        {!selectedMeta?.available && (
          <div className="rounded-2xl border border-dashed border-border px-4 py-4 mt-4 text-xs text-muted-foreground">
            {selectedMeta?.label} ainda não tem integração ativa no backend desta versão.
          </div>
        )}

        <Button type="submit" disabled={!canConnect || connectMutation.isPending} className="w-full h-14 rounded-2xl font-semibold text-base mt-5">
          {connectMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
          {selectedDevice ? 'Atualizar conexão' : 'Conectar dispositivo'}
        </Button>
      </form>

      {(feedback || error) && (
        <div className={`rounded-2xl border p-4 ${feedback?.type === 'error' || error ? 'bg-destructive/5 border-destructive/30' : 'bg-card border-border'}`}>
          <div className="flex items-start gap-3">
            {feedback?.type === 'error' || error ? (
              <AlertTriangle className="w-5 h-5 text-destructive mt-0.5" />
            ) : (
              <CheckCircle2 className="w-5 h-5 text-primary mt-0.5" />
            )}
            <div>
              <p className="text-sm font-semibold text-foreground mb-1">{feedback?.title || 'Não foi possível carregar os dispositivos'}</p>
              <p className="text-xs text-muted-foreground">{feedback?.description || error?.message}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
