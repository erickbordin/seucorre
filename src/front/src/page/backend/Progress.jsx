import { useQuery } from '@tanstack/react-query';
import { Activity, Award, Flame, Target } from 'lucide-react';
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
import { api } from '@/lib/api';

export default function Progress() {
  const { data: overview } = useQuery({ queryKey: ['progresso', 'visao'], queryFn: api.progresso.visaoGeral });
  const { data: history = [] } = useQuery({ queryKey: ['progresso', 'historico'], queryFn: api.progresso.historico });

  const weeklyData = history.map((item) => ({
    name: `S${item.numeroSemana}`,
    km: Number(item.volumeKm || 0),
    adesao: item.taxaAdesao || 0,
  }));

  const stats = [
    { icon: Target, label: 'Adesão média', value: `${overview?.taxaAdesaoMedia || 0}%`, color: 'text-primary', bg: 'bg-primary/10' },
    { icon: Flame, label: 'Treinos', value: `${overview?.totalTreinos || 0}`, color: 'text-orange-400', bg: 'bg-orange-400/10' },
    { icon: Activity, label: 'Km totais', value: `${overview?.volumeTotalKm || 0}`, color: 'text-blue-400', bg: 'bg-blue-400/10' },
    { icon: Award, label: 'Semanas', value: `${overview?.semanasRegistradas || 0}`, color: 'text-purple-400', bg: 'bg-purple-400/10' },
  ];

  return (
    <div className="px-5 pt-14 pb-28">
      <h1 className="text-2xl font-bold text-foreground mb-1">Progresso</h1>
      <p className="text-xs text-muted-foreground mb-6">Dados vindos de /api/progresso</p>

      <div className="grid grid-cols-2 gap-3 mb-6">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="bg-card rounded-2xl p-4 border border-border/50">
              <div className={`w-9 h-9 rounded-xl ${stat.bg} flex items-center justify-center mb-2`}>
                <Icon className={`w-4 h-4 ${stat.color}`} />
              </div>
              <p className="text-xl font-bold text-foreground">{stat.value}</p>
              <p className="text-[10px] text-muted-foreground">{stat.label}</p>
            </div>
          );
        })}
      </div>

      <div className="bg-card rounded-2xl border border-border/50 p-4 mb-6">
        <h3 className="text-sm font-semibold text-foreground mb-1">Volume semanal</h3>
        <p className="text-[10px] text-muted-foreground mb-4">Km registrados por semana</p>
        <div className="h-44">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={weeklyData}>
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: 'hsl(0,0%,55%)' }} />
              <YAxis hide />
              <Bar dataKey="km" radius={[8, 8, 0, 0]} fill="hsl(75,100%,65%)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Semanas</h3>
        {history.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">Registre treinos para gerar progresso.</p>
        ) : history.map((item) => (
          <div key={item.id} className="bg-card rounded-2xl p-4 border border-border/50">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold text-foreground">Semana {item.numeroSemana}</span>
              <span className="text-xs text-muted-foreground">{item.totalTreinos} treinos</span>
            </div>
            <p className="text-xs text-muted-foreground">{item.resumo}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
