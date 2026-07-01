import { useState } from 'react';
import { Bell, CalendarClock, HeartPulse, MoonStar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';

const DEFAULT_SETTINGS = {
  treinoHoje: true,
  checkinSemanal: true,
  riscoElevado: true,
  progressoSemanal: false,
};

export default function Notifications() {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [saved, setSaved] = useState(false);

  const updateSetting = (key, checked) => {
    setSaved(false);
    setSettings((current) => ({ ...current, [key]: checked }));
  };

  const handleSave = () => {
    setSaved(true);
    // TODO: conectar endpoint real de preferências de notificações quando existir no backend.
  };

  const items = [
    {
      key: 'treinoHoje',
      icon: CalendarClock,
      title: 'Lembrete de treino do dia',
      description: 'Avise quando houver sessão planejada para hoje.',
    },
    {
      key: 'checkinSemanal',
      icon: Bell,
      title: 'Check-in semanal',
      description: 'Lembrar de avaliar esforço, dor e sono antes da próxima semana.',
    },
    {
      key: 'riscoElevado',
      icon: HeartPulse,
      title: 'Alertas de risco',
      description: 'Notificar quando a IA identificar sobrecarga ou recuperação ruim.',
    },
    {
      key: 'progressoSemanal',
      icon: MoonStar,
      title: 'Resumo de progresso',
      description: 'Receber um resumo da semana com adesão e volume total.',
    },
  ];

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="mb-6">
        <p className="text-xs text-muted-foreground mb-1">Notificações</p>
        <h1 className="text-2xl font-bold text-foreground">Escolha o que realmente vale te interromper</h1>
      </div>

      <section className="rounded-3xl bg-card border border-border p-5 mb-6">
        <p className="text-sm text-foreground">
          O objetivo aqui é manter contexto sem virar ruído: próximo treino, check-in, alerta de risco e resumo do que mudou na semana.
        </p>
      </section>

      <div className="space-y-3 mb-6">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <div key={item.key} className="rounded-2xl bg-card border border-border p-4">
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center shrink-0">
                  <Icon className="w-4 h-4 text-primary" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between gap-4 mb-1">
                    <p className="text-sm font-semibold text-foreground">{item.title}</p>
                    <Switch checked={settings[item.key]} onCheckedChange={(checked) => updateSetting(item.key, checked)} />
                  </div>
                  <p className="text-xs text-muted-foreground">{item.description}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <Button onClick={handleSave} className="w-full h-14 rounded-2xl font-semibold text-base mb-4">
        Testar preferências nesta tela
      </Button>

      {saved && (
        <div className="rounded-2xl bg-card border border-border p-4">
          <p className="text-sm font-semibold text-foreground mb-1">Preferências simuladas no frontend</p>
          <p className="text-xs text-muted-foreground">Esse estado ainda nao e persistido: falta o endpoint de configuracoes de notificacoes no backend.</p>
        </div>
      )}
    </div>
  );
}
