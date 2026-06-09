export const LEVEL_OPTIONS = [
  { value: 'INICIANTE', label: 'Iniciante', description: 'Começando agora' },
  { value: 'INTERMEDIARIO', label: 'Intermediário', description: 'Corre com alguma rotina' },
  { value: 'AVANCADO', label: 'Avançado', description: 'Treina com consistência' },
];

export const GOAL_OPTIONS = [
  { value: 'SAUDE_GERAL', label: 'Saúde', description: 'Bem-estar e rotina' },
  { value: 'EMAGRECER', label: 'Emagrecer', description: 'Perda de peso' },
  { value: 'COMPLETAR_5K', label: 'Completar 5 km', description: 'Primeira prova curta' },
  { value: 'COMPLETAR_10K', label: 'Completar 10 km', description: 'Ganhar base para provas mais longas' },
  { value: 'MELHORAR_5K', label: 'Melhorar 5 km', description: 'Ganhar velocidade' },
  { value: 'MELHORAR_10K', label: 'Melhorar 10 km', description: 'Evoluir ritmo e consistência' },
  { value: 'COMPLETAR_MEIA_MARATONA', label: 'Meia maratona', description: 'Preparar 21 km' },
  { value: 'MELHORAR_MEIA_MARATONA', label: 'Melhorar meia maratona', description: 'Baixar tempo nos 21 km' },
  { value: 'COMPLETAR_MARATONA', label: 'Maratona', description: 'Preparar 42 km' },
  { value: 'MELHORAR_MARATONA', label: 'Melhorar maratona', description: 'Evoluir resistência e ritmo' },
];

export const GENDER_OPTIONS = [
  { value: 'FEMININO', label: 'Feminino' },
  { value: 'MASCULINO', label: 'Masculino' },
];

export const LEVEL_LABELS = Object.fromEntries(LEVEL_OPTIONS.map((item) => [item.value, item.label]));
export const GOAL_LABELS = Object.fromEntries(GOAL_OPTIONS.map((item) => [item.value, item.label]));

export function parseTrainingDays(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);
}

export function serializeTrainingDays(days) {
  return (days || []).map((item) => String(item).trim().toUpperCase()).filter(Boolean).join(',');
}

export function hasCompletedOnboarding(user) {
  if (!user) return false;
  return Boolean(
    user.nivelCondicionamento &&
    user.objetivo &&
    user.pesoKg != null &&
    user.alturaCm != null &&
    user.dataNascimento &&
    user.genero &&
    user.diasDisponiveisSemana &&
    user.diasSemanaTreino
  );
}

export function birthDateFromAge(age) {
  const years = Number(age);
  if (!Number.isFinite(years) || years <= 0) return '';
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

export function ageFromBirthDate(value) {
  if (!value) return '';
  const birthDate = new Date(`${value}T00:00:00`);
  if (Number.isNaN(birthDate.getTime())) return '';

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  const dayDiff = today.getDate() - birthDate.getDate();
  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age -= 1;
  }
  return String(age);
}
