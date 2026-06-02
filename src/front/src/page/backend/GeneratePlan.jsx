import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Brain, CheckCircle2, Loader2, Sparkles, Zap } from 'lucide-react';
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
  const { user } = useAuth();
  const [currentStep, setCurrentStep] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const run = async () => {
      if (!user?.objetivo) {
        navigate('/onboarding');
        return;
      }
      try {
        setCurrentStep(1);
        await new Promise((resolve) => setTimeout(resolve, 500));
        setCurrentStep(2);
        await api.planos.gerar(user.objetivo);
        if (!active) return;
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
  }, [navigate, user]);

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
        <div className="mt-8 text-center">
          <p className="text-sm text-destructive mb-4">{error}</p>
          <button onClick={() => navigate('/plano')} className="text-sm text-primary">Voltar para o plano</button>
        </div>
      )}
    </div>
  );
}
