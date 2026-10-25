import React from 'react'

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="min-h-screen flex items-center justify-center p-6 bg-background text-on-background">
          <div className="max-w-md w-full p-8 bg-surface-card border-2 border-hairline rounded-2xl shadow-xl text-center space-y-4">
            <div className="w-12 h-12 mx-auto rounded-full bg-error/10 text-error flex items-center justify-center text-2xl font-bold">
              !
            </div>
            <h1 className="font-display-md text-xl font-bold text-primary">Something went wrong</h1>
            <p className="text-sm text-body-muted">
              An unexpected error occurred in the application interface.
            </p>
            {this.state.error?.message && (
              <pre className="p-3 bg-surface text-xs rounded border border-hairline text-left overflow-x-auto text-error font-mono">
                {this.state.error.message}
              </pre>
            )}
            <button
              onClick={this.handleReset}
              className="font-button text-sm bg-primary text-on-primary px-5 py-2.5 rounded-xl font-bold shadow hover:opacity-90 transition-opacity"
            >
              Return to Home
            </button>
          </div>
        </main>
      )
    }

    return this.props.children
  }
}
