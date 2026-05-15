import { useState } from 'react';
import { Link2, ShieldCheck, Watch } from 'lucide-react';
import { useAuth } from '@/lib/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const PLATFORMS = [
  { value: 'GARMIN', label: 'Garmin', helper: 'Importar treinos e FC automaticamente' },
  { value: 'POLAR', label: 'Polar', helper: 'Sincronizar sessões e métricas do relógio' },
  { value: 'APPLE_WATCH', label: 'Apple Watch', helper: 'Usar dados do Apple Health no plano' },
  { value: 'STRAVA', label: 'Strava', helper: 'Acompanhar histórico e consistência' },
];

export default function Wearables() {
  const { user } = useAuth();
  const [selectedPlatform, setSelectedPlatform] = useState(user?.dispositivos?.[0]?.plataforma || '');
  const [token, setToken] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const connectedPlatforms = user?.dispositivos || [];

  const handleConnect = () => {
    setSubmitted(true);
    // TODO: conectar endpoint/OAuth real para wearables quando a integração existir.
  };

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
            <p className="text-xs text-muted-foreground">A integração reduz digitação manual e melhora o ajuste de volume, pace e alertas de saúde.</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {connectedPlatforms.length > 0 ? connectedPlatforms.map((item) => (
            <span key={item.plataforma} className="px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">
              {item.plataforma} {item.tokenValido ? 'conectado' : 'reconectar'}
            </span>
          )) : (
            <span className="px-3 py-1 rounded-full bg-card border border-border text-[10px] font-semibold text-muted-foreground">
              Nenhum wearable conectado
            </span>
          )}
        </div>
      </section>

      <section className="space-y-3 mb-6">
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Plataformas</h2>
        {PLATFORMS.map((platform) => (
          <button
            key={platform.value}
            type="button"
            onClick={() => setSelectedPlatform(platform.value)}
            className={`w-full rounded-2xl border p-4 text-left ${selectedPlatform === platform.value ? 'bg-primary/10 border-primary/30' : 'bg-card border-border'}`}
          >
            <div className="flex items-start gap-3">
              <Link2 className={`w-4 h-4 mt-0.5 ${selectedPlatform === platform.value ? 'text-primary' : 'text-muted-foreground'}`} />
              <div>
                <p className="text-sm font-semibold text-foreground">{platform.label}</p>
                <p className="text-xs text-muted-foreground">{platform.helper}</p>
              </div>
            </div>
          </button>
        ))}
      </section>

      <section className="rounded-3xl bg-card border border-border p-5 mb-6">
        <div className="flex items-start gap-3 mb-4">
          <ShieldCheck className="w-5 h-5 text-primary mt-0.5" />
          <div>
            <h2 className="text-sm font-semibold text-foreground">Preparar conexão</h2>
            <p className="text-xs text-muted-foreground">Este fluxo já mostra a plataforma e o estado da conta. A autenticação real entra quando o backend expor OAuth.</p>
          </div>
        </div>
        <div className="space-y-3">
          <Input value={selectedPlatform} readOnly className="bg-background border-border h-12 rounded-xl" />
          <Input
            value={token}
            onChange={(event) => setToken(event.target.value)}
            placeholder="Token temporário / identificador de conexão"
            className="bg-background border-border h-12 rounded-xl"
          />
        </div>
      </section>

      <Button onClick={handleConnect} className="w-full h-14 rounded-2xl font-semibold text-base mb-4">
        Simular conexão
      </Button>

      {submitted && (
        <div className="rounded-2xl bg-card border border-border p-4">
          <p className="text-sm font-semibold text-foreground mb-1">Integração ainda pendente no backend</p>
          <p className="text-xs text-muted-foreground">
            A tela já está pronta para o fluxo. Falta apenas o endpoint real de OAuth para trocar este envio manual por autorização segura.
          </p>
        </div>
      )}
    </div>
  );
}
