import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Brain, CheckCircle2, Loader2, RefreshCw, Sparkles, Zap } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';

const steps = [
  { icon: Brain, label: 'Lendo seu perfil no backend...' },
  { icon: Zap, label: 'Chamando /api/planos/gerar...' },
  { icon: Sparkles, label: 'Aguardando geração pela IA...' },
  { icon: CheckCircle2, label: 'Plano criado.' },
];

export default function GeneratePlan() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasCompletedOnboarding, user } = useAuth();
  const [currentStep, setCurrentStep] = useState(0);
  const [error, setError] = useState('');
  const [attemptKey, setAttemptKey] = useState(0);

  useEffect(() => {
    let active = true;
    const run = async () => {
      if (!hasCompletedOnboarding || !user?.objetivo) {
        navigate('/onboarding');
        return;
      }
      try {
        setError('');
        setCurrentStep(0);
        setCurrentStep(1);
        await new Promise((resolve) => setTimeout(resolve, 500));
        setCurrentStep(2);
        const plan = await api.planos.gerar(user.objetivo);
        if (!active) return;
        queryClient.setQueryData(['meus-planos'], (current = []) => {
          const otherPlans = (current || []).filter((item) => item.id !== plan.id);
          return [plan, ...otherPlans];
        });
        queryClient.setQueryData(['plano', plan.id], plan);
        queryClient.invalidateQueries({ queryKey: ['meus-planos'] });
        setCurrentStep(3);
        setTimeout(() => navigate('/plano'), 700);
      } catch (err) {
        setError(err.message || 'Não foi possível gerar o plano.');
      }
    };
    run();
    return () => {
      active = false;
    };
  }, [attemptKey, hasCompletedOnboarding, navigate, queryClient, user]);

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
            Revise os dados do seu perfil ou tente novamente. Se a IA falhar, o objetivo atual continua sendo {user?.objetivo || 'o informado no seu onboarding'}.
          </p>
          <div className="flex flex-col gap-3">
            <button onClick={() => setAttemptKey((value) => value + 1)} className="w-full h-12 rounded-2xl bg-primary text-primary-foreground font-semibold inline-flex items-center justify-center gap-2">
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
