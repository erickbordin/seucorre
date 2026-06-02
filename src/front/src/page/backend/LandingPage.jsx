import { Link } from 'react-router-dom';
import { ArrowRight, LogIn, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';

const RUNNER_IMAGE = 'https://images.unsplash.com/photo-1502904550040-7534597429ae?auto=format&fit=crop&w=1800&q=85';

export default function LandingPage() {
  return (
    <main className="relative min-h-screen overflow-hidden bg-background text-foreground">
      <div
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: `url(${RUNNER_IMAGE})` }}
        aria-hidden="true"
      />
      <div className="absolute inset-0 bg-[linear-gradient(180deg,rgba(10,10,16,0.30)_0%,rgba(10,10,16,0.72)_54%,rgba(10,10,16,0.98)_100%)]" />
      <div className="relative mx-auto flex min-h-screen max-w-lg flex-col px-6 pb-8 pt-10">
        <header className="flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.28em] text-primary">SeuCorre</p>
            <h1 className="mt-3 text-5xl font-black leading-none text-white">SeuCorre</h1>
          </div>
          <div className="rounded-full border border-white/20 bg-black/20 px-3 py-1 text-xs font-semibold text-white/85 backdrop-blur">
            IA para corrida
          </div>
        </header>

        <section className="mt-auto pb-5">
          <p className="max-w-sm text-lg font-semibold leading-tight text-white">
            Planos de corrida adaptados ao seu momento, da primeira semana ao proximo recorde.
          </p>
          <div className="mt-8 space-y-3">
            <Button asChild className="h-14 w-full rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
              <Link to="/entrar">
                <LogIn className="h-5 w-5" />
                Entrar
                <ArrowRight className="h-5 w-5" />
              </Link>
            </Button>
            <Button asChild variant="outline" className="h-14 w-full rounded-2xl border-white/20 bg-white/10 text-base font-semibold text-white backdrop-blur hover:bg-white/15 hover:text-white">
              <Link to="/cadastro">
                <Sparkles className="h-5 w-5" />
                Criar conta
              </Link>
            </Button>
          </div>
        </section>
      </div>
    </main>
  );
}
