// React error boundary that catches unhandled render errors and shows a fallback recovery screen.
import { Component } from 'react'
import { AlertTriangle } from 'lucide-react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  // Called during rendering when a descendant throws; switches the boundary into error mode.
  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  // Called after React flushes the tree; used for side-effects such as logging.
  componentDidCatch(error, info) {
    console.error('[ErrorBoundary]', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-red-500">
            <AlertTriangle className="h-7 w-7" strokeWidth={1.5} />
          </div>
          <h1 className="text-xl font-semibold text-slate-900">Qualcosa è andato storto</h1>
          <p className="max-w-sm text-sm text-slate-500">
            Si è verificato un errore inatteso. Ricarica la pagina per riprovare.
          </p>
          <button
            type="button"
            className="rounded-full bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700"
            onClick={() => window.location.reload()}
          >
            Ricarica pagina
          </button>
        </div>
      )
    }

    return this.props.children
  }
}
