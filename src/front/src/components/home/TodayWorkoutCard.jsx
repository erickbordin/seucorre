import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Play, Clock, MapPin, Zap, ChevronRight } from 'lucide-react';

const TYPE_STYLES = {
  easy_run: { color: 'text-green-400', bg: 'bg-green-400/10', label: 'Corrida Leve' },
  intervals: { color: 'text-orange-400', bg: 'bg-orange-400/10', label: 'Intervalado' },
  long_run: { color: 'text-blue-400', bg: 'bg-blue-400/10', label: 'Longão' },
  recovery: { color: 'text-purple-400', bg: 'bg-purple-400/10', label: 'Recuperação' },
  tempo: { color: 'text-yellow-400', bg: 'bg-yellow-400/10', label: 'Tempo Run' },
};

export default function TodayWorkoutCard({ session, planoId, sessionIndex }) {
  if (!session) return null;

  const style = TYPE_STYLES[session.tipo] || TYPE_STYLES.easy_run;

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
            <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${style.bg} ${style.color}`}>
              {style.label}
            </span>
            <ChevronRight className="w-5 h-5 text-muted-foreground" />
          </div>

          <h3 className="text-lg font-bold text-foreground mb-1">{session.titulo}</h3>
          <p className="text-xs text-muted-foreground mb-4 line-clamp-2">{session.descricao}</p>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{session.duracao_min} min</span>
            </div>
            {session.distancia_km > 0 && (
              <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-muted-foreground" />
                <span className="text-xs text-muted-foreground">{session.distancia_km} km</span>
              </div>
            )}
            <div className="flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">{session.zona_fc}</span>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-center gap-2 bg-primary text-primary-foreground rounded-2xl py-3 font-semibold text-sm">
            <Play className="w-4 h-4" fill="currentColor" />
            Ver Treino Completo
          </div>
        </div>
      </motion.div>
    </Link>
  );
}