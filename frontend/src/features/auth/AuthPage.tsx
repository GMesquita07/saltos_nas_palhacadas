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
  const [username, setUsername] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const isRegistering = mode === 'register'

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode)
    setError(null)
    setUsername('')
    setFirstName('')
    setLastName('')
    setPhone('')
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

    if (isRegistering && !isValidUsername(username)) {
      setError('Escolhe um nome de utilizador com 3 a 30 caracteres, em minúsculas, usando letras, números, ponto ou underscore.')
      return
    }

    if (isRegistering && firstName.trim().length < 2) {
      setError('Indica o teu primeiro nome.')
      return
    }

    if (isRegistering && lastName.trim().length < 2) {
      setError('Indica o teu último nome.')
      return
    }

    if (isRegistering && !isValidPhone(phone)) {
      setError('Indica um contacto telefónico válido.')
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
        ? await register({
          email: normalizedEmail,
          username: username.trim().toLowerCase(),
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          phone: phone.trim(),
          password,
        })
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
          {isRegistering && (
            <>
              <label>
                Nome de utilizador
                <input
                  autoComplete="username"
                  maxLength={30}
                  minLength={3}
                  onChange={(event) => setUsername(event.target.value.toLowerCase().replace(/\s+/g, ''))}
                  pattern="(?!.*\.\.)(?!\.)(?!.*\.$)[a-z0-9._]{3,30}"
                  placeholder="nome_utilizador"
                  required
                  value={username}
                />
                <small>Minúsculas, números, ponto ou underscore.</small>
              </label>
              <div className={styles.nameFields}>
                <label>
                  Primeiro nome
                  <input
                    autoComplete="given-name"
                    maxLength={80}
                    minLength={2}
                    onChange={(event) => setFirstName(event.target.value)}
                    required
                    value={firstName}
                  />
                </label>
                <label>
                  Último nome
                  <input
                    autoComplete="family-name"
                    maxLength={80}
                    minLength={2}
                    onChange={(event) => setLastName(event.target.value)}
                    required
                    value={lastName}
                  />
                </label>
              </div>
              <label>
                Contacto telefónico
                <input
                  autoComplete="tel"
                  inputMode="tel"
                  onChange={(event) => setPhone(event.target.value)}
                  placeholder="+351 912 345 678"
                  required
                  type="tel"
                  value={phone}
                />
              </label>
            </>
          )}
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

function isValidUsername(value: string) {
  return /^(?!.*\.\.)(?!\.)(?!.*\.$)[a-z0-9._]{3,30}$/.test(value.trim())
}

function isValidPhone(value: string) {
  const trimmed = value.trim()
  const digitCount = trimmed.replace(/\D/g, '').length
  return /^\+?[0-9][0-9().\s-]{7,24}$/.test(trimmed) && digitCount >= 9 && digitCount <= 15
}
