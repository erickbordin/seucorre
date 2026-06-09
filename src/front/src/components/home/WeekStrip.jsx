import { motion } from 'framer-motion';

const DAY_MAP = { dom: 0, seg: 1, ter: 2, qua: 3, qui: 4, sex: 5, sab: 6 };
const DAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

export default function WeekStrip({ sessions = [], onSelectDay, selectedDay }) {
  const today = new Date();
  const startOfWeek = new Date(today);
  startOfWeek.setDate(today.getDate() - today.getDay());

  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(startOfWeek);
    d.setDate(startOfWeek.getDate() + i);
    return d;
  });

  const sessionDays = sessions.map(s => DAY_MAP[s.dia_semana]).filter(d => d !== undefined);

  return (
    <div className="flex gap-2 justify-between px-1">
      {weekDays.map((day, i) => {
        const isToday = day.toDateString() === today.toDateString();
        const hasWorkout = sessionDays.includes(i);
        const isSelected = selectedDay === i;

        return (
          <motion.button
            key={i}
            whileTap={{ scale: 0.9 }}
            onClick={() => onSelectDay(i)}
            className={`flex flex-col items-center gap-1 py-2 px-2.5 rounded-xl transition-all ${
              isSelected
                ? 'bg-primary text-primary-foreground'
                : isToday
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground'
            }`}
          >
            <span className="text-[10px] font-medium uppercase">{DAY_LABELS[i]}</span>
            <span className={`text-sm font-bold ${isSelected ? '' : isToday ? 'text-foreground' : ''}`}>
              {day.getDate()}
            </span>
            {hasWorkout && (
              <div className={`w-1.5 h-1.5 rounded-full ${isSelected ? 'bg-primary-foreground' : 'bg-primary'}`} />
            )}
          </motion.button>
        );
      })}
    </div>
  );
}