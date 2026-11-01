import { Outlet } from 'react-router-dom';
import TabBar from './TabBar';

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto relative">
      <main className="pb-24">
        <Outlet />
      </main>
      <div className="scroll-edge-bottom pointer-events-none fixed inset-x-0 bottom-0 z-40 h-28" />
      <TabBar />
    </div>
  );
}