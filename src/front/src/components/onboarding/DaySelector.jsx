import { motion } from 'framer-motion';

const DAYS = [
  { key: 'seg', label: 'S' },
  { key: 'ter', label: 'T' },
  { key: 'qua', label: 'Q' },
  { key: 'qui', label: 'Q' },
  { key: 'sex', label: 'S' },
  { key: 'sab', label: 'S' },
  { key: 'dom', label: 'D' },
];

const FULL_LABELS = {
  seg: 'Seg', ter: 'Ter', qua: 'Qua', qui: 'Qui', sex: 'Sex', sab: 'Sáb', dom: 'Dom'
};

export default function DaySelector({ selected, onChange, max = DAYS.length }) {
  const toggle = (day) => {
    if (selected.includes(day)) {
      onChange(selected.filter(d => d !== day));
    } else if (selected.length < max) {
      onChange([...selected, day]);
    }
  };

  return (
    <div className="flex gap-2 justify-between">
      {DAYS.map(({ key, label }) => {
        const isActive = selected.includes(key);
        const isBlocked = !isActive && selected.length >= max;
        return (
          <motion.button
            type="button"
            key={key}
            whileTap={{ scale: 0.9 }}
            onClick={() => toggle(key)}
            disabled={isBlocked}
            className={`w-11 h-14 rounded-xl flex flex-col items-center justify-center gap-0.5 transition-all duration-200 ${
              isActive
                ? 'bg-primary text-primary-foreground'
                : `bg-card border border-border text-muted-foreground ${isBlocked ? 'opacity-40' : ''}`
            }`}
          >
            <span className="text-xs font-medium">{FULL_LABELS[key]}</span>
            <span className="text-[0.625rem] opacity-70">{label}</span>
          </motion.button>
        );
      })}
    </div>
  );
}
