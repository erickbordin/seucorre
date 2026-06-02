import { motion } from 'framer-motion';
import { Check } from 'lucide-react';

export default function SelectionCard({ icon: Icon, label, description, selected, onClick, emoji }) {
  return (
    <motion.button
      type="button"
      whileTap={{ scale: 0.97 }}
      onClick={onClick}
      className={`w-full flex items-center gap-4 p-4 rounded-2xl border transition-all duration-200 text-left ${
        selected
          ? 'bg-primary/10 border-primary/40 ring-1 ring-primary/20'
          : 'bg-card border-border hover:border-border/80'
      }`}
    >
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-lg shrink-0 ${
        selected ? 'bg-primary/20' : 'bg-secondary'
      }`}>
        {emoji ? (
          <span className="text-xl">{emoji}</span>
        ) : Icon ? (
          <Icon className={`w-5 h-5 ${selected ? 'text-primary' : 'text-muted-foreground'}`} />
        ) : null}
      </div>
      <div className="flex-1 min-w-0">
        <p className={`font-semibold text-sm ${selected ? 'text-primary' : 'text-foreground'}`}>
          {label}
        </p>
        {description && (
          <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{description}</p>
        )}
      </div>
      {selected && (
        <div className="w-6 h-6 rounded-full bg-primary flex items-center justify-center shrink-0">
          <Check className="w-3.5 h-3.5 text-primary-foreground" />
        </div>
      )}
    </motion.button>
  );
}
