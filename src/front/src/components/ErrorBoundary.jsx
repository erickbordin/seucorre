import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div className="min-h-screen bg-background text-foreground flex items-center justify-center px-6">
          <div className="max-w-md rounded-2xl border border-destructive/30 bg-card p-5">
            <p className="text-sm font-semibold text-destructive mb-2">Erro ao abrir o frontend</p>
            <p className="text-xs text-muted-foreground break-words">{this.state.error.message}</p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
