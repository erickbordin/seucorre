export const RISK_STYLES = {
  BAIXO: 'bg-primary/10 text-primary',
  MODERADO: 'bg-orange-400/10 text-orange-400',
  ALTO: 'bg-destructive/10 text-destructive',
  CRITICO: 'bg-destructive/10 text-destructive',
};

export function latestCheckin(history) {
  return (history || []).reduce((latest, item) => {
    if (!latest) return item;
    const byDate = String(item.dataCheckin || '').localeCompare(String(latest.dataCheckin || ''));
    if (byDate !== 0) return byDate > 0 ? item : latest;
    return Number(item.semana) >= Number(latest.semana) ? item : latest;
  }, null);
}
