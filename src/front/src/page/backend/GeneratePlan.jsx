import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Brain, CheckCircle2, Loader2, RefreshCw, Sparkles, Zap } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';

const steps = [
  { icon: Brain, label: 'Lendo seu perfil...' },
  { icon: Zap, label: 'Calculando o volume de cada semana...' },
  { icon: Sparkles, label: 'Montando sua rotina inicial...' },
  { icon: CheckCircle2, label: 'Plano criado.' },
];

export default function GeneratePlan() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasCompletedOnboarding, user } = useAuth();
  const [currentStep, setCurrentStep] = useState(-1);
  const [error, setError] = useState('');
  const [generating, setGenerating] = useState(false);
  const requestInFlight = useRef(false);

  useEffect(() => {
    if (!hasCompletedOnboarding || !user?.objetivo) {
      navigate('/onboarding', { replace: true });
    }
  }, [hasCompletedOnboarding, navigate, user?.objetivo]);

  const generate = async () => {
    if (requestInFlight.current) return;
    requestInFlight.current = true;
    setError('');
    setGenerating(true);
    setCurrentStep(0);
    try {
      await new Promise((resolve) => setTimeout(resolve, 500));
      setCurrentStep(1);
      const plan = await api.planos.gerar(user.objetivo);
      setCurrentStep(2);
      queryClient.setQueryData(['meus-planos'], (current = []) => {
        const otherPlans = (current || []).filter((item) => item.id !== plan.id);
        return [plan, ...otherPlans];
      });
      queryClient.setQueryData(['plano', plan.id], plan);
      queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
      setCurrentStep(3);
      setTimeout(() => navigate('/plano', { replace: true }), 700);
    } catch (err) {
      setError(err.message || 'Não foi possível gerar o plano.');
      setGenerating(false);
      requestInFlight.current = false;
    }
  };

  if (!generating && !error) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center px-8 max-w-lg mx-auto text-center">
        <div className="w-24 h-24 rounded-full bg-primary/15 flex items-center justify-center mb-10">
          <Sparkles className="w-8 h-8 text-primary" />
        </div>
        <h1 className="text-2xl font-bold text-foreground mb-3">Gerar seu plano de treino</h1>
        <p className="text-sm text-muted-foreground mb-8">Se você já tem um plano, ele será substituído pelo novo.</p>
        <button onClick={generate} className="w-full h-12 rounded-2xl bg-primary text-primary-foreground font-semibold inline-flex items-center justify-center gap-2">
          <Sparkles className="w-4 h-4" />
          Gerar meu plano
        </button>
        <button onClick={() => navigate('/plano')} className="mt-4 text-sm text-primary">
          Voltar para o plano
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-8 max-w-lg mx-auto">
      <div className="w-24 h-24 rounded-full bg-primary/15 flex items-center justify-center mb-10">
        {error ? <Sparkles className="w-8 h-8 text-destructive" /> : <Loader2 className="w-8 h-8 text-primary animate-spin" />}
      </div>
      <div className="space-y-4 w-full">
        {steps.map((step, index) => {
          const Icon = step.icon;
          const active = index <= currentStep;
          return (
            <div key={step.label} className={`flex items-center gap-3 px-4 py-3 rounded-xl border ${active ? 'bg-primary/10 border-primary/20' : 'bg-card/50 border-border/50'}`}>
              <Icon className={`w-5 h-5 ${active ? 'text-primary' : 'text-muted-foreground'}`} />
              <span className={`text-sm font-medium ${active ? 'text-foreground' : 'text-muted-foreground'}`}>{step.label}</span>
            </div>
          );
        })}
      </div>
      {error && (
        <div className="mt-8 text-center w-full rounded-2xl border border-destructive/20 bg-card p-5">
          <p className="text-sm font-semibold text-foreground mb-2">Não foi possível gerar seu plano agora</p>
          <p className="text-sm text-destructive mb-4">{error}</p>
          <p className="text-xs text-muted-foreground mb-5">
            Revise os dados do seu perfil ou tente novamente. Se a geração falhar, o objetivo atual continua sendo {user?.objetivo || 'o informado no seu onboarding'}.
          </p>
          <div className="flex flex-col gap-3">
            <button onClick={generate} className="w-full h-12 rounded-2xl bg-primary text-primary-foreground font-semibold inline-flex items-center justify-center gap-2">
              <RefreshCw className="w-4 h-4" />
              Tentar novamente
            </button>
            <Link to="/onboarding" className="w-full h-12 rounded-2xl border border-border bg-card font-semibold text-foreground inline-flex items-center justify-center">
              Revisar perfil
            </Link>
            <button onClick={() => navigate('/plano')} className="text-sm text-primary">
              Voltar para o plano
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
