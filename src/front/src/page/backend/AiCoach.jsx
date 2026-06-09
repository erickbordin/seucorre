import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bot, Sparkles } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';

export default function AICoach() {
  const { user } = useAuth();
  const { data: plans = [] } = useQuery({ queryKey: ['meus-planos'], queryFn: api.planos.listarMeus });
  const plan = plans.find((item) => item.status === 'ativo') || plans[0];

  const insights = useMemo(() => {
    const sessoes = plan?.sessoes || [];
    const atrasados = sessoes.filter((item) => item.atrasada && !item.executada).length;
    const longos = sessoes.filter((item) => item.tipo === 'long_run').length;
    return [
      `Objetivo atual: ${user?.objetivo || 'não informado'}.`,
      plan ? `Plano com ${plan.total_semanas} semanas e ${sessoes.length} sessões.` : 'Gere um plano para receber recomendações contextuais.',
      atrasados > 0 ? `Há ${atrasados} treino(s) atrasado(s). Priorize retomar com carga leve.` : 'Não há treinos atrasados no plano carregado.',
      longos > 0 ? `O plano inclui ${longos} treino(s) longo(s). Evite aumentar volume fora do planejado.` : 'Seu plano atual ainda não tem treinos longos mapeados.',
    ];
  }, [plan, user]);

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center">
          <Sparkles className="w-5 h-5 text-primary" />
        </div>
        <div>
          <h1 className="text-lg font-bold text-foreground">Coach do Plano</h1>
          <p className="text-xs text-muted-foreground">Recomendações com dados do backend</p>
        </div>
      </div>

      <div className="space-y-3">
        {insights.map((item) => (
          <div key={item} className="flex gap-3 rounded-2xl bg-card border border-border p-4">
            <Bot className="w-5 h-5 text-primary shrink-0 mt-0.5" />
            <p className="text-sm text-foreground">{item}</p>
          </div>
        ))}
      </div>

      <p className="text-xs text-muted-foreground mt-6">
        O protótipo tinha chat via Base44. O backend atual só expõe geração de plano com IA, então esta tela foi ajustada para não chamar uma API inexistente.
      </p>
    </div>
  );
}
