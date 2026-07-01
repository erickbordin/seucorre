import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Check, Crown, Sparkles, Zap, Brain, Shield } from 'lucide-react';
import { Button } from '@/components/ui/button';

const FREE_FEATURES = [
  'Plano básico de 4 semanas',
  '1 geração de plano com IA',
  'Acompanhamento de progresso',
  'Coach IA (3 mensagens/dia)',
];

const PREMIUM_FEATURES = [
  'Planos ilimitados com IA',
  'Ajustes dinâmicos semanais',
  'Coach IA ilimitado',
  'Zonas de FC personalizadas',
  'Feedback pós-corrida com IA',
  'Integração com wearables',
  'Reescrita automática de plano',
  'Suporte prioritário',
];

export default function Paywall() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto px-5 pt-14 pb-8">
      <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
        <ArrowLeft className="w-4 h-4" />
        Voltar
      </button>

      {/* Hero */}
      <div className="text-center mb-8">
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="w-20 h-20 rounded-3xl bg-gradient-to-br from-primary/30 to-yellow-400/20 flex items-center justify-center mx-auto mb-4"
        >
          <Crown className="w-10 h-10 text-primary" />
        </motion.div>
        <h1 className="text-2xl font-bold text-foreground mb-2">SeuCorre Premium</h1>
        <p className="text-sm text-muted-foreground">Desbloqueie todo o poder da IA para suas corridas</p>
      </div>

      <div className="bg-card rounded-2xl p-4 border border-border/50 mb-8">
        <p className="text-xs text-muted-foreground mb-1">Indicado para quem quer evoluir com menos tentativa e erro</p>
        <p className="text-sm text-foreground">
          O plano free cobre a base. O premium entra quando você quer check-ins adaptativos, integração com wearable e reescrita automática do ciclo com base na sua semana real.
        </p>
        <p className="text-xs text-muted-foreground mt-3">Nesta versão, a proposta comercial já está desenhada, mas o checkout e o backend de billing ainda não estão conectados.</p>
      </div>

      {/* Benefits icons */}
      <div className="grid grid-cols-3 gap-3 mb-8">
        {[
          { icon: Brain, label: 'IA Ilimitada', color: 'text-purple-400', bg: 'bg-purple-400/10' },
          { icon: Zap, label: 'Ajuste Dinâmico', color: 'text-orange-400', bg: 'bg-orange-400/10' },
          { icon: Shield, label: 'Proteção Saúde', color: 'text-green-400', bg: 'bg-green-400/10' },
        ].map((b, i) => {
          const Icon = b.icon;
          return (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              className="bg-card rounded-2xl p-4 border border-border/50 text-center"
            >
              <div className={`w-10 h-10 rounded-xl ${b.bg} flex items-center justify-center mx-auto mb-2`}>
                <Icon className={`w-5 h-5 ${b.color}`} />
              </div>
              <p className="text-[10px] text-muted-foreground">{b.label}</p>
            </motion.div>
          );
        })}
      </div>

      {/* Plan comparison */}
      <div className="space-y-4 mb-8">
        {/* Free */}
        <div className="bg-card rounded-2xl p-5 border border-border/50">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-xs text-muted-foreground">Plano Atual</p>
              <p className="text-lg font-bold text-foreground">Free</p>
            </div>
            <p className="text-2xl font-bold text-foreground">R$ 0</p>
          </div>
          <div className="space-y-2">
            {FREE_FEATURES.map((f, i) => (
              <div key={i} className="flex items-center gap-2">
                <Check className="w-3.5 h-3.5 text-muted-foreground" />
                <span className="text-xs text-muted-foreground">{f}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Premium */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-br from-primary/10 via-card to-card rounded-2xl p-5 border-2 border-primary/40 relative overflow-hidden"
        >
          <div className="absolute top-0 right-0 bg-primary text-primary-foreground text-[10px] font-bold px-3 py-1 rounded-bl-xl">
            RECOMENDADO
          </div>
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-xs text-primary">Upgrade para</p>
              <p className="text-lg font-bold text-foreground">Premium</p>
            </div>
            <div className="text-right">
              <p className="text-2xl font-bold text-foreground">R$ 29<span className="text-sm font-normal text-muted-foreground">/mês</span></p>
              <p className="text-[10px] text-muted-foreground">ou R$ 249/ano</p>
            </div>
          </div>
          <div className="space-y-2 mb-5">
            {PREMIUM_FEATURES.map((f, i) => (
              <div key={i} className="flex items-center gap-2">
                <Check className="w-3.5 h-3.5 text-primary" />
                <span className="text-xs text-foreground">{f}</span>
              </div>
            ))}
          </div>
          <Button disabled className="w-full h-13 rounded-2xl bg-primary text-primary-foreground font-semibold text-sm gap-2">
            <Sparkles className="w-4 h-4" />
            Premium em breve
          </Button>
          <p className="text-[10px] text-muted-foreground mt-3">O fluxo de assinatura fica habilitado depois que billing, entitlement e webhook estiverem prontos no backend.</p>
        </motion.div>
      </div>

      <Button variant="outline" className="w-full h-12 rounded-2xl mb-4" onClick={() => navigate('/')}>
        Continuar no plano free
      </Button>

      <p className="text-[10px] text-muted-foreground text-center">
        Cancele a qualquer momento. Sem compromisso.
      </p>
    </div>
  );
}
