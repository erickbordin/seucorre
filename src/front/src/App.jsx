import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { AuthProvider, useAuth } from '@/lib/AuthContext';
import AppLayout from '@/components/layout/AppLayout';
import PageNotFound from '@/lib/PageNotFound';
import Home from '@/page/backend/Home';
import AuthPage from '@/page/backend/AuthPage';
import GeneratePlan from '@/page/backend/GeneratePlan';
import WorkoutDetail from '@/page/backend/WorkoutDetail';
import Plan from '@/page/backend/Plan';
import Progress from '@/page/backend/Progress';
import AICoach from '@/page/backend/AiCoach';
import Profile from '@/page/backend/Profile';
import Paywall from '@/page/Paywall';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

const Loading = () => (
  <div className="fixed inset-0 flex items-center justify-center bg-background">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
      <p className="text-xs text-muted-foreground">Carregando...</p>
    </div>
  </div>
);

const AuthenticatedRoutes = () => {
  const { isLoadingAuth, isAuthenticated } = useAuth();

  if (isLoadingAuth) return <Loading />;

  return (
    <Routes>
      <Route path="/entrar" element={isAuthenticated ? <Navigate to="/" replace /> : <AuthPage mode="login" />} />
      <Route path="/onboarding" element={<AuthPage mode={isAuthenticated ? 'profile' : 'register'} />} />
      <Route path="/gerar-plano" element={isAuthenticated ? <GeneratePlan /> : <Navigate to="/entrar" replace />} />
      <Route path="/treino/:planoId/:sessionIdx" element={isAuthenticated ? <WorkoutDetail /> : <Navigate to="/entrar" replace />} />
      <Route path="/paywall" element={<Paywall />} />
      <Route element={isAuthenticated ? <AppLayout /> : <Navigate to="/entrar" replace />}>
        <Route path="/" element={<Home />} />
        <Route path="/plano" element={<Plan />} />
        <Route path="/ia" element={<AICoach />} />
        <Route path="/progresso" element={<Progress />} />
        <Route path="/perfil" element={<Profile />} />
      </Route>
      <Route path="*" element={<PageNotFound />} />
    </Routes>
  );
};

function App() {
  return (
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <Router>
          <AuthenticatedRoutes />
        </Router>
        <Toaster />
      </QueryClientProvider>
    </AuthProvider>
  );
}

export default App;
