import { Sparkles } from 'lucide-react';

export default function AIInsightCard({ insight }) {
  if (!insight) return null;

  return (
    <div className="rounded-2xl bg-gradient-to-r from-purple-500/10 via-blue-500/10 to-primary/10 border border-purple-500/20 p-4">
      <div className="flex items-start gap-3">
        <div className="w-8 h-8 rounded-xl bg-purple-500/20 flex items-center justify-center shrink-0">
          <Sparkles className="w-4 h-4 text-purple-400" />
        </div>
        <div>
          <p className="text-xs font-semibold text-purple-400 mb-1">IA Coach</p>
          <p className="text-xs text-muted-foreground leading-relaxed">{insight}</p>
        </div>
      </div>
    </div>
  );
}