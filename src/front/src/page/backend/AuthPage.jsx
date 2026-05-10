import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Loader2, LogIn, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import DaySelector from '@/components/onboarding/DaySelector';
import SelectionCard from '@/components/onboarding/SelectionCard';

const LEVELS = [
  { value: 'INICIANTE', label: 'Iniciante', description: 'Começando agora' },
  { value: 'INTERMEDIARIO', label: 'Intermediário', description: 'Corre com alguma rotina' },
  { value: 'AVANCADO', label: 'Avançado', description: 'Treina com consistência' },
];

const GOALS = [
  { value: 'SAUDE_GERAL', label: 'Saúde', description: 'Bem-estar e rotina' },
  { value: 'EMAGRECER', label: 'Emagrecer', description: 'Perda de peso' },
  { value: 'COMPLETAR_5K', label: 'Completar 5 km', description: 'Primeira prova curta' },
  { value: 'MELHORAR_5K', label: 'Melhorar 5 km', description: 'Ganhar velocidade' },
  { value: 'COMPLETAR_MEIA_MARATONA', label: 'Meia maratona', description: 'Preparar 21 km' },
  { value: 'COMPLETAR_MARATONA', label: 'Maratona', description: 'Preparar 42 km' },
];

const birthDateFromAge = (age) => {
  const date = new Date();
  date.setFullYear(date.getFullYear() - Number(age));
  return date.toISOString().slice(0, 10);
};

const buildProfilePayload = (form) => ({
  telefone: form.telefone || null,
  dadosFisicos: {
    pesoKg: Number(form.pesoKg),
    alturaCm: Number(form.alturaCm),
    dataNascimento: birthDateFromAge(form.idade),
    genero: form.genero,
    fcRepouso: form.fcRepouso ? Number(form.fcRepouso) : null,
    fcMaxima: form.fcMaxima ? Number(form.fcMaxima) : null,
    horasSonoMedia: form.horasSonoMedia ? Number(form.horasSonoMedia) : null,
    sedentario: form.nivelCondicionamento === 'INICIANTE',
  },
  perfilAtleta: {
    nivelCondicionamento: form.nivelCondicionamento,
    objetivo: form.objetivo,
    jaCorre: form.jaCorre,
    diasDisponiveisSemana: form.diasSemanaTreino.length,
    diasSemanaTreino: form.diasSemanaTreino.join(','),
  },
  perfilCorrida: null,
  condicoesSaude: [],
  dispositivos: [],
});

export default function AuthPage({ mode = 'register' }) {
  const navigate = useNavigate();
  const { login, reloadUser, isAuthenticated } = useAuth();
  const [authMode, setAuthMode] = useState(mode);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    nome: '',
    email: '',
    senha: '',
    nivelCondicionamento: 'INICIANTE',
    objetivo: 'SAUDE_GERAL',
    jaCorre: false,
    diasSemanaTreino: ['seg', 'qua', 'sex'],
    idade: '',
    pesoKg: '',
    alturaCm: '',
    genero: '',
    fcRepouso: '',
    fcMaxima: '',
    horasSonoMedia: '',
  });

  const isLogin = authMode === 'login';
  const isProfileOnly = authMode === 'profile' || isAuthenticated;

  const valid = useMemo(() => {
    if (isLogin) return form.email && form.senha;
    const accountOk = isProfileOnly || (form.nome && form.email && form.senha.length >= 6);
    return accountOk && form.idade && form.pesoKg && form.alturaCm && form.genero && form.diasSemanaTreino.length > 0;
  }, [form, isLogin, isProfileOnly]);

  const submit = async (event) => {
    event.preventDefault();
    if (!valid || loading) return;
    setLoading(true);
    setError('');
    try {
      if (isLogin) {
        await login({ email: form.email, senha: form.senha });
        navigate('/');
        return;
      }

      const profile = buildProfilePayload(form);
      if (isProfileOnly) {
        await api.usuarios.atualizarOnboarding(profile);
      } else {
        await api.usuarios.registrar({
          nome: form.nome,
          email: form.email,
          senha: form.senha,
          ...profile,
        });
        await login({ email: form.email, senha: form.senha });
      }
      await reloadUser();
      navigate('/gerar-plano');
    } catch (err) {
      setError(err.message || 'Não foi possível concluir a operação.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto px-5 py-10">
      <div className="mb-8">
        <p className="text-xs text-primary font-semibold mb-2">SeuCorre</p>
        <h1 className="text-3xl font-bold text-foreground">{isLogin ? 'Entrar' : isProfileOnly ? 'Atualizar perfil' : 'Criar conta'}</h1>
        <p className="text-sm text-muted-foreground mt-2">
          {isLogin ? 'Acesse com seu JWT do backend.' : 'Preencha os campos usados pelo cadastro e onboarding da API.'}
        </p>
      </div>

      <form onSubmit={submit} className="space-y-5 pb-28">
        {!isLogin && !isProfileOnly && (
          <div className="space-y-3">
            <Input placeholder="Nome" value={form.nome} onChange={(event) => setForm({ ...form, nome: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
            <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
            <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => setForm({ ...form, senha: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
          </div>
        )}

        {isLogin ? (
          <div className="space-y-3">
            <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
            <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => setForm({ ...form, senha: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
            <button type="button" onClick={() => setAuthMode('register')} className="w-full text-sm text-primary">Criar uma conta</button>
          </div>
        ) : (
          <>
            <section>
              <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Nível</h2>
              <div className="space-y-2">
                {LEVELS.map((item) => (
                  <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.nivelCondicionamento === item.value} onClick={() => setForm({ ...form, nivelCondicionamento: item.value })} />
                ))}
              </div>
            </section>

            <section>
              <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Objetivo</h2>
              <div className="grid grid-cols-1 gap-2">
                {GOALS.map((item) => (
                  <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.objetivo === item.value} onClick={() => setForm({ ...form, objetivo: item.value })} />
                ))}
              </div>
            </section>

            <section>
              <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Histórico</h2>
              <div className="grid grid-cols-2 gap-3">
                <SelectionCard label="Estou começando" selected={!form.jaCorre} onClick={() => setForm({ ...form, jaCorre: false })} />
                <SelectionCard label="Já corro" selected={form.jaCorre} onClick={() => setForm({ ...form, jaCorre: true })} />
              </div>
            </section>

            <section>
              <h2 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wider">Dias de treino</h2>
              <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => setForm({ ...form, diasSemanaTreino: dias })} />
            </section>

            <section className="space-y-3">
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Dados físicos</h2>
              <div className="grid grid-cols-2 gap-3">
                <Input type="number" placeholder="Idade" value={form.idade} onChange={(event) => setForm({ ...form, idade: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="Peso kg" value={form.pesoKg} onChange={(event) => setForm({ ...form, pesoKg: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
              </div>
              <Input type="number" placeholder="Altura cm" value={form.alturaCm} onChange={(event) => setForm({ ...form, alturaCm: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
              <div className="grid grid-cols-2 gap-3">
                <SelectionCard label="Masculino" selected={form.genero === 'masculino'} onClick={() => setForm({ ...form, genero: 'masculino' })} />
                <SelectionCard label="Feminino" selected={form.genero === 'feminino'} onClick={() => setForm({ ...form, genero: 'feminino' })} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Input type="number" placeholder="FC repouso" value={form.fcRepouso} onChange={(event) => setForm({ ...form, fcRepouso: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" placeholder="Sono médio" value={form.horasSonoMedia} onChange={(event) => setForm({ ...form, horasSonoMedia: event.target.value })} className="bg-card border-border h-12 rounded-xl" />
              </div>
            </section>
          </>
        )}

        {error && <p className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}

        {!isLogin && !isProfileOnly && (
          <Link to="/entrar" className="block text-center text-sm text-primary">Já tenho conta</Link>
        )}

        <div className="fixed bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-background via-background to-transparent max-w-lg mx-auto">
          <Button type="submit" disabled={!valid || loading} className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-semibold text-base">
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : isLogin ? <span className="flex items-center gap-2"><LogIn className="w-5 h-5" />Entrar</span> : <span className="flex items-center gap-2"><Sparkles className="w-5 h-5" />Salvar e gerar plano<ArrowRight className="w-5 h-5" /></span>}
          </Button>
        </div>
      </form>
    </div>
  );
}
