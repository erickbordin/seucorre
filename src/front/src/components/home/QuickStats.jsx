import { Flame, Target, TrendingUp } from 'lucide-react';

export default function QuickStats({ sessions = [] }) {
  const completed = sessions.filter(s => s.status === 'concluido').length;
  const total = sessions.length;
  const thisWeekSessions = sessions.filter(s => s.semana === 1);
  const weekCompleted = thisWeekSessions.filter(s => s.status === 'concluido').length;

  const stats = [
    { icon: Flame, label: 'Semana', value: `${weekCompleted}/${thisWeekSessions.length}`, color: 'text-orange-400', bg: 'bg-orange-400/10' },
    { icon: Target, label: 'Total', value: `${completed}/${total}`, color: 'text-blue-400', bg: 'bg-blue-400/10' },
    { icon: TrendingUp, label: 'Adesão', value: total > 0 ? `${Math.round((completed / total) * 100)}%` : '0%', color: 'text-green-400', bg: 'bg-green-400/10' },
  ];

  return (
    <div className="grid grid-cols-3 gap-3">
      {stats.map((s, i) => {
        const Icon = s.icon;
        return (
          <div key={i} className="bg-card rounded-2xl p-3 border border-border/50 text-center">
            <div className={`w-8 h-8 rounded-xl ${s.bg} flex items-center justify-center mx-auto mb-2`}>
              <Icon className={`w-4 h-4 ${s.color}`} />
            </div>
            <p className="text-lg font-bold text-foreground">{s.value}</p>
            <p className="text-[10px] text-muted-foreground">{s.label}</p>
          </div>
        );
      })}
    </div>
  );
}