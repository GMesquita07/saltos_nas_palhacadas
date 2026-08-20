import { useState, type FormEvent } from 'react'
import { useAuth } from './AuthContext'
import type { AuthSession } from '../../types/auth'
import styles from './AuthPage.module.css'

type AuthMode = 'login' | 'register'

type AuthPageProps = {
  initialMode: AuthMode
  onAuthenticated: (session: AuthSession) => void
  onBack: () => void
}

export function AuthPage({ initialMode, onAuthenticated, onBack }: AuthPageProps) {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<AuthMode>(initialMode)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isRegistering = mode === 'register'

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode)
    setError(null)
    setPassword('')
    setPasswordConfirmation('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedEmail = email.trim().toLowerCase()

    if (!normalizedEmail) {
      setError('Indica o teu email.')
      return
    }

    if (!password) {
      setError('Indica a tua palavra-passe.')
      return
    }

    if (isRegistering && password.length < 8) {
      setError('A palavra-passe tem de ter pelo menos 8 caracteres.')
      return
    }

    if (isRegistering && password !== passwordConfirmation) {
      setError('As palavras-passe não coincidem.')
      return
    }

    setIsSubmitting(true)
    setError(null)
    try {
      const session = isRegistering
        ? await register({ email: normalizedEmail, password })
        : await login({ email: normalizedEmail, password })
      onAuthenticated(session)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível concluir a autenticação.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onBack}>← Voltar ao site</button>
      <div className={styles.panel}>
        <p className="eyebrow">Conta Saltos nas Palhaçadas</p>
        <h1>{isRegistering ? 'Criar conta' : 'Entrar'}</h1>
        <p className={styles.intro}>
          {isRegistering
            ? 'Guarda as publicações de que mais gostas e consulta-as mais tarde.'
            : 'Entra para veres os teus favoritos e, se tiveres permissão, gerir o portfólio.'}
        </p>

        <div className={styles.modeSwitch} aria-label="Tipo de autenticação">
          <button aria-pressed={!isRegistering} className={!isRegistering ? styles.active : ''} type="button" onClick={() => changeMode('login')}>Entrar</button>
          <button aria-pressed={isRegistering} className={isRegistering ? styles.active : ''} type="button" onClick={() => changeMode('register')}>Criar conta</button>
        </div>

        <form onSubmit={(event) => { void submit(event) }} noValidate>
          <label>
            Email
            <input
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>
          <label>
            Palavra-passe
            <input
              autoComplete={isRegistering ? 'new-password' : 'current-password'}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
            {isRegistering && <small>Usa pelo menos 8 caracteres.</small>}
          </label>
          {isRegistering && (
            <label>
              Confirmar palavra-passe
              <input
                autoComplete="new-password"
                onChange={(event) => setPasswordConfirmation(event.target.value)}
                required
                type="password"
                value={passwordConfirmation}
              />
            </label>
          )}
          {error && <p className={styles.error} role="alert">{error}</p>}
          <button disabled={isSubmitting} type="submit">
            {isSubmitting ? 'A processar...' : isRegistering ? 'Criar conta' : 'Entrar'}
          </button>
        </form>
      </div>
    </section>
  )
}
