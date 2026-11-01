import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Sparkles } from 'lucide-react';
import { RISK_STYLES } from '@/lib/checkin';

const LONG_TEXT = 160;

export default function CheckinInsightCard({ checkin, currentWeek }) {
  const [expanded, setExpanded] = useState(false);

  if (!checkin) {
    return (
      <Link to="/check-in" className="block rounded-3xl border border-border bg-card p-5 transition-transform active:scale-[0.98]">
        <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          <Sparkles className="h-4 w-4 text-primary" aria-hidden="true" />
          Check-in da semana
        </div>
        <p className="mt-2 text-sm text-foreground">Conte como foi a semana. A IA ajusta o plano ao seu sono, dor e cansaço.</p>
        <span className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-primary">
          Fazer check-in
          <ChevronRight className="h-4 w-4" />
        </span>
      </Link>
    );
  }

  const text = (checkin.analiseIA || checkin.avaliacao || 'Sem comentário nesta semana.').replaceAll('**', '').replace(/^\s*[*-]\s+/gm, '• ');
  const isLong = text.length > LONG_TEXT;

  return (
    <section className="rounded-3xl border border-border bg-card p-5">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          <Sparkles className="h-4 w-4 text-primary" aria-hidden="true" />
          Leitura da IA · Semana {checkin.semana}
        </div>
        <span className={`shrink-0 rounded-full px-3 py-1 text-[0.625rem] font-semibold ${RISK_STYLES[checkin.nivelRisco] || RISK_STYLES.BAIXO}`}>
          {checkin.nivelRiscoLabel || checkin.nivelRisco}
        </span>
      </div>

      <p className={`mt-3 whitespace-pre-line text-sm text-foreground ${expanded ? '' : 'line-clamp-4'}`}>{text}</p>
      {isLong && (
        <button type="button" onClick={() => setExpanded((value) => !value)} className="mt-2 text-xs font-semibold text-primary">
          {expanded ? 'Mostrar menos' : 'Ler tudo'}
        </button>
      )}

      {checkin.planoReescrito && (
        <p className="mt-3 text-xs font-medium text-primary">Próximas semanas ajustadas a partir deste check-in.</p>
      )}

      {Number(checkin.semana) !== Number(currentWeek) && (
        <Link to="/check-in" className="mt-4 flex items-center justify-between border-t border-border pt-4 text-sm font-semibold text-primary">
          Fazer check-in da semana {currentWeek}
          <ChevronRight className="h-4 w-4" />
        </Link>
      )}
    </section>
  );
}
