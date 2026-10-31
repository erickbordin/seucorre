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
  GENDER_OPTIONS,
  ageFromBirthDate,
  birthDateFromAge,
  clearOnboardingDraft,
  parseTrainingDays,
  readOnboardingDraft,
  saveOnboardingDraft,
  serializeTrainingDays,
} from '@/lib/user';

const DISTANCE_GOALS = [
  { value: '5K', label: '5 km', description: 'Recomendado para construir base com segurança', recommended: true },
  { value: '10K', label: '10 km', description: 'Disponível para quem está muito ativo' },
];

const RUNNER_GOALS = [
  { value: 'COMPLETAR_5K', label: '5 km', description: 'Completar a distância com boa base' },
  { value: 'COMPLETAR_10K', label: '10 km', description: 'Evoluir resistencia e ritmo' },
  { value: 'COMPLETAR_MEIA_MARATONA', label: '21 km', description: 'Preparar meia maratona' },
  { value: 'COMPLETAR_MARATONA', label: '42 km', description: 'Preparar maratona' },
  { value: 'MELHORAR_DISTANCIA', label: 'Melhorar marca', description: 'Baixar tempo em uma distância' },
  { value: 'VOLTAR_A_CORRER', label: 'Voltar a correr', description: 'Retomar rotina com progressão' },
];

const LEVELS = [
  { value: 'INICIANTE', label: 'Iniciante', description: 'Corre pouco ou está retomando do zero' },
  { value: 'INTERMEDIARIO', label: 'Intermediário', description: 'Já treina com alguma frequência' },
  { value: 'AVANCADO', label: 'Avançado', description: 'Treina com consistência e usa zonas/ritmos' },
];

const SLEEP_QUALITY = [
  { value: 'RUIM', label: 'Ruim', description: 'Acordo cansado quase sempre' },
  { value: 'REGULAR', label: 'Regular', description: 'Durmo bem só em alguns dias' },
  { value: 'BOA', label: 'Boa', description: 'Acordo descansado na maioria dos dias' },
  { value: 'OTIMA', label: 'Ótima', description: 'Durmo bem e acordo disposto' },
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
  genero: '',
  jaCorre: null,
  fazAtividade: null,
  atividadeNivel: '',
  qualidadeSono: '',
  diasSemanaTreino: ['seg', 'qua', 'sex'],
  objetivoNaoCorredor: '5K',
  objetivoCorredor: 'COMPLETAR_10K',
  nivelCorrida: '',
  distanciaRp: '5',
  paceRp: '',
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

const TRAINING_DAYS = { min: 3, max: 4 };

const hasValidTrainingDays = (days) => days.length >= TRAINING_DAYS.min && days.length <= TRAINING_DAYS.max;

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

const buildTrainingErrors = (form) => {
  const errors = {};
  const paceSeconds = parsePaceToSeconds(form.paceRp);

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
    genero: user?.genero || '',
    jaCorre: user?.jaCorre ?? null,
    diasSemanaTreino: days.length > 0 ? days : ['seg', 'qua', 'sex'],
    qualidadeSono: user?.qualidadeSono || '',
    nivelCorrida: user?.nivelCondicionamento || '',
    objetivoCorredor: user?.objetivo || 'COMPLETAR_10K',
    objetivoNaoCorredor: user?.objetivo === 'COMPLETAR_10K' ? '10K' : user?.objetivo === 'COMPLETAR_5K' ? '5K' : '5K',
    distanciaRp: storedRacePace.distanciaRp,
    paceRp: storedRacePace.paceRp,
  };
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
      genero: form.genero,
      fcRepouso: null,
      fcMaxima: null,
      qualidadeSono: form.qualidadeSono || null,
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
  };
};

const Field = ({ label, error, children }) => (
  <label className="block space-y-2">
    <span className="text-xs font-semibold text-muted-foreground">{label}</span>
    {children}
    {error ? <span className="block text-xs text-destructive">{error}</span> : null}
  </label>
);

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

  const profileSteps = useMemo(() => {
    if (form.jaCorre === null) return ['dados', 'historico', 'nivel'];
    if (isNoRunnerFlow) return ['dados', 'historico', 'base', 'objetivo'];
    if (isBeginnerRunnerFlow) return ['dados', 'historico', 'nivel', 'base', 'objetivo'];
    return ['dados', 'historico', 'nivel', 'corrida', 'performance'];
  }, [form.jaCorre, isBeginnerRunnerFlow, isNoRunnerFlow]);

  const keepsDraft = isProfileOnly && !hasCompletedOnboarding;

  useEffect(() => {
    if (isProfileOnly) {
      const draft = keepsDraft ? readOnboardingDraft(user?.email) : null;
      setForm(draft ? { ...buildFormFromUser(user), ...draft.form } : buildFormFromUser(user));
      setProfileStep(draft ? draft.step : 0);
      setTouchedPhysicalFields({});
      setTouchedTrainingFields({});
      return;
    }
    setForm(createEmptyForm());
    setTouchedPhysicalFields({});
    setTouchedTrainingFields({});
  }, [isProfileOnly, keepsDraft, user]);

  useEffect(() => {
    if (keepsDraft && user?.email) saveOnboardingDraft({ email: user.email, step: profileStep, form: { ...form, senha: '' } });
  }, [form, keepsDraft, profileStep, user?.email]);

  const totalSteps = isProfileOnly ? profileSteps.length : 1;
  const currentKey = profileSteps[Math.min(profileStep, profileSteps.length - 1)];
  const isLastProfileStep = profileStep === profileSteps.length - 1;

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setError('');
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

  const physicalMetricErrors = useMemo(() => buildPhysicalMetricErrors(form), [form.alturaCm, form.idade, form.pesoKg]);
  const hasPhysicalMetricErrors = Object.keys(physicalMetricErrors).length > 0;
  const trainingErrors = useMemo(() => buildTrainingErrors(form), [form.paceRp]);

  const isCurrentProfileStepValid = useMemo(() => {
    if (!isProfileOnly) return true;

    if (currentKey === 'dados') {
      return !hasPhysicalMetricErrors && Boolean(form.genero);
    }

    if (currentKey === 'historico') {
      return form.jaCorre !== null;
    }

    if (currentKey === 'nivel') {
      return Boolean(form.nivelCorrida);
    }

    if (currentKey === 'base') {
      return form.fazAtividade !== null && Boolean(form.atividadeNivel && form.qualidadeSono);
    }

    if (currentKey === 'objetivo') {
      const wants10k = form.objetivoNaoCorredor === '10K';
      return Boolean(
        hasValidTrainingDays(form.diasSemanaTreino) &&
        form.objetivoNaoCorredor &&
        (!wants10k || form.atividadeNivel === 'MUITO_ATIVO')
      );
    }

    if (currentKey === 'corrida') {
      return Boolean(form.objetivoCorredor);
    }

    if (currentKey === 'performance') {
      return Boolean(form.distanciaRp && hasValidTrainingDays(form.diasSemanaTreino) && !trainingErrors.paceRp);
    }

    return true;
  }, [currentKey, form, hasPhysicalMetricErrors, isProfileOnly, trainingErrors]);

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
    if (loading) return;
    if (!valid) {
      if (isProfileOnly) showMissing();
      return;
    }

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
        clearOnboardingDraft();
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
      setError(err.message || 'Não foi possível concluir a operação.');
    } finally {
      setLoading(false);
    }
  };

  const describeMissing = () => {
    if (currentKey === 'dados') return form.genero ? 'Revise idade, altura e peso para continuar.' : 'Escolha seu gênero para continuar.';
    if (currentKey === 'historico') return 'Escolha se você já corre para continuar.';
    if (currentKey === 'nivel') return 'Escolha seu nível na corrida para continuar.';
    if (currentKey === 'corrida') return 'Escolha um objetivo para continuar.';

    const missing = [];
    if (currentKey === 'base') {
      if (form.fazAtividade === null) missing.push('se faz outras atividades físicas');
      if (!form.atividadeNivel) missing.push('quão ativa é sua rotina');
      if (!form.qualidadeSono) missing.push('a qualidade do seu sono');
    }
    if (currentKey === 'objetivo' || currentKey === 'performance') {
      if (!hasValidTrainingDays(form.diasSemanaTreino)) missing.push('3 ou 4 dias de treino');
    }
    if (currentKey === 'performance') {
      if (trainingErrors.paceRp) return trainingErrors.paceRp;
    }
    if (currentKey === 'objetivo' && form.objetivoNaoCorredor === '10K' && form.atividadeNivel !== 'MUITO_ATIVO') {
      return '10 km só fica liberado para quem marcou rotina muito ativa. Escolha 5 km.';
    }
    return missing.length > 0 ? 'Falta informar ' + missing.join(', ') + '.' : 'Revise as respostas desta etapa para continuar.';
  };

  const showMissing = () => {
    if (currentKey === 'dados') setTouchedPhysicalFields({ idade: true, alturaCm: true, pesoKg: true });
    if (currentKey === 'performance') {
      setTouchedTrainingFields((current) => ({ ...current, paceRp: true }));
    }
    setError(describeMissing());
  };

  const goToNextProfileStep = () => {
    if (!isCurrentProfileStepValid) {
      showMissing();
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
      <p className="mb-2 text-xs font-semibold text-primary">
        {form.jaCorre === null ? `Etapa ${profileStep + 1}` : `Etapa ${profileStep + 1} de ${totalSteps}`}
      </p>
    </div>
  );

  const renderProfileStep = () => (
    <>
      {renderProgress()}

      {currentKey === 'dados' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Dados do atleta</h2>
            <p className="mt-2 text-sm text-muted-foreground">Comece com o básico para calibrar volume e segurança.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Idade (anos)" error={shouldShowPhysicalMetricError('idade') ? physicalMetricErrors.idade : null}>
              <Input type="text" inputMode="numeric" placeholder="Ex.: 28" value={form.idade} onChange={(event) => handlePhysicalMetricChange('idade', event.target.value)} onBlur={() => touchPhysicalField('idade')} aria-invalid={shouldShowPhysicalMetricError('idade')} className={getPhysicalMetricInputClassName('idade')} />
            </Field>
            <Field label="Altura (cm)" error={shouldShowPhysicalMetricError('alturaCm') ? physicalMetricErrors.alturaCm : null}>
              <Input type="text" inputMode="numeric" placeholder="Ex.: 175" value={form.alturaCm} onChange={(event) => handlePhysicalMetricChange('alturaCm', event.target.value)} onBlur={() => touchPhysicalField('alturaCm')} aria-invalid={shouldShowPhysicalMetricError('alturaCm')} className={getPhysicalMetricInputClassName('alturaCm')} />
            </Field>
          </div>
          <Field label="Peso (kg)" error={shouldShowPhysicalMetricError('pesoKg') ? physicalMetricErrors.pesoKg : null}>
            <Input type="text" inputMode="decimal" placeholder="Ex.: 72,5" value={form.pesoKg} onChange={(event) => handlePhysicalMetricChange('pesoKg', event.target.value)} onBlur={() => touchPhysicalField('pesoKg')} aria-invalid={shouldShowPhysicalMetricError('pesoKg')} className={getPhysicalMetricInputClassName('pesoKg')} />
          </Field>
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Gênero</p>
            <div className="grid grid-cols-2 gap-3">
              {GENDER_OPTIONS.map((item) => (
                <SelectionCard key={item.value} label={item.label} selected={form.genero === item.value} onClick={() => updateForm('genero', item.value)} />
              ))}
            </div>
          </div>
        </section>
      )}

      {currentKey === 'historico' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Você já corre?</h2>
            <p className="mt-2 text-sm text-muted-foreground">Isso define se o plano começa pela base ou pela performance.</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectionCard label="Não corro" description="Quero começar com progressão segura" selected={form.jaCorre === false} onClick={() => updateForm('jaCorre', false)} />
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
            <p className="text-xs font-semibold uppercase text-muted-foreground">Faz outras atividades físicas?</p>
            <div className="grid grid-cols-2 gap-3">
              <SelectionCard label="Sim" selected={form.fazAtividade === true} onClick={() => updateForm('fazAtividade', true)} />
              <SelectionCard label="Não" selected={form.fazAtividade === false} onClick={() => updateForm('fazAtividade', false)} />
            </div>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Quão ativa é sua rotina?</p>
            <div className="space-y-2">
              <SelectionCard label="Pouco ativa" description="Passo boa parte do dia sentado" selected={form.atividadeNivel === 'POUCO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'POUCO_ATIVO')} />
              <SelectionCard label="Ativa" description="Caminho, treino ou me movimento com frequência" selected={form.atividadeNivel === 'ATIVO'} onClick={() => updateForm('atividadeNivel', 'ATIVO')} />
              <SelectionCard label="Muito ativa" description="Treino outras modalidades com boa consistência" selected={form.atividadeNivel === 'MUITO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'MUITO_ATIVO')} />
            </div>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Como é a qualidade do seu sono?</p>
            <div className="grid grid-cols-2 gap-3">
              {SLEEP_QUALITY.map((item) => (
                <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.qualidadeSono === item.value} onClick={() => updateForm('qualidadeSono', item.value)} />
              ))}
            </div>
          </div>
        </section>
      )}

      {currentKey === 'objetivo' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Seu primeiro objetivo</h2>
            <p className="mt-2 text-sm text-muted-foreground">Escolha a distância e o tempo de preparação.</p>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponíveis por semana (3 ou 4)</p>
            <DaySelector selected={form.diasSemanaTreino} max={TRAINING_DAYS.max} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
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
            <h2 className="text-2xl font-bold text-foreground">Seu nível na corrida</h2>
            <p className="mt-2 text-sm text-muted-foreground">Escolha o nível que melhor descreve seu momento atual para ajustar o resto do fluxo.</p>
          </div>
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Nível na corrida</p>
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
            <h2 className="text-2xl font-bold text-foreground">{isIntermediate ? 'Seu melhor ritmo' : 'Dados avançados'}</h2>
            <p className="mt-2 text-sm text-muted-foreground">Use uma referência real para a IA estimar treinos mais precisos.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {RP_DISTANCES.map((item) => (
              <SelectionCard key={item.value} label={item.label} selected={form.distanciaRp === item.value} onClick={() => updateForm('distanciaRp', item.value)} />
            ))}
          </div>
          <Field label="Pace do seu melhor tempo (min/km)" error={shouldShowTrainingFieldError('paceRp') ? trainingErrors.paceRp : null}>
            <Input placeholder="Ex.: 5:30" value={form.paceRp} onChange={(event) => handlePaceChange(event.target.value)} onBlur={handlePaceBlur} aria-invalid={shouldShowTrainingFieldError('paceRp')} className={getTrainingFieldInputClassName('paceRp')} />
            <span className="block text-xs text-muted-foreground">Use o melhor pace real da distância escolhida, sempre no formato m:ss.</span>
          </Field>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponíveis para treinar (3 ou 4)</p>
            <DaySelector selected={form.diasSemanaTreino} max={TRAINING_DAYS.max} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
          </div>
        </section>
      )}
    </>
  );

  return (
    <div className="min-h-screen bg-background px-5 py-8">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg flex-col">
        <div className={isProfileOnly ? 'mb-5' : 'mb-8'}>
          <Link to="/" className={(isProfileOnly ? 'mb-4' : 'mb-6') + ' inline-flex items-center gap-2 text-sm font-semibold text-primary'}>
            <ChevronLeft className="h-4 w-4" />
            SeuCorre
          </Link>
          {isProfileOnly ? (
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15 text-primary">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <h1 className="text-xl font-bold text-foreground">Perfil do atleta</h1>
            </div>
          ) : (
            <div className="rounded-3xl border border-border bg-card/80 p-5 shadow-2xl shadow-black/20">
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/15 text-primary">
                {isLogin ? <LogIn className="h-6 w-6" /> : <Mail className="h-6 w-6" />}
              </div>
              <h1 className="text-3xl font-bold text-foreground">{isLogin ? 'Entrar' : 'Criar conta'}</h1>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                {isLogin ? 'Entre com email e senha ou continue pelo Google.' : 'Crie sua conta com email e senha ou continue pelo Google.'}
              </p>
            </div>
          )}
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
            <Link to="/entrar" className="block text-center text-sm font-medium text-primary">Já tenho conta</Link>
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
                  <Button type="submit" disabled={loading} className="h-14 flex-1 rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
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
