import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Play, Clock, MapPin, Zap, ChevronRight } from 'lucide-react';
import { localIsoDate } from '@/lib/date';
import { WORKOUT_TYPE_COLORS } from '@/lib/workoutTypes';

export default function TodayWorkoutCard({ session, planoId, sessionIndex }) {
  if (!session) return null;

  const when = !session.dataPrevista
    ? 'Próximo treino'
    : session.dataPrevista === localIsoDate()
    ? 'Hoje'
    : new Date(`${session.dataPrevista}T00:00:00`).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'short' });

  return (
    <Link to={`/treino/${planoId}/${sessionIndex}`}>
      <motion.div
        whileTap={{ scale: 0.98 }}
        className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary/15 via-primary/5 to-card border border-primary/20 p-5"
      >
        {/* Glow */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full blur-3xl -translate-y-8 translate-x-8" />

        <div className="relative z-10">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold uppercase tracking-wide text-primary">{when}</span>
            <ChevronRight className="w-5 h-5 text-muted-foreground" />
          </div>

          <h3 className="flex items-center gap-2 text-2xl font-bold text-foreground mb-1">
            <span className={`h-3 w-3 shrink-0 rounded-full ${WORKOUT_TYPE_COLORS[session.tipo] || WORKOUT_TYPE_COLORS.easy_run}`} aria-hidden="true" />
            {session.tipoLabel}
          </h3>
          <p className="text-xs text-muted-foreground mb-4 line-clamp-2">{session.descricao}</p>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{session.duracao_min} min</span>
            </div>
            {session.distancia_km > 0 && (
              <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-muted-foreground" />
                <span className="text-xs text-muted-foreground">{String(session.distancia_km).replace('.', ',')} km</span>
              </div>
            )}
            <div className="flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{session.zona_fc}</span>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-center gap-2 bg-primary text-primary-foreground rounded-2xl py-3 font-semibold text-sm">
            <Play className="w-4 h-4" fill="currentColor" />
            Ver treino
          </div>
        </div>
      </motion.div>
    </Link>
  );
}