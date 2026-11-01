import { Link } from 'react-router-dom';
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, ArrowRight, Award, Flame, Target } from 'lucide-react';
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
import { api } from '@/lib/api';
import { formatKm } from '@/lib/workoutTypes';

const percent = (value) => `${Math.floor(Number(value || 0))}%`;
const km = (value) => formatKm(Number(value || 0));
const contagem = (n, singular, plural) => `${n} ${n === 1 ? singular : plural}`;

function resumoSemana(item) {
  const partes = [
    [item.treinosConcluidos, 'concluído', 'concluídos'],
    [item.treinosParciais, 'parcial', 'parciais'],
    [item.treinosPerdidos, 'perdido', 'perdidos'],
  ].filter(([n]) => n > 0).map(([n, singular, plural]) => contagem(n, singular, plural));
  if (partes.length === 0) return 'Continue registrando seus treinos para enriquecer a leitura da semana.';
  const lista = partes.length > 1 ? `${partes.slice(0, -1).join(', ')} e ${partes.at(-1)}` : partes[0];
  return `${lista} · ${km(item.volumeKm)} km no total.`;
}

export default function Progress() {
  const { data: overview } = useQuery({ queryKey: ['progresso', 'visao'], queryFn: api.progresso.visaoGeral });
  const { data: history = [] } = useQuery({ queryKey: ['progresso', 'historico'], queryFn: api.progresso.historico });

  const weeklyData = history.map((item) => ({
    name: `S${item.numeroSemana}`,
    km: Number(item.volumeKm || 0),
    adesao: item.taxaAdesao || 0,
  }));

  const stats = [
    { icon: Target, label: 'Adesão média', value: percent(overview?.taxaAdesaoMedia), color: 'text-primary', bg: 'bg-primary/10' },
    { icon: Flame, label: 'Treinos', value: `${overview?.totalTreinos || 0}`, color: 'text-orange-400', bg: 'bg-orange-400/10' },
    { icon: Activity, label: 'Km totais', value: km(overview?.volumeTotalKm), color: 'text-blue-400', bg: 'bg-blue-400/10' },
    { icon: Award, label: 'Semanas', value: `${overview?.semanasRegistradas || 0}`, color: 'text-purple-400', bg: 'bg-purple-400/10' },
  ];

  const latestWeek = useMemo(() => {
    if (history.length === 0) return null;
    return history.reduce((current, item) => {
      if (!current) return item;
      return Number(item.numeroSemana || 0) > Number(current.numeroSemana || 0) ? item : current;
    }, null);
  }, [history]);

  return (
    <div className="px-5 pt-14 pb-28">
      <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground mb-1">Progresso</h1>
          <p className="text-xs text-muted-foreground">Visão semanal e evolução do ciclo</p>
        </div>
        <Link to="/historico" className="text-xs text-primary font-semibold inline-flex items-center gap-1 mt-1">
          Histórico
          <ArrowRight className="w-3 h-3" />
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-3 mb-6">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="bg-card rounded-2xl p-4 border border-border/50">
              <div className={`w-9 h-9 rounded-xl ${stat.bg} flex items-center justify-center mb-2`}>
                <Icon className={`w-4 h-4 ${stat.color}`} />
              </div>
              <p className="text-xl font-bold text-foreground tabular-nums">{stat.value}</p>
              <p className="text-[0.625rem] text-muted-foreground">{stat.label}</p>
            </div>
          );
        })}
      </div>

      {latestWeek && (
        <div className="bg-card rounded-2xl border border-border/50 p-4 mb-6">
          <div className="flex items-start justify-between gap-4 mb-2">
            <div>
              <p className="text-xs text-muted-foreground mb-1">Leitura mais recente</p>
              <h3 className="text-sm font-semibold text-foreground">Semana {latestWeek.numeroSemana}</h3>
            </div>
            <span className="text-[0.625rem] font-semibold px-2 py-1 rounded-full bg-primary/10 text-primary">
              {percent(latestWeek.taxaAdesao)} adesão
            </span>
          </div>
          <p className="text-xs text-muted-foreground">{resumoSemana(latestWeek)}</p>
        </div>
      )}

      <div className="bg-card rounded-2xl border border-border/50 p-4 mb-6">
        <h3 className="text-sm font-semibold text-foreground mb-1">Volume semanal</h3>
        <p className="text-[0.625rem] text-muted-foreground mb-4">Km registrados por semana para mostrar tendência de consistência.</p>
        {weeklyData.length === 0 ? (
          <p className="rounded-xl border border-dashed border-border p-6 text-center text-xs text-muted-foreground">O gráfico aparece depois do primeiro treino registrado.</p>
        ) : (
          <div className="h-44">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyData}>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: 'hsl(0,0%,55%)' }} />
                <YAxis hide />
                <Bar dataKey="km" radius={[8, 8, 0, 0]} maxBarSize={40} fill="hsl(75,100%,65%)" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Semanas</h3>
        {history.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-border p-6 text-center text-sm text-muted-foreground">Registre treinos para gerar progresso.</p>
        ) : history.map((item) => (
          <div key={item.id} className="bg-card rounded-2xl p-4 border border-border/50">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold text-foreground">Semana {item.numeroSemana}</span>
              <span className="text-xs text-muted-foreground">{contagem(item.totalTreinos, 'treino', 'treinos')} • {km(item.volumeKm)} km</span>
            </div>
            <div className="w-full h-2 rounded-full bg-background overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: `${Math.max(0, Math.min(100, Number(item.taxaAdesao || 0)))}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
