import { motion } from 'framer-motion';
import { currentWeekDates, localIsoDate } from '@/lib/date';

const DAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

export default function WeekStrip({ sessions = [], onSelectDay, selectedDay }) {
  const today = new Date();
  const weekDays = currentWeekDates(today);
  const sessionDates = new Set(sessions.map((session) => session.dataPrevista));

  return (
    <div className="grid grid-cols-7 gap-1">
      {weekDays.map((day, i) => {
        const isToday = day.toDateString() === today.toDateString();
        const hasWorkout = sessionDates.has(localIsoDate(day));
        const isSelected = selectedDay === i;

        return (
          <motion.button
            key={i}
            whileTap={{ scale: 0.9 }}
            onClick={() => onSelectDay(i)}
            className={`flex min-w-0 flex-col items-center gap-1 py-2 rounded-xl transition-all ${
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