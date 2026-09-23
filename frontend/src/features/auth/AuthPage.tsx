import { useCallback, useRef, useState, type FormEvent } from 'react'
import { useAuth } from './AuthContext'
import { forgotPassword, resetPassword } from '../../services/authService'
import type { AuthSession } from '../../types/auth'
import { Turnstile, type TurnstileHandle } from './Turnstile'
import type { AuthMode } from './authTypes'
import { canSubmitProtectedAuth, protectedAuthRequiresSiteKey, turnstileActionForMode } from './turnstileAuth'
import { NavIcon } from '../../components/NavIcon/NavIcon'
import styles from './AuthPage.module.css'

const turnstileSiteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY?.trim() ?? ''
const isProduction = import.meta.env.PROD

type AuthPageProps = {
  initialMode: AuthMode
  initialNotice?: string | null
  resetToken?: string | null
  onAuthenticated: (session: AuthSession) => void
  onBack: () => void
  onModeChange: (mode: AuthMode, notice?: string) => void
}

export function AuthPage({ initialMode, initialNotice, resetToken: initialResetToken, onAuthenticated, onBack, onModeChange }: AuthPageProps) {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<AuthMode>(initialResetToken ? 'reset' : initialMode)
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [resetToken, setResetToken] = useState(initialResetToken ?? '')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(initialNotice ?? null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [turnstileToken, setTurnstileToken] = useState<string | null>(null)
  const turnstileRef = useRef<TurnstileHandle | null>(null)

  const isRegistering = mode === 'register'
  const isRecoveringPassword = mode === 'forgot'
  const isResettingPassword = mode === 'reset'
  const turnstileAction = turnstileActionForMode(mode)
  const hasTurnstileSiteKey = turnstileSiteKey.length > 0
  const turnstileConfigError = protectedAuthRequiresSiteKey(mode, isProduction, turnstileSiteKey)
    ? 'A proteção anti-bot não está configurada. Contacta a equipa Saltos nas Palhaçadas.'
    : null
  const isSubmitDisabled = isSubmitting || !canSubmitProtectedAuth(mode, turnstileSiteKey, turnstileToken, isProduction)

  const resetTurnstile = useCallback(() => {
    setTurnstileToken(null)
    turnstileRef.current?.reset()
  }, [])

  const handleTurnstileError = useCallback(() => {
    setTurnstileToken(null)
    setError('Não foi possível validar a proteção anti-bot. Tenta novamente.')
  }, [])

  const handleTurnstileExpire = useCallback(() => {
    setTurnstileToken(null)
    setError('A validação anti-bot expirou. Confirma novamente antes de continuar.')
  }, [])

  function changeMode(nextMode: AuthMode) {
    setMode(nextMode)
    setError(null)
    setNotice(null)
    resetTurnstile()
    setUsername('')
    setFirstName('')
    setLastName('')
    setPhone('')
    setPassword('')
    setPasswordConfirmation('')
    onModeChange(nextMode)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedEmail = email.trim().toLowerCase()

    if (!normalizedEmail && !isResettingPassword) {
      setError('Indica o teu email.')
      return
    }

    if (turnstileConfigError) {
      setError(turnstileConfigError)
      return
    }

    if (turnstileAction && hasTurnstileSiteKey && !turnstileToken) {
      setError('Confirma a validação anti-bot antes de continuar.')
      return
    }

    if (isRecoveringPassword) {
      setIsSubmitting(true)
      setError(null)
      setNotice(null)
      try {
        await forgotPassword(normalizedEmail, turnstileToken ?? undefined)
        setNotice('Se existir uma conta com esse email, vais receber um link para recuperar a palavra-passe.')
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : 'Não foi possível pedir a recuperação.')
      } finally {
        resetTurnstile()
        setIsSubmitting(false)
      }
      return
    }

    if (isResettingPassword && !resetToken.trim()) {
      setError('O link de recuperação está incompleto.')
      return
    }

    if (!password) {
      setError('Indica a tua palavra-passe.')
      return
    }

    if (isResettingPassword && password.length < 8) {
      setError('A nova palavra-passe tem de ter pelo menos 8 caracteres.')
      return
    }

    if (isResettingPassword && password !== passwordConfirmation) {
      setError('As palavras-passe não coincidem.')
      return
    }

    if (isResettingPassword) {
      setIsSubmitting(true)
      setError(null)
      setNotice(null)
      try {
        await resetPassword(resetToken.trim(), password)
        setPassword('')
        setPasswordConfirmation('')
        setResetToken('')
        setMode('login')
        onModeChange('login', 'Palavra-passe atualizada. Já podes entrar.')
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar a palavra-passe.')
      } finally {
        setIsSubmitting(false)
      }
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
        }, turnstileToken ?? undefined)
        : await login({ email: normalizedEmail, password }, turnstileToken ?? undefined)
      resetTurnstile()
      setIsSubmitting(false)
      onAuthenticated(session)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível concluir a autenticação.')
      resetTurnstile()
      setIsSubmitting(false)
    }
  }

  return (
    <section className={styles.page}>
      <button className={styles.back} type="button" onClick={onBack}>
        <NavIcon name="arrow-left" />
        Voltar ao site
      </button>
      <div className={styles.panel}>
        <p className="eyebrow">Conta Saltos nas Palhaçadas</p>
        <h1>{pageTitle(mode)}</h1>
        <p className={styles.intro}>
          {pageIntro(mode)}
        </p>

        {!isRecoveringPassword && !isResettingPassword && (
          <div className={styles.modeSwitch} aria-label="Tipo de autenticação">
            <button aria-pressed={!isRegistering} className={!isRegistering ? styles.active : ''} type="button" onClick={() => changeMode('login')}>Entrar</button>
            <button aria-pressed={isRegistering} className={isRegistering ? styles.active : ''} type="button" onClick={() => changeMode('register')}>Criar conta</button>
          </div>
        )}

        <form onSubmit={(event) => { void submit(event) }} noValidate>
          {!isResettingPassword && (
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
          )}
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
          {isResettingPassword && !initialResetToken && (
            <label>
              Código de recuperação
              <input
                autoComplete="off"
                onChange={(event) => setResetToken(event.target.value)}
                required
                value={resetToken}
              />
            </label>
          )}
          {!isRecoveringPassword && (
            <label>
              {isResettingPassword ? 'Nova palavra-passe' : 'Palavra-passe'}
              <input
                autoComplete={isRegistering || isResettingPassword ? 'new-password' : 'current-password'}
                onChange={(event) => setPassword(event.target.value)}
                required
                type="password"
                value={password}
              />
              {(isRegistering || isResettingPassword) && <small>Usa pelo menos 8 caracteres.</small>}
            </label>
          )}
          {(isRegistering || isResettingPassword) && (
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
          {turnstileAction && hasTurnstileSiteKey && (
            <Turnstile
              action={turnstileAction}
              className={styles.turnstile}
              onError={handleTurnstileError}
              onExpire={handleTurnstileExpire}
              onToken={setTurnstileToken}
              ref={turnstileRef}
              siteKey={turnstileSiteKey}
            />
          )}
          {notice && <p className={styles.success} role="status">{notice}</p>}
          {turnstileConfigError && <p className={styles.error} role="alert">{turnstileConfigError}</p>}
          {error && <p className={styles.error} role="alert">{error}</p>}
          <button disabled={isSubmitDisabled} type="submit">
            {isSubmitting ? 'A processar...' : submitLabel(mode)}
          </button>
          {!isRegistering && !isRecoveringPassword && !isResettingPassword && (
            <button className={styles.textButton} type="button" onClick={() => changeMode('forgot')}>Esqueci-me da palavra-passe</button>
          )}
          {(isRecoveringPassword || isResettingPassword) && (
            <button className={styles.textButton} type="button" onClick={() => changeMode('login')}>Voltar ao login</button>
          )}
        </form>
      </div>
    </section>
  )
}

function pageTitle(mode: AuthMode) {
  if (mode === 'register') return 'Criar conta'
  if (mode === 'forgot') return 'Recuperar palavra-passe'
  if (mode === 'reset') return 'Nova palavra-passe'
  return 'Entrar'
}

function pageIntro(mode: AuthMode) {
  if (mode === 'register') return 'Guarda as publicações de que mais gostas e consulta-as mais tarde.'
  if (mode === 'forgot') return 'Indica o email da conta para receberes um link temporário de recuperação.'
  if (mode === 'reset') return 'Define uma nova palavra-passe para voltares a entrar na tua conta.'
  return 'Entra para veres os teus favoritos e pedidos.'
}

function submitLabel(mode: AuthMode) {
  if (mode === 'register') return 'Criar conta'
  if (mode === 'forgot') return 'Enviar link'
  if (mode === 'reset') return 'Atualizar palavra-passe'
  return 'Entrar'
}

function isValidUsername(value: string) {
  return /^(?!.*\.\.)(?!\.)(?!.*\.$)[a-z0-9._]{3,30}$/.test(value.trim())
}

function isValidPhone(value: string) {
  const trimmed = value.trim()
  const digitCount = trimmed.replace(/\D/g, '').length
  return /^\+?[0-9][0-9().\s-]{7,24}$/.test(trimmed) && digitCount >= 9 && digitCount <= 15
}
