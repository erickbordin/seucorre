import { motion } from 'framer-motion';

export default function OnboardingStep({ step, total, title, subtitle, children }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: 40 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -40 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      className="flex flex-col min-h-screen px-6 pt-16 pb-8"
    >
      {/* Progress */}
      <div className="flex gap-1.5 mb-10">
        {Array.from({ length: total }).map((_, i) => (
          <div
            key={i}
            className={`h-1 rounded-full flex-1 transition-all duration-500 ${
              i <= step ? 'bg-primary' : 'bg-secondary'
            }`}
          />
        ))}
      </div>

      <h1 className="text-2xl font-bold text-foreground mb-2">{title}</h1>
      {subtitle && (
        <p className="text-sm text-muted-foreground mb-8 leading-relaxed">{subtitle}</p>
      )}

      <div className="flex-1">{children}</div>
    </motion.div>
  );
}