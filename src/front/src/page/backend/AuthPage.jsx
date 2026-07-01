import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronLeft, Loader2, LogIn, Mail, ShieldCheck, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import DaySelector from '@/components/onboarding/DaySelector';
import SelectionCard from '@/components/onboarding/SelectionCard';
import {
  ageFromBirthDate,
  birthDateFromAge,
  parseTrainingDays,
  serializeTrainingDays,
} from '@/lib/user';

const DISTANCE_GOALS = [
  { value: '3K', label: '3 km', description: 'Comecar leve e ganhar consistencia' },
  { value: '5K', label: '5 km', description: 'Recomendado para construir base com seguranca', recommended: true },
  { value: '10K', label: '10 km', description: 'Disponivel para quem esta muito ativo' },
];

const RUNNER_GOALS = [
  { value: 'COMPLETAR_5K', label: '5 km', description: 'Completar a distancia com boa base' },
  { value: 'COMPLETAR_10K', label: '10 km', description: 'Evoluir resistencia e ritmo' },
  { value: 'COMPLETAR_15K', label: '15 km', description: 'Subir volume sem perder controle' },
  { value: 'COMPLETAR_MEIA_MARATONA', label: '21 km', description: 'Preparar meia maratona' },
  { value: 'COMPLETAR_MARATONA', label: '42 km', description: 'Preparar maratona' },
  { value: 'MELHORAR_DISTANCIA', label: 'Melhorar marca', description: 'Baixar tempo em uma distancia' },
  { value: 'VOLTAR_A_CORRER', label: 'Voltar a correr', description: 'Retomar rotina com progressao' },
];

const LEVELS = [
  { value: 'INICIANTE', label: 'Iniciante', description: 'Corre pouco ou esta retomando do zero' },
  { value: 'INTERMEDIARIO', label: 'Intermediario', description: 'Ja treina com alguma frequencia' },
  { value: 'AVANCADO', label: 'Avancado', description: 'Treina com consistencia e usa zonas/ritmos' },
];

const RP_DISTANCES = [
  { value: '5', label: '5 km' },
  { value: '10', label: '10 km' },
  { value: '21', label: '21 km' },
  { value: '42', label: '42 km' },
];

const createEmptyForm = () => ({
  nome: '',
  email: '',
  senha: '',
  telefone: '',
  idade: '',
  alturaCm: '',
  pesoKg: '',
  jaCorre: null,
  fazAtividade: null,
  atividadeNivel: '',
  horasSonoMedia: '',
  diasSemanaTreino: ['seg', 'qua', 'sex'],
  semanasTreino: '10',
  objetivoNaoCorredor: '5K',
  objetivoCorredor: 'COMPLETAR_10K',
  nivelCorrida: '',
  distanciaRp: '5',
  paceRp: '',
  zonasPace: {
    z1: '',
    z2: '',
    z3: '',
    z4: '',
    z5: '',
  },
});

const PHYSICAL_METRIC_LIMITS = {
  idade: { min: 12, max: 90 },
  alturaCm: { min: 120, max: 230 },
  pesoKg: { min: 30, max: 250 },
};

const PACE_LIMITS = {
  minSeconds: 150,
  maxSeconds: 720,
};

const NO_RUNNER_WEEKS_CONFIG = {
  '3K': { label: '3 km', min: 6, max: 14, recommended: 8 },
  '5K': { label: '5 km', min: 8, max: 18, recommended: 10 },
  '10K': { label: '10 km', min: 12, max: 24, recommended: 16 },
};

const RUNNER_WEEKS_CONFIG = {
  COMPLETAR_5K: { label: '5 km', min: 6, max: 12, recommended: 8 },
  COMPLETAR_10K: { label: '10 km', min: 8, max: 16, recommended: 10 },
  COMPLETAR_15K: { label: '15 km', min: 10, max: 18, recommended: 12 },
  COMPLETAR_MEIA_MARATONA: { label: '21 km', min: 12, max: 20, recommended: 14 },
  COMPLETAR_MARATONA: { label: '42 km', min: 16, max: 28, recommended: 20 },
  VOLTAR_A_CORRER: { label: 'voltar a correr', min: 8, max: 16, recommended: 10 },
};

const IMPROVEMENT_WEEKS_CONFIG = {
  '5': { label: 'melhorar 5 km', min: 8, max: 14, recommended: 10 },
  '10': { label: 'melhorar 10 km', min: 10, max: 16, recommended: 12 },
  '21': { label: 'melhorar 21 km', min: 12, max: 20, recommended: 14 },
  '42': { label: 'melhorar 42 km', min: 16, max: 28, recommended: 20 },
};

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const sanitizeIntegerMetric = (value) => String(value ?? '').replace(/\D/g, '');

const sanitizeDecimalMetric = (value) => {
  const normalized = String(value ?? '')
    .replace(',', '.')
    .replace(/[^\d.]/g, '');

  if (!normalized) return '';
  const [wholePart, ...decimalParts] = normalized.split('.');
  const decimalPart = decimalParts.join('').slice(0, 2);
  if (!wholePart && !decimalPart) return '';
  return decimalPart ? `${wholePart || '0'}.${decimalPart}` : wholePart;
};

const sanitizePaceInput = (value) => {
  const normalized = String(value ?? '').replace(/[^\d:.,]/g, '');
  let result = '';
  let separatorUsed = false;

  for (const char of normalized) {
    if (/\d/.test(char)) {
      result += char;
      continue;
    }

    if (!separatorUsed) {
      result += ':';
      separatorUsed = true;
    }
  }

  return result.slice(0, 5);
};

const parsePaceToSeconds = (value) => {
  const normalized = sanitizePaceInput(value);
  const match = normalized.match(/^(\d{1,2}):(\d{1,2})$/);
  if (!match) return null;

  const minutes = Number(match[1]);
  const seconds = Number(match[2]);
  if (!Number.isInteger(minutes) || !Number.isInteger(seconds) || seconds >= 60) return null;

  return (minutes * 60) + seconds;
};

const formatPaceFromSeconds = (value) => {
  if (!Number.isFinite(value) || value <= 0) return '';
  const totalSeconds = Math.round(value);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes + ':' + String(seconds).padStart(2, '0');
};

const paceDecimalToDisplay = (value) => {
  const parsed = Number(String(value ?? '').replace(',', '.'));
  if (!Number.isFinite(parsed) || parsed <= 0) return '';
  return formatPaceFromSeconds(parsed * 60);
};

const paceDisplayToDecimal = (value) => {
  const totalSeconds = parsePaceToSeconds(value);
  if (!Number.isFinite(totalSeconds)) return null;
  return Number((totalSeconds / 60).toFixed(2));
};

const getStoredRacePace = (perfilCorrida) => {
  if (perfilCorrida?.pace5kMinKm != null) return { distanciaRp: '5', paceRp: paceDecimalToDisplay(perfilCorrida.pace5kMinKm) };
  if (perfilCorrida?.pace10kMinKm != null) return { distanciaRp: '10', paceRp: paceDecimalToDisplay(perfilCorrida.pace10kMinKm) };
  if (perfilCorrida?.pace21kMinKm != null) return { distanciaRp: '21', paceRp: paceDecimalToDisplay(perfilCorrida.pace21kMinKm) };
  if (perfilCorrida?.pace42kMinKm != null) return { distanciaRp: '42', paceRp: paceDecimalToDisplay(perfilCorrida.pace42kMinKm) };
  return { distanciaRp: '5', paceRp: '' };
};

const resolveTrainingWeeksBaseConfig = (form) => {
  const isBeginnerBaseFlow = form.jaCorre === false || form.nivelCorrida === 'INICIANTE';

  if (isBeginnerBaseFlow) {
    return NO_RUNNER_WEEKS_CONFIG[form.objetivoNaoCorredor || '5K'] || NO_RUNNER_WEEKS_CONFIG['5K'];
  }

  if (form.objetivoCorredor === 'MELHORAR_DISTANCIA') {
    return IMPROVEMENT_WEEKS_CONFIG[form.distanciaRp || '5'] || IMPROVEMENT_WEEKS_CONFIG['5'];
  }

  return RUNNER_WEEKS_CONFIG[form.objetivoCorredor || 'COMPLETAR_10K'] || RUNNER_WEEKS_CONFIG.COMPLETAR_10K;
};

const buildTrainingWeeksGuidance = (form) => {
  const baseConfig = resolveTrainingWeeksBaseConfig(form);
  const age = Number(form.idade);
  const sleepHours = Number(form.horasSonoMedia);
  const trainingDays = form.diasSemanaTreino.length;
  let min = baseConfig.min;
  let max = baseConfig.max;
  let recommended = baseConfig.recommended;
  const reasons = [];

  if (form.jaCorre === false || form.nivelCorrida === 'INICIANTE') {
    min += 1;
    max += 2;
    recommended += 2;
    reasons.push('voce ainda esta construindo base');
  }

  if (trainingDays <= 2) {
    min += 2;
    max += 4;
    recommended += 3;
    reasons.push('voce tem ate 2 dias de treino por semana');
  } else if (trainingDays === 3) {
    min += 1;
    max += 2;
    recommended += 1;
    reasons.push('voce tem 3 dias de treino por semana');
  } else if (trainingDays >= 5 && form.jaCorre && form.nivelCorrida === 'AVANCADO') {
    min -= 1;
    max -= 1;
    recommended -= 1;
    reasons.push('sua rotina suporta um ciclo um pouco mais enxuto');
  }

  if (sleepHours > 0 && sleepHours < 6) {
    min += 1;
    max += 2;
    recommended += 2;
    reasons.push('menos de 6 horas de sono pedem mais recuperacao');
  } else if (sleepHours >= 6 && sleepHours < 7) {
    recommended += 1;
    max += 1;
    reasons.push('menos de 7 horas de sono pedem progressao gradual');
  }

  if (form.fazAtividade === false) {
    recommended += 1;
    max += 1;
    reasons.push('voce relatou pouca base complementar');
  }

  if (form.atividadeNivel === 'POUCO_ATIVO') {
    recommended += 1;
    max += 1;
    reasons.push('sua rotina atual e pouco ativa');
  } else if (form.atividadeNivel === 'MUITO_ATIVO' && trainingDays >= 4 && sleepHours >= 7) {
    recommended -= 1;
  }

  if (Number.isFinite(age) && age >= 60) {
    min += 1;
    max += 2;
    recommended += 2;
    reasons.push('vale deixar mais margem para recuperacao');
  } else if (Number.isFinite(age) && age >= 45) {
    recommended += 1;
    max += 1;
    reasons.push('uma margem extra ajuda na recuperacao');
  }

  min = clamp(min, 6, 30);
  max = clamp(max, min + 2, 32);
  recommended = clamp(recommended, min, max);

  const summary = reasons.length > 0
    ? 'Recomendacao ajustada porque ' + reasons.slice(0, 2).join(' e ') + '.'
    : 'Recomendacao baseada no seu objetivo e no volume atual de treino.';

  return {
    ...baseConfig,
    min,
    max,
    recommended,
    summary,
  };
};

const buildTrainingErrors = (form, guidance) => {
  const errors = {};
  const weeks = Number(form.semanasTreino);
  const paceSeconds = parsePaceToSeconds(form.paceRp);

  if (!form.semanasTreino || !Number.isInteger(weeks) || weeks < guidance.min || weeks > guidance.max) {
    errors.semanasTreino = 'Para ' + guidance.label + ', use entre ' + guidance.min + ' e ' + guidance.max + ' semanas.';
  }

  if (!form.paceRp) {
    errors.paceRp = 'Informe o pace do RP no formato m:ss.';
  } else if (!Number.isFinite(paceSeconds)) {
    errors.paceRp = 'Use o formato m:ss, por exemplo 5:30 min/km.';
  } else if (paceSeconds < PACE_LIMITS.minSeconds || paceSeconds > PACE_LIMITS.maxSeconds) {
    errors.paceRp = 'Informe um pace entre 2:30 e 12:00 min/km.';
  }

  return errors;
};


const buildPhysicalMetricErrors = (form) => {
  const errors = {};
  const idade = Number(form.idade);
  const alturaCm = Number(form.alturaCm);
  const pesoKg = Number(String(form.pesoKg || '').replace(',', '.'));

  if (!form.idade || !Number.isInteger(idade) || idade < PHYSICAL_METRIC_LIMITS.idade.min || idade > PHYSICAL_METRIC_LIMITS.idade.max) {
    errors.idade = 'Informe uma idade entre 12 e 90 anos.';
  }

  if (!form.alturaCm || !Number.isInteger(alturaCm) || alturaCm < PHYSICAL_METRIC_LIMITS.alturaCm.min || alturaCm > PHYSICAL_METRIC_LIMITS.alturaCm.max) {
    errors.alturaCm = 'Informe uma altura entre 120 cm e 230 cm.';
  }

  if (!form.pesoKg || !Number.isFinite(pesoKg) || pesoKg < PHYSICAL_METRIC_LIMITS.pesoKg.min || pesoKg > PHYSICAL_METRIC_LIMITS.pesoKg.max) {
    errors.pesoKg = 'Informe um peso entre 30 kg e 250 kg.';
  }

  return errors;
};

const buildFormFromUser = (user) => {
  const days = parseTrainingDays(user?.diasSemanaTreino);
  const storedRacePace = getStoredRacePace(user?.perfilCorrida);

  return {
    ...createEmptyForm(),
    nome: user?.nome || '',
    email: user?.email || '',
    idade: ageFromBirthDate(user?.dataNascimento),
    pesoKg: user?.pesoKg != null ? String(user.pesoKg) : '',
    alturaCm: user?.alturaCm != null ? String(user.alturaCm) : '',
    jaCorre: user?.jaCorre ?? null,
    diasSemanaTreino: days.length > 0 ? days : ['seg', 'qua', 'sex'],
    horasSonoMedia: user?.horasSonoMedia != null ? String(user.horasSonoMedia) : '',
    nivelCorrida: user?.nivelCondicionamento || '',
    objetivoCorredor: user?.objetivo || 'COMPLETAR_10K',
    objetivoNaoCorredor: user?.objetivo === 'COMPLETAR_10K' ? '10K' : user?.objetivo === 'COMPLETAR_5K' ? '5K' : '5K',
    distanciaRp: storedRacePace.distanciaRp,
    paceRp: storedRacePace.paceRp,
  };
};

const toOptionalInteger = (value) => {
  if (value === '' || value == null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const toOptionalDecimal = (value) => {
  if (value === '' || value == null) return null;
  const normalized = String(value).replace(',', '.');
  const parsed = Number(normalized);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
};

const getNoRunnerGoal = (value) => {
  if (value === '10K') return 'COMPLETAR_10K';
  return 'COMPLETAR_5K';
};

const getRunnerGoal = (form) => {
  if (form.objetivoCorredor === 'MELHORAR_DISTANCIA') {
    if (form.distanciaRp === '42') return 'MELHORAR_MARATONA';
    if (form.distanciaRp === '21') return 'MELHORAR_MEIA_MARATONA';
    if (form.distanciaRp === '10') return 'MELHORAR_10K';
    return 'MELHORAR_5K';
  }

  if (form.objetivoCorredor === 'VOLTAR_A_CORRER') return 'SAUDE_GERAL';
  if (form.objetivoCorredor === 'COMPLETAR_15K') return 'COMPLETAR_10K';
  return form.objetivoCorredor;
};

const buildPerfilCorrida = (form) => {
  const pace = paceDisplayToDecimal(form.paceRp);
  if (!pace) return null;

  return {
    pace5kMinKm: form.distanciaRp === '5' ? pace : null,
    pace10kMinKm: form.distanciaRp === '10' ? pace : null,
    pace21kMinKm: form.distanciaRp === '21' ? pace : null,
    pace42kMinKm: form.distanciaRp === '42' ? pace : null,
    vo2Estimado: null,
    zonasFc: [],
  };
};

const buildProfilePayload = (form) => {
  const runnerLevel = form.jaCorre ? form.nivelCorrida : 'INICIANTE';
  const isBeginnerRunner = form.jaCorre && form.nivelCorrida === 'INICIANTE';
  const useNoRunnerFlow = !form.jaCorre || isBeginnerRunner;
  const nivelCondicionamento = useNoRunnerFlow
    ? form.atividadeNivel === 'MUITO_ATIVO' ? 'INTERMEDIARIO' : 'INICIANTE'
    : runnerLevel;

  return {
    telefone: form.telefone || null,
    dadosFisicos: {
      pesoKg: toOptionalDecimal(form.pesoKg),
      alturaCm: toOptionalDecimal(form.alturaCm),
      dataNascimento: birthDateFromAge(form.idade),
      genero: 'MASCULINO',
      fcRepouso: null,
      fcMaxima: null,
      horasSonoMedia: toOptionalInteger(form.horasSonoMedia),
      sedentario: form.atividadeNivel !== 'MUITO_ATIVO' && nivelCondicionamento === 'INICIANTE',
    },
    perfilAtleta: {
      nivelCondicionamento,
      objetivo: useNoRunnerFlow ? getNoRunnerGoal(form.objetivoNaoCorredor) : getRunnerGoal(form),
      jaCorre: Boolean(form.jaCorre),
      diasDisponiveisSemana: form.diasSemanaTreino.length,
      diasSemanaTreino: serializeTrainingDays(form.diasSemanaTreino),
    },
    perfilCorrida: useNoRunnerFlow ? null : buildPerfilCorrida(form),
    condicoesSaude: [],
    dispositivos: [],
  };
};

const GoogleButton = ({ label, onClick }) => (
  <Button
    type="button"
    variant="outline"
    onClick={onClick}
    className="h-12 w-full rounded-xl border-border bg-card text-foreground hover:bg-secondary"
  >
    <span className="mr-2 flex h-5 w-5 items-center justify-center rounded-full bg-foreground text-[11px] font-bold text-background">G</span>
    {label}
  </Button>
);

export default function AuthPage({ mode = 'register' }) {
  const navigate = useNavigate();
  const { hasCompletedOnboarding, login, reloadUser, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(createEmptyForm);
  const [profileStep, setProfileStep] = useState(0);
  const [touchedPhysicalFields, setTouchedPhysicalFields] = useState({});
  const [touchedTrainingFields, setTouchedTrainingFields] = useState({});

  const isLogin = mode === 'login';
  const isProfileOnly = mode === 'profile';
  const isNoRunnerFlow = form.jaCorre === false;
  const isBeginnerRunnerFlow = form.jaCorre === true && form.nivelCorrida === 'INICIANTE';
  const isIntermediate = form.jaCorre && form.nivelCorrida === 'INTERMEDIARIO';
  const isAdvanced = form.jaCorre && form.nivelCorrida === 'AVANCADO';

  const profileSteps = useMemo(() => {
    if (form.jaCorre === null || profileStep < 2) return ['dados', 'historico', 'nivel'];
    if (isNoRunnerFlow) return ['dados', 'historico', 'base', 'objetivo'];
    if (isBeginnerRunnerFlow) return ['dados', 'historico', 'nivel', 'base', 'objetivo'];
    return ['dados', 'historico', 'nivel', 'corrida', 'performance'];
  }, [form.jaCorre, isBeginnerRunnerFlow, isNoRunnerFlow, profileStep]);

  useEffect(() => {
    if (isProfileOnly) {
      setForm(buildFormFromUser(user));
      setProfileStep(0);
      setTouchedPhysicalFields({});
      setTouchedTrainingFields({});
      return;
    }
    setForm(createEmptyForm());
    setTouchedPhysicalFields({});
    setTouchedTrainingFields({});
  }, [isProfileOnly, user]);

  const totalSteps = isProfileOnly ? profileSteps.length : 1;
  const currentKey = profileSteps[Math.min(profileStep, profileSteps.length - 1)];
  const isLastProfileStep = profileStep === profileSteps.length - 1;

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const touchPhysicalField = (field) => {
    setTouchedPhysicalFields((current) => ({ ...current, [field]: true }));
  };

  const touchTrainingField = (field) => {
    setTouchedTrainingFields((current) => ({ ...current, [field]: true }));
  };

  const handlePhysicalMetricChange = (field, value) => {
    const normalizedValue = field === 'pesoKg' ? sanitizeDecimalMetric(value) : sanitizeIntegerMetric(value);
    updateForm(field, normalizedValue);
    if (error) setError('');
  };

  const shouldShowPhysicalMetricError = (field) => Boolean(touchedPhysicalFields[field] && physicalMetricErrors[field]);

  const getPhysicalMetricInputClassName = (field) => 'h-12 rounded-xl bg-card ' + (shouldShowPhysicalMetricError(field) ? 'border-destructive' : 'border-border');

  const shouldShowTrainingFieldError = (field) => Boolean(touchedTrainingFields[field] && trainingErrors[field]);

  const getTrainingFieldInputClassName = (field) => 'h-12 rounded-xl bg-card ' + (shouldShowTrainingFieldError(field) ? 'border-destructive' : 'border-border');

  const handleWeeksChange = (value) => {
    updateForm('semanasTreino', sanitizeIntegerMetric(value));
    if (error) setError('');
  };

  const handlePaceChange = (value) => {
    updateForm('paceRp', sanitizePaceInput(value));
    if (error) setError('');
  };

  const handlePaceBlur = () => {
    touchTrainingField('paceRp');
    const paceSeconds = parsePaceToSeconds(form.paceRp);
    if (Number.isFinite(paceSeconds)) {
      updateForm('paceRp', formatPaceFromSeconds(paceSeconds));
    }
  };

  const updateZone = (field, value) => {
    setForm((current) => ({
      ...current,
      zonasPace: { ...current.zonasPace, [field]: value },
    }));
  };

  const physicalMetricErrors = useMemo(() => buildPhysicalMetricErrors(form), [form.alturaCm, form.idade, form.pesoKg]);
  const hasPhysicalMetricErrors = Object.keys(physicalMetricErrors).length > 0;
  const trainingWeeksGuidance = useMemo(() => buildTrainingWeeksGuidance(form), [form.atividadeNivel, form.diasSemanaTreino, form.distanciaRp, form.fazAtividade, form.horasSonoMedia, form.idade, form.jaCorre, form.nivelCorrida, form.objetivoCorredor, form.objetivoNaoCorredor]);
  const trainingErrors = useMemo(() => buildTrainingErrors(form, trainingWeeksGuidance), [form.paceRp, form.semanasTreino, trainingWeeksGuidance]);

  const isCurrentProfileStepValid = useMemo(() => {
    if (!isProfileOnly) return true;

    if (currentKey === 'dados') {
      return !hasPhysicalMetricErrors;
    }

    if (currentKey === 'historico') {
      return form.jaCorre !== null;
    }

    if (currentKey === 'nivel') {
      return Boolean(form.nivelCorrida);
    }

    if (currentKey === 'base') {
      return form.fazAtividade !== null && Boolean(form.atividadeNivel && form.horasSonoMedia);
    }

    if (currentKey === 'objetivo') {
      const wants10k = form.objetivoNaoCorredor === '10K';
      return Boolean(
        form.diasSemanaTreino.length > 0 &&
        form.objetivoNaoCorredor &&
        !trainingErrors.semanasTreino &&
        (!wants10k || form.atividadeNivel === 'MUITO_ATIVO')
      );
    }

    if (currentKey === 'corrida') {
      return Boolean(form.objetivoCorredor);
    }

    if (currentKey === 'performance') {
      const hasBase = Boolean(form.distanciaRp && form.diasSemanaTreino.length > 0 && !trainingErrors.paceRp && !trainingErrors.semanasTreino);
      const hasZones = !isAdvanced || Object.values(form.zonasPace).every(Boolean);
      return hasBase && hasZones;
    }

    return true;
  }, [currentKey, form, hasPhysicalMetricErrors, isAdvanced, isProfileOnly, trainingErrors]);

  const valid = useMemo(() => {
    if (isLogin) return Boolean(form.email && form.senha);
    if (!isProfileOnly) return Boolean(form.email && form.senha.length >= 6);

    return Boolean(
      form.idade &&
      form.alturaCm &&
      form.pesoKg &&
      form.jaCorre !== null &&
      isCurrentProfileStepValid
    );
  }, [form, isCurrentProfileStepValid, isLogin, isProfileOnly]);

  const nextRouteAfterProfileSave = hasCompletedOnboarding ? '/' : '/gerar-plano';

  const submit = async (event) => {
    event.preventDefault();
    if (!valid || loading) return;

    setLoading(true);
    setError('');

    try {
      if (isLogin) {
        const currentUser = await login({ email: form.email, senha: form.senha });
        const shouldCompleteProfile = !currentUser?.objetivo || !currentUser?.nivelCondicionamento;
        navigate(shouldCompleteProfile ? '/onboarding' : '/');
        return;
      }

      if (isProfileOnly) {
        await api.usuarios.atualizarOnboarding(buildProfilePayload(form));
      } else {
        await api.usuarios.registrar({
          nome: form.email.split('@')[0],
          email: form.email,
          senha: form.senha,
        });
        await login({ email: form.email, senha: form.senha });
      }

      await reloadUser();
      navigate(isProfileOnly ? nextRouteAfterProfileSave : '/onboarding');
    } catch (err) {
      setError(err.message || 'Nao foi possivel concluir a operacao.');
    } finally {
      setLoading(false);
    }
  };

  const goToNextProfileStep = () => {
    if (currentKey === 'dados' && !isCurrentProfileStepValid) {
      setTouchedPhysicalFields({ idade: true, alturaCm: true, pesoKg: true });
      setError('Revise idade, altura e peso para continuar.');
      return;
    }

    if (currentKey === 'objetivo' && !isCurrentProfileStepValid) {
      setTouchedTrainingFields((current) => ({ ...current, semanasTreino: true }));
      setError(trainingErrors.semanasTreino || 'Preencha os dados obrigatorios desta etapa para continuar.');
      return;
    }

    if (currentKey === 'performance' && !isCurrentProfileStepValid) {
      setTouchedTrainingFields((current) => ({ ...current, paceRp: true, semanasTreino: true }));
      setError(trainingErrors.paceRp || trainingErrors.semanasTreino || 'Preencha os dados obrigatorios desta etapa para continuar.');
      return;
    }

    if (!isCurrentProfileStepValid) {
      setError('Preencha os dados obrigatorios desta etapa para continuar.');
      return;
    }

    setError('');
    setProfileStep((current) => Math.min(current + 1, profileSteps.length - 1));
  };

  const handleGoogle = () => {
    setError('Entrada com Google ainda precisa ser conectada no backend.');
  };

  const renderProgress = () => (
    <div className="mb-8">
      <div className="mb-4 flex gap-1.5">
        {Array.from({ length: totalSteps }).map((_, index) => (
          <div
            key={index}
            className={`h-1 flex-1 rounded-full ${index <= profileStep ? 'bg-primary' : 'bg-secondary'}`}
          />
        ))}
      </div>
      <p className="mb-2 text-xs font-semibold text-primary">Etapa {profileStep + 1} de {totalSteps}</p>
    </div>
  );

  const renderProfileStep = () => (
    <>
      {renderProgress()}

      {currentKey === 'dados' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Dados do atleta</h2>
            <p className="mt-2 text-sm text-muted-foreground">Comece com o basico para calibrar volume e seguranca.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Input type="text" inputMode="numeric" placeholder="Idade" value={form.idade} onChange={(event) => handlePhysicalMetricChange('idade', event.target.value)} onBlur={() => touchPhysicalField('idade')} aria-invalid={shouldShowPhysicalMetricError('idade')} className={getPhysicalMetricInputClassName('idade')} />
              {shouldShowPhysicalMetricError('idade') ? <p className="text-xs text-destructive">{physicalMetricErrors.idade}</p> : null}
            </div>
            <div className="space-y-2">
              <Input type="text" inputMode="numeric" placeholder="Altura cm" value={form.alturaCm} onChange={(event) => handlePhysicalMetricChange('alturaCm', event.target.value)} onBlur={() => touchPhysicalField('alturaCm')} aria-invalid={shouldShowPhysicalMetricError('alturaCm')} className={getPhysicalMetricInputClassName('alturaCm')} />
              {shouldShowPhysicalMetricError('alturaCm') ? <p className="text-xs text-destructive">{physicalMetricErrors.alturaCm}</p> : null}
            </div>
          </div>
          <div className="space-y-2">
            <Input type="text" inputMode="decimal" placeholder="Peso kg" value={form.pesoKg} onChange={(event) => handlePhysicalMetricChange('pesoKg', event.target.value)} onBlur={() => touchPhysicalField('pesoKg')} aria-invalid={shouldShowPhysicalMetricError('pesoKg')} className={getPhysicalMetricInputClassName('pesoKg')} />
            {shouldShowPhysicalMetricError('pesoKg') ? <p className="text-xs text-destructive">{physicalMetricErrors.pesoKg}</p> : null}
          </div>
        </section>
      )}

      {currentKey === 'historico' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Você já corre?</h2>
            <p className="mt-2 text-sm text-muted-foreground">Isso define se o plano comeca pela base ou pela performance.</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectionCard label="Não corro" description="Quero comecar com progressão segura" selected={form.jaCorre === false} onClick={() => updateForm('jaCorre', false)} />
            <SelectionCard label="Já corro" description="Tenho alguma experiência com corrida" selected={form.jaCorre === true} onClick={() => updateForm('jaCorre', true)} />
          </div>
        </section>
      )}

      {currentKey === 'base' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Sua base atual</h2>
            <p className="mt-2 text-sm text-muted-foreground">Essas respostas ajudam a evitar uma carga agressiva demais.</p>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Faz outras atividades fisicas?</p>
            <div className="grid grid-cols-2 gap-3">
              <SelectionCard label="Sim" selected={form.fazAtividade === true} onClick={() => updateForm('fazAtividade', true)} />
              <SelectionCard label="Nao" selected={form.fazAtividade === false} onClick={() => updateForm('fazAtividade', false)} />
            </div>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Quao ativa e sua rotina?</p>
            <div className="space-y-2">
              <SelectionCard label="Pouco ativa" description="Passo boa parte do dia sentado" selected={form.atividadeNivel === 'POUCO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'POUCO_ATIVO')} />
              <SelectionCard label="Ativa" description="Caminho, treino ou me movimento com frequencia" selected={form.atividadeNivel === 'ATIVO'} onClick={() => updateForm('atividadeNivel', 'ATIVO')} />
              <SelectionCard label="Muito ativa" description="Treino outras modalidades com boa consistencia" selected={form.atividadeNivel === 'MUITO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'MUITO_ATIVO')} />
            </div>
          </div>
          <Input type="number" min="3" max="12" placeholder="Horas de sono por noite" value={form.horasSonoMedia} onChange={(event) => updateForm('horasSonoMedia', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
        </section>
      )}

      {currentKey === 'objetivo' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Seu primeiro objetivo</h2>
            <p className="mt-2 text-sm text-muted-foreground">Escolha a distancia e o tempo de preparacao.</p>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponiveis por semana</p>
            <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
          </div>
          <div className="space-y-2">
            <Input type="text" inputMode="numeric" min={trainingWeeksGuidance.min} max={trainingWeeksGuidance.max} placeholder={'Semanas de treinamento (' + trainingWeeksGuidance.min + ' a ' + trainingWeeksGuidance.max + ')'} value={form.semanasTreino} onChange={(event) => handleWeeksChange(event.target.value)} onBlur={() => touchTrainingField('semanasTreino')} aria-invalid={shouldShowTrainingFieldError('semanasTreino')} className={getTrainingFieldInputClassName('semanasTreino')} />
            <p className="text-xs text-muted-foreground">Faixa segura para seu perfil: {trainingWeeksGuidance.min} a {trainingWeeksGuidance.max} semanas. Recomendado agora: {trainingWeeksGuidance.recommended} semanas.</p>
            <p className="text-xs text-muted-foreground">{trainingWeeksGuidance.summary}</p>
            {shouldShowTrainingFieldError('semanasTreino') ? <p className="text-xs text-destructive">{trainingErrors.semanasTreino}</p> : null}
          </div>
          <div className="space-y-2">
            {DISTANCE_GOALS.map((item) => {
              const disabled = item.value === '10K' && form.atividadeNivel !== 'MUITO_ATIVO';
              return (
                <SelectionCard
                  key={item.value}
                  label={item.recommended ? `${item.label} - recomendado` : item.label}
                  description={disabled ? '10 km fica liberado para quem marcou rotina muito ativa' : item.description}
                  selected={form.objetivoNaoCorredor === item.value}
                  onClick={() => !disabled && updateForm('objetivoNaoCorredor', item.value)}
                />
              );
            })}
          </div>
        </section>
      )}

      {currentKey === 'nivel' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Seu nivel na corrida</h2>
            <p className="mt-2 text-sm text-muted-foreground">Escolha o nivel que melhor descreve seu momento atual para ajustar o resto do fluxo.</p>
          </div>
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Nivel na corrida</p>
            {LEVELS.map((item) => (
              <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.nivelCorrida === item.value} onClick={() => updateForm('nivelCorrida', item.value)} />
            ))}
          </div>
        </section>
      )}

      {currentKey === 'corrida' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Seu objetivo na corrida</h2>
            <p className="mt-2 text-sm text-muted-foreground">Agora escolha o objetivo que mais combina com a sua fase atual.</p>
          </div>
          <div className="space-y-2">
            {RUNNER_GOALS.map((item) => (
              <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.objetivoCorredor === item.value} onClick={() => updateForm('objetivoCorredor', item.value)} />
            ))}
          </div>
        </section>
      )}

      {currentKey === 'performance' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">{isIntermediate ? 'Seu melhor ritmo' : 'Dados avancados'}</h2>
            <p className="mt-2 text-sm text-muted-foreground">Use uma referencia real para a IA estimar treinos mais precisos.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {RP_DISTANCES.map((item) => (
              <SelectionCard key={item.value} label={item.label} selected={form.distanciaRp === item.value} onClick={() => updateForm('distanciaRp', item.value)} />
            ))}
          </div>
          <div className="space-y-2">
            <Input placeholder="Pace do RP (ex.: 5:30 min/km)" value={form.paceRp} onChange={(event) => handlePaceChange(event.target.value)} onBlur={handlePaceBlur} aria-invalid={shouldShowTrainingFieldError('paceRp')} className={getTrainingFieldInputClassName('paceRp')} />
            <p className="text-xs text-muted-foreground">Use o melhor pace real da distancia escolhida, sempre no formato m:ss.</p>
            {shouldShowTrainingFieldError('paceRp') ? <p className="text-xs text-destructive">{trainingErrors.paceRp}</p> : null}
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponiveis para treinar</p>
            <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
          </div>
          <div className="space-y-2">
            <Input type="text" inputMode="numeric" min={trainingWeeksGuidance.min} max={trainingWeeksGuidance.max} placeholder={'Semanas ate o objetivo (' + trainingWeeksGuidance.min + ' a ' + trainingWeeksGuidance.max + ')'} value={form.semanasTreino} onChange={(event) => handleWeeksChange(event.target.value)} onBlur={() => touchTrainingField('semanasTreino')} aria-invalid={shouldShowTrainingFieldError('semanasTreino')} className={getTrainingFieldInputClassName('semanasTreino')} />
            <p className="text-xs text-muted-foreground">Faixa segura para seu perfil: {trainingWeeksGuidance.min} a {trainingWeeksGuidance.max} semanas. Recomendado agora: {trainingWeeksGuidance.recommended} semanas.</p>
            <p className="text-xs text-muted-foreground">{trainingWeeksGuidance.summary}</p>
            {shouldShowTrainingFieldError('semanasTreino') ? <p className="text-xs text-destructive">{trainingErrors.semanasTreino}</p> : null}
          </div>
          {isAdvanced && (
            <div className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div>
                <p className="text-xs font-semibold uppercase text-muted-foreground">Paces por zona</p>
                <p className="mt-1 text-xs text-muted-foreground">Preencha Z1 ate Z5 no formato min/km.</p>
              </div>
              <div className="grid grid-cols-2 gap-3">
                {['z1', 'z2', 'z3', 'z4', 'z5'].map((zone) => (
                  <Input key={zone} placeholder={zone.toUpperCase()} value={form.zonasPace[zone]} onChange={(event) => updateZone(zone, event.target.value)} className="h-11 rounded-xl border-border bg-background" />
                ))}
              </div>
            </div>
          )}
        </section>
      )}
    </>
  );

  return (
    <div className="min-h-screen bg-background px-5 py-8">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg flex-col">
        <div className="mb-8">
          <Link to="/" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-primary">
            <ChevronLeft className="h-4 w-4" />
            SeuCorre
          </Link>
          <div className="rounded-3xl border border-border bg-card/80 p-5 shadow-2xl shadow-black/20">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/15 text-primary">
              {isLogin ? <LogIn className="h-6 w-6" /> : isProfileOnly ? <ShieldCheck className="h-6 w-6" /> : <Mail className="h-6 w-6" />}
            </div>
            <h1 className="text-3xl font-bold text-foreground">
              {isLogin ? 'Entrar' : isProfileOnly ? 'Perfil do atleta' : 'Criar conta'}
            </h1>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {isLogin
                ? 'Entre com email e senha ou continue pelo Google.'
                : isProfileOnly
                  ? 'Responda o necessario para o SeuCorre montar um plano coerente com seu momento.'
                  : 'Crie sua conta com email e senha ou continue pelo Google.'}
            </p>
          </div>
        </div>

        <form onSubmit={submit} className="flex flex-1 flex-col gap-5 pb-28">
          {!isProfileOnly && (
            <>
              <GoogleButton label={isLogin ? 'Entrar com Google' : 'Criar conta com Google'} onClick={handleGoogle} />
              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                <span className="h-px flex-1 bg-border" />
                ou
                <span className="h-px flex-1 bg-border" />
              </div>
            </>
          )}

          {!isLogin && !isProfileOnly && (
            <div className="space-y-3">
              <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
            </div>
          )}

          {isLogin && (
            <div className="space-y-3">
              <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Link to="/cadastro" className="block text-center text-sm font-medium text-primary">Criar uma conta</Link>
            </div>
          )}

          {isProfileOnly && renderProfileStep()}

          {!isLogin && !isProfileOnly && (
            <Link to="/entrar" className="block text-center text-sm font-medium text-primary">Ja tenho conta</Link>
          )}

          {error && <p className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}

          <div className="fixed bottom-0 left-0 right-0 mx-auto max-w-lg bg-gradient-to-t from-background via-background to-transparent p-5">
            {isProfileOnly ? (
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={loading || profileStep === 0}
                  onClick={() => {
                    setError('');
                    setProfileStep((current) => Math.max(current - 1, 0));
                  }}
                  className="h-14 rounded-2xl px-5"
                >
                  <ChevronLeft className="h-5 w-5" />
                  Voltar
                </Button>
                {isLastProfileStep ? (
                  <Button type="submit" disabled={!valid || loading} className="h-14 flex-1 rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                    {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : <span className="flex items-center gap-2"><Sparkles className="h-5 w-5" />Finalizar<ArrowRight className="h-5 w-5" /></span>}
                  </Button>
                ) : (
                  <Button type="button" onClick={goToNextProfileStep} className="h-14 flex-1 rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                    Continuar
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                )}
              </div>
            ) : (
              <Button type="submit" disabled={!valid || loading} className="h-14 w-full rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                {loading ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : isLogin ? (
                  <span className="flex items-center gap-2"><LogIn className="h-5 w-5" />Entrar</span>
                ) : (
                  <span className="flex items-center gap-2"><Sparkles className="h-5 w-5" />Criar conta<ArrowRight className="h-5 w-5" /></span>
                )}
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
